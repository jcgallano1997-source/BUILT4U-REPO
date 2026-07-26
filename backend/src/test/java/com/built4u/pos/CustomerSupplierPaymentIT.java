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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Customers / suppliers / payment modes + customer-attached checkout against
 * BUILT4U (V5 schema; also validates payment-mode boolean columns). @Transactional
 * rolls back; MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CustomerSupplierPaymentIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private String auth;

    private JsonNode postJson(String url, String body) throws Exception {
        String res = mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    @Test
    void catalogs_and_customer_attached_sale() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(login).get("accessToken").asText();

        // Customer + supplier.
        long custId = postJson("/api/customers", "{\"name\":\"Juan Dela Cruz\",\"contact\":\"0917\"}").get("id").asLong();
        postJson("/api/suppliers", "{\"code\":\"SUP1\",\"name\":\"Acme Supply\"}");
        mvc.perform(post("/api/suppliers").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"SUP1\",\"name\":\"Dup\"}"))
            .andExpect(status().isConflict());

        // Payment modes: seeded (MAIN) list + admin add.
        mvc.perform(get("/api/payment-modes").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code", hasItems("CASH", "GCASH")));
        mvc.perform(post("/api/admin/payment-modes").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"CHARGE","label":"Charge (credit)","surchargeType":"NONE","surchargeValue":0,
                     "isCash":false,"allowsPartial":true,"customerRequired":true,"accountsReceivable":true,
                     "arDueDays":30,"sortOrder":50,"active":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("CHARGE")))
            .andExpect(jsonPath("$.accountsReceivable", is(true)));

        // Item, then checkout with the customer attached (GCASH).
        long catId = postJson("/api/categories", "{\"name\":\"Bev7\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"A7\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"PCS\"}");
        long itemId = postJson("/api/items", """
            {"code":"IT7","name":"Water","catId":%d,"locId":%d,"uom":"PCS","quantity":10,"sellingPrice":30.00}"""
            .formatted(catId, locId)).get("id").asLong();

        JsonNode sale = postJson("/api/sales", """
            {"customerId":%d,"modeOfPayment":"GCASH","payment":30.00,"lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(custId, itemId));
        assertThat_(sale.get("customerName").asText(), "Juan Dela Cruz");
        assertThat_(sale.get("modeOfPayment").asText(), "GCASH");

        // Sales list shows the customer name.
        mvc.perform(get("/api/sales").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerName", is("Juan Dela Cruz")));
    }

    private static void assertThat_(String actual, String expected) {
        if (!expected.equals(actual)) throw new AssertionError("expected '" + expected + "' but was '" + actual + "'");
    }
}
