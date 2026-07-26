package com.built4u.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit log against BUILT4U: business writes are captured (with the JWT user
 * and a readable entity id), high-volume internal logs are skipped, and the
 * trail exports to a binary. @Transactional rolls back — including the audit
 * rows the listener writes via JDBC into the same transaction, which is exactly
 * why they are visible to the read query here. MOCK web env avoids loopback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuditLogFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String auth;

    private JsonNode postJson(String url, String body) throws Exception {
        String res = mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }
    private JsonNode getJson(String url) throws Exception {
        String res = mvc.perform(get(url).header("Authorization", auth))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private void loginAsAdmin() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(login).get("accessToken").asText();
    }

    @Test
    void businessWrite_isCaptured_and_exports() throws Exception {
        loginAsAdmin();
        // Item create flushes within the request (ItemService re-reads after save),
        // so the listener's audit INSERT lands with the request's JWT user set —
        // exactly as it would on commit in production.
        long catId = postJson("/api/categories", "{\"name\":\"AuditCat\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"AuditLoc\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"AUD\"}");
        postJson("/api/items", """
            {"code":"AUDIT1","name":"Audited Item","catId":%d,"locId":%d,"uom":"AUD","quantity":5,"sellingPrice":10.00}"""
            .formatted(catId, locId));

        JsonNode page = getJson("/api/admin/audit-log?entity=item&action=CREATE");
        assertThat(page.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode top = page.get("content").get(0);
        assertThat(top.get("entityName").asText()).isEqualTo("Item");
        assertThat(top.get("action").asText()).isEqualTo("CREATE");
        assertThat(top.get("username").asText()).isEqualTo("admin");
        assertThat(top.get("entityId").asText()).contains("itemId=");

        // Export to xlsx (a zip → starts with "PK").
        byte[] xlsx = mvc.perform(get("/api/admin/audit-log?format=xlsx").header("Authorization", auth))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");
    }

    @Test
    void highVolumeInternalLogs_areSkipped() throws Exception {
        loginAsAdmin();
        long catId = postJson("/api/categories", "{\"name\":\"AudCat2\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"AudLoc2\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"AUU\"}");
        long itemId = postJson("/api/items", """
            {"code":"AUD1","name":"Aud Item","catId":%d,"locId":%d,"uom":"AUU","quantity":10,"sellingPrice":25.00}"""
            .formatted(catId, locId)).get("id").asLong();
        postJson("/api/shifts/open", "{\"openingFloat\":0}");
        postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":50.00,"lines":[{"itemId":%d,"quantity":2}]}""".formatted(itemId));

        // The Sale itself is audited …
        assertThat(getJson("/api/admin/audit-log?entity=sale").get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
        // … but the transaction log (STOCK_OUT_SALE rows) is on the skip-list.
        assertThat(getJson("/api/admin/audit-log?entity=transactionlog").get("totalElements").asInt()).isEqualTo(0);
    }
}
