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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end POS core against BUILT4U: shift open → checkout (stock down) →
 * partial refund (stock up) → void (stock restored) → shift reconciliation.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PosFlowIT {

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
    private static BigDecimal bd(JsonNode n) { return new BigDecimal(n.asText()); }

    private void loginAsAdmin() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(login).get("accessToken").asText();
    }

    @Test
    void checkout_refund_void_and_shift_reconciliation() throws Exception {
        loginAsAdmin();

        long catId = postJson("/api/categories", "{\"name\":\"Bev\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"A1\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"PCS\"}");
        long itemId = postJson("/api/items", """
            {"code":"IT1","name":"Cola","catId":%d,"locId":%d,"uom":"PCS","quantity":20,"sellingPrice":25.00}"""
            .formatted(catId, locId)).get("id").asLong();

        postJson("/api/shifts/open", "{\"openingFloat\":1000.00}");

        // Checkout 2 units CASH → grand 50, change 50 (paid 100); stock 20→18.
        JsonNode saleA = postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":100.00,"lines":[{"itemId":%d,"quantity":2}]}""".formatted(itemId));
        assertThat(bd(saleA.get("grandTotal"))).isEqualByComparingTo("50.00");
        assertThat(bd(saleA.get("change"))).isEqualByComparingTo("50.00");
        assertThat(saleA.get("status").asText()).isEqualTo("COMPLETED");
        String saleANum = saleA.get("salesNumber").asText();
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("18");

        // Refund 1 unit → ₱25 back; stock 18→19; sale still COMPLETED (partial).
        JsonNode ret = postJson("/api/sales/" + saleANum + "/refund",
            "{\"reason\":\"damaged\",\"lines\":[{\"itemId\":%d,\"quantity\":1}]}".formatted(itemId));
        assertThat(bd(ret.get("totalRefunded"))).isEqualByComparingTo("25.00");
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("19");
        JsonNode saleAafter = getJson("/api/sales/" + saleANum);
        assertThat(saleAafter.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(bd(saleAafter.get("lines").get(0).get("refundableQuantity"))).isEqualByComparingTo("1");

        // Checkout 3 units (stock 19→16) then void → stock restored to 19.
        String saleBNum = postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":75.00,"lines":[{"itemId":%d,"quantity":3}]}""".formatted(itemId))
            .get("salesNumber").asText();
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("16");
        JsonNode voided = postJson("/api/sales/" + saleBNum + "/void", "");
        assertThat(voided.get("status").asText()).isEqualTo("VOIDED");
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("19");

        // Live shift preview: cash sales 50 (saleB excluded), refunds 25,
        // expected = 1000 + 50 - 25 = 1025, saleCount 1 (only saleA counts).
        JsonNode cur = getJson("/api/shifts/current");
        assertThat(bd(cur.get("cashSalesTotal"))).isEqualByComparingTo("50.00");
        assertThat(bd(cur.get("cashRefundsTotal"))).isEqualByComparingTo("25.00");
        assertThat(bd(cur.get("expectedCash"))).isEqualByComparingTo("1025.00");
        assertThat(cur.get("saleCount").asInt()).isEqualTo(1);
        String shiftNum = cur.get("shiftNumber").asText();

        JsonNode closed = postJson("/api/shifts/" + shiftNum + "/close", "{\"countedCash\":1025.00}");
        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(bd(closed.get("cashVariance"))).isEqualByComparingTo("0");
    }

    @Test
    void insufficient_stock_is_rejected() throws Exception {
        loginAsAdmin();

        long catId = postJson("/api/categories", "{\"name\":\"Bev2\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"A2\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"BOX\"}");
        long itemId = postJson("/api/items", """
            {"code":"IT2","name":"Rare","catId":%d,"locId":%d,"uom":"BOX","quantity":1,"sellingPrice":10.00}"""
            .formatted(catId, locId)).get("id").asLong();

        mvc.perform(post("/api/sales").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"modeOfPayment":"CASH","payment":9999.00,"lines":[{"itemId":%d,"quantity":999}]}""".formatted(itemId)))
            .andExpect(status().isBadRequest());
    }
}
