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
 * Vouchers & loyalty end-to-end against BUILT4U:
 *   Voucher — validate, apply at checkout (discount + single-use consume),
 *             reject when exhausted, release on void.
 *   Loyalty — earn points on a sale, redeem a reward for points, claw points
 *             back when the earning sale is voided.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class VoucherLoyaltyFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String auth;

    private JsonNode postJson(String url, String body) throws Exception {
        var result = mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        int st = result.getResponse().getStatus();
        String res = result.getResponse().getContentAsString();
        if (st < 200 || st >= 300) throw new AssertionError("POST " + url + " -> " + st + " : " + res);
        return json.readTree(res);
    }
    private JsonNode getJson(String url) throws Exception {
        String res = mvc.perform(get(url).header("Authorization", auth))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }
    private JsonNode putJson(String url, String body) throws Exception {
        String res = mvc.perform(put(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }
    private void postExpect(String url, String body, int expectStatus) throws Exception {
        mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is(expectStatus));
    }
    private static BigDecimal bd(JsonNode n) { return new BigDecimal(n.asText()); }

    private void loginAsAdmin() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(login).get("accessToken").asText();
    }

    private long seedItem(String suffix, String qty, String price) throws Exception {
        long catId = postJson("/api/categories", "{\"name\":\"VCat" + suffix + "\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"VLoc" + suffix + "\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"VU" + suffix + "\"}");
        return postJson("/api/items", """
            {"code":"V%s","name":"Item %s","catId":%d,"locId":%d,"uom":"VU%s","quantity":%s,"sellingPrice":%s}"""
            .formatted(suffix, suffix, catId, locId, suffix, qty, price)).get("id").asLong();
    }

    // ── Vouchers ────────────────────────────────────────────────────────────────

    @Test
    void voucher_appliesAtCheckout_singleUse_and_releasedOnVoid() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("A", "10", "100.00");
        postJson("/api/shifts/open", "{\"openingFloat\":0}");

        // 10% single-use voucher.
        postJson("/api/admin/vouchers", """
            {"code":"SAVE10","description":"10% off","discountType":"PERCENT","discountValue":10,
             "usageLimit":1,"active":true}""");

        // Validate against a ₱100 subtotal → ₱10 off.
        JsonNode ev = postJson("/api/vouchers/validate", "{\"code\":\"SAVE10\",\"subtotal\":100.00}");
        assertThat(ev.get("valid").asBoolean()).isTrue();
        assertThat(bd(ev.get("discountAmount"))).isEqualByComparingTo("10.00");

        // Checkout with the voucher → grand total 90, discountAll 10.
        JsonNode sale = postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":90.00,"voucherCode":"SAVE10","lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(itemId));
        String saleNum = sale.get("salesNumber").asText();
        assertThat(bd(sale.get("grandTotal"))).isEqualByComparingTo("90.00");
        assertThat(bd(sale.get("discountAll"))).isEqualByComparingTo("10.00");

        // Same voucher again → exhausted → 400.
        postExpect("/api/sales", """
            {"modeOfPayment":"CASH","payment":90.00,"voucherCode":"SAVE10","lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(itemId), 400);

        // Void the first sale → the voucher is released → validates again.
        postJson("/api/sales/" + saleNum + "/void", "");
        JsonNode ev2 = postJson("/api/vouchers/validate", "{\"code\":\"SAVE10\",\"subtotal\":100.00}");
        assertThat(ev2.get("valid").asBoolean()).isTrue();
    }

    // ── Loyalty ───────────────────────────────────────────────────────────────

    @Test
    void loyalty_earnsOnSale_and_rewardRedeemSpendsPoints() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("B", "10", "100.00");
        putJson("/api/admin/loyalty-config", "{\"pointsRate\":10,\"redeemValue\":1}");
        long custId = postJson("/api/customers", "{\"name\":\"Loy Al\",\"points\":0,\"creditLimit\":0}").get("id").asLong();
        postJson("/api/shifts/open", "{\"openingFloat\":0}");

        // Sale of ₱100 with the customer → earns 10 points (10%).
        postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":100.00,"customerId":%d,"lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(custId, itemId));
        JsonNode ledger = getJson("/api/loyalty/ledger?customerId=" + custId);
        assertThat(bd(ledger.get("liveBalance"))).isEqualByComparingTo("10");
        assertThat(ledger.get("content").get(0).get("entryType").asText()).isEqualTo("EARN");
        assertThat(bd(ledger.get("content").get(0).get("points"))).isEqualByComparingTo("10");

        // A free-text reward costing 8 points → redeem → balance 2.
        long rewardId = postJson("/api/admin/loyalty-rewards", """
            {"name":"Free coffee","pointsCost":8,"rewardType":"FREETEXT","sortOrder":0,"active":true}""")
            .get("id").asLong();
        JsonNode redeemed = postJson("/api/loyalty/redeem-reward",
            "{\"customerId\":%d,\"rewardId\":%d}".formatted(custId, rewardId));
        assertThat(bd(redeemed.get("newBalance"))).isEqualByComparingTo("2");
        assertThat(bd(getJson("/api/loyalty/ledger?customerId=" + custId).get("liveBalance"))).isEqualByComparingTo("2");
    }

    @Test
    void voidingSale_clawsBackEarnedPoints() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("C", "10", "100.00");
        putJson("/api/admin/loyalty-config", "{\"pointsRate\":10,\"redeemValue\":1}");
        long custId = postJson("/api/customers", "{\"name\":\"Voi Der\",\"points\":0,\"creditLimit\":0}").get("id").asLong();
        postJson("/api/shifts/open", "{\"openingFloat\":0}");

        String saleNum = postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":100.00,"customerId":%d,"lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(custId, itemId)).get("salesNumber").asText();
        assertThat(bd(getJson("/api/loyalty/ledger?customerId=" + custId).get("liveBalance"))).isEqualByComparingTo("10");

        postJson("/api/sales/" + saleNum + "/void", "");
        assertThat(bd(getJson("/api/loyalty/ledger?customerId=" + custId).get("liveBalance"))).isEqualByComparingTo("0");
    }
}
