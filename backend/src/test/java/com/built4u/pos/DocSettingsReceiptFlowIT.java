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
 * Document settings + PDFs against BUILT4U: branding saves/reads back, a report
 * PDF renders with that branding, and a sale produces a receipt PDF.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class DocSettingsReceiptFlowIT {

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
    private byte[] getPdf(String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(r -> assertThat(r.getResponse().getContentType()).contains("pdf"))
            .andReturn().getResponse().getContentAsByteArray();
    }

    private void loginAsAdmin() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(login).get("accessToken").asText();
    }

    @Test
    void branding_savesReadsBack_and_pdfsRender() throws Exception {
        loginAsAdmin();

        String saved = mvc.perform(put("/api/admin/doc-settings").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"businessName":"Aling Nena Store","addressLine":"123 Rizal St","contactLine":"0917-000-0000",
                     "tin":"123-456-789","footerNote":"BIR permit #42","accentColor":"#0F766E",
                     "receiptTitle":"OFFICIAL RECEIPT","receiptFooter":"Salamat po!"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode d = json.readTree(saved);
        assertThat(d.get("businessName").asText()).isEqualTo("Aling Nena Store");
        assertThat(d.get("accentColor").asText()).isEqualTo("#0F766E");
        assertThat(d.get("usingDefault").asBoolean()).isFalse();

        JsonNode reread = getJson("/api/admin/doc-settings");
        assertThat(reread.get("receiptTitle").asText()).isEqualTo("OFFICIAL RECEIPT");

        // A report PDF renders (branding applied in the letterhead).
        byte[] report = getPdf("/api/reports/inventory-valuation?format=pdf");
        assertThat(new String(report, 0, 4)).isEqualTo("%PDF");

        // Ring a sale, then fetch its receipt PDF.
        long catId = postJson("/api/categories", "{\"name\":\"DocCat\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"DocLoc\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"DOC\"}");
        long itemId = postJson("/api/items", """
            {"code":"DOC1","name":"Doc Item","catId":%d,"locId":%d,"uom":"DOC","quantity":10,"sellingPrice":30.00}"""
            .formatted(catId, locId)).get("id").asLong();
        postJson("/api/shifts/open", "{\"openingFloat\":0}");
        String saleNum = postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":100.00,"lines":[{"itemId":%d,"quantity":2}]}""".formatted(itemId))
            .get("salesNumber").asText();

        byte[] receipt = getPdf("/api/sales/" + saleNum + "/receipt");
        assertThat(new String(receipt, 0, 4)).isEqualTo("%PDF");
        assertThat(receipt.length).isGreaterThan(500);
    }
}
