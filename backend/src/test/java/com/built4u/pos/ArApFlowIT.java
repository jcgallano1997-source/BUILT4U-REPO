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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accounts receivable & payable end-to-end against BUILT4U:
 *   AR — a credit sale opens a receivable; collections close it; the credit
 *        limit blocks an oversized credit sale; voiding cancels the receivable.
 *   AP — a goods receipt from an AP-enabled supplier auto-creates a payable
 *        (a non-AP supplier creates none); disbursements close it; a manual
 *        expense payable can be created and paid.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ArApFlowIT {

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
        long catId = postJson("/api/categories", "{\"name\":\"ArCat" + suffix + "\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"ArLoc" + suffix + "\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"AU" + suffix + "\"}");
        return postJson("/api/items", """
            {"code":"AR%s","name":"Good %s","catId":%d,"locId":%d,"uom":"AU%s","quantity":%s,"sellingPrice":%s}"""
            .formatted(suffix, suffix, catId, locId, suffix, qty, price)).get("id").asLong();
    }

    /** Create an accounts-receivable payment mode; returns its code. */
    private String seedArMode(String code) throws Exception {
        postJson("/api/admin/payment-modes", """
            {"code":"%s","label":"On account %s","surchargeType":"NONE","surchargeValue":0,
             "isCash":false,"allowsPartial":true,"customerRequired":true,"accountsReceivable":true,
             "arDueDays":30,"sortOrder":90,"active":true}""".formatted(code, code));
        return code;
    }

    private long seedCustomer(String name, String creditLimit) throws Exception {
        return postJson("/api/customers", """
            {"name":"%s","points":0,"creditLimit":%s}""".formatted(name, creditLimit)).get("id").asLong();
    }

    // ── AR ────────────────────────────────────────────────────────────────────

    @Test
    void creditSale_opensReceivable_and_collectionsCloseIt() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("A", "100", "300.00");
        String mode = seedArMode("CHARGEA");
        long custId = seedCustomer("Ana Cruz", "0");   // 0 = no limit
        postJson("/api/shifts/open", "{\"openingFloat\":0}");

        // Credit sale: 2 @ 300 = 600, nothing paid → receivable balance 600.
        JsonNode sale = postJson("/api/sales", """
            {"modeOfPayment":"%s","payment":0,"customerId":%d,"lines":[{"itemId":%d,"quantity":2}]}"""
            .formatted(mode, custId, itemId));
        assertThat(sale.get("status").asText()).isEqualTo("COMPLETED");

        JsonNode rows = getJson("/api/receivables").get("content");
        assertThat(rows.size()).isEqualTo(1);
        JsonNode r = rows.get(0);
        assertThat(bd(r.get("balance"))).isEqualByComparingTo("600.00");
        assertThat(r.get("status").asText()).isEqualTo("OPEN");
        long rid = r.get("id").asLong();

        // Collect 250 → PARTIAL (350 left), then 350 → PAID.
        JsonNode afterPartial = postJson("/api/receivables/" + rid + "/payments", "{\"amount\":250.00}");
        assertThat(afterPartial.get("receivable").get("status").asText()).isEqualTo("PARTIAL");
        assertThat(bd(afterPartial.get("receivable").get("balance"))).isEqualByComparingTo("350.00");

        JsonNode afterFull = postJson("/api/receivables/" + rid + "/payments", "{\"amount\":350.00,\"note\":\"final\"}");
        assertThat(afterFull.get("receivable").get("status").asText()).isEqualTo("PAID");
        assertThat(bd(afterFull.get("receivable").get("balance"))).isEqualByComparingTo("0");
        assertThat(afterFull.get("payments").size()).isEqualTo(2);

        // Over-collect on a fully paid receivable → 400.
        postExpect("/api/receivables/" + rid + "/payments", "{\"amount\":1.00}", 400);
    }

    @Test
    void creditLimit_blocksOversizedCreditSale() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("B", "100", "500.00");
        String mode = seedArMode("CHARGEB");
        long custId = seedCustomer("Bo Lim", "700");   // limit ₱700
        postJson("/api/shifts/open", "{\"openingFloat\":0}");

        // First credit sale 500 → OK (outstanding 500 ≤ 700).
        postJson("/api/sales", """
            {"modeOfPayment":"%s","payment":0,"customerId":%d,"lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(mode, custId, itemId));

        // Second credit sale 500 → outstanding would be 1000 > 700 → rejected.
        postExpect("/api/sales", """
            {"modeOfPayment":"%s","payment":0,"customerId":%d,"lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(mode, custId, itemId), 400);
    }

    @Test
    void voidingCreditSale_cancelsReceivable() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("C", "100", "150.00");
        String mode = seedArMode("CHARGEC");
        long custId = seedCustomer("Cita Reyes", "0");
        postJson("/api/shifts/open", "{\"openingFloat\":0}");

        String saleNum = postJson("/api/sales", """
            {"modeOfPayment":"%s","payment":0,"customerId":%d,"lines":[{"itemId":%d,"quantity":1}]}"""
            .formatted(mode, custId, itemId)).get("salesNumber").asText();
        long rid = getJson("/api/receivables").get("content").get(0).get("id").asLong();

        postJson("/api/sales/" + saleNum + "/void", "");
        assertThat(getJson("/api/receivables/" + rid).get("receivable").get("status").asText()).isEqualTo("CANCELLED");
    }

    // ── AP ────────────────────────────────────────────────────────────────────

    @Test
    void goodsReceiptFromApSupplier_createsPayable_thenDisbursementsCloseIt() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("D", "0", "10.00");

        // AP-enabled supplier, 15-day terms.
        postJson("/api/suppliers", """
            {"code":"APT","name":"AP Traders","apEnabled":true,"payableDays":15}""");

        // PO (auto-approved) → receive in full → payable auto-created for ₱30.
        String poNumber = postJson("/api/purchase-orders", """
            {"supplier":"AP Traders","lines":[{"itemId":%d,"quantity":3,"unitPrice":10.00}]}""".formatted(itemId))
            .get("poNumber").asText();
        postJson("/api/goods-receipts", """
            {"poNumber":"%s","lines":[{"itemId":%d,"quantity":3}]}""".formatted(poNumber, itemId));

        JsonNode rows = getJson("/api/payables?source=PURCHASE").get("content");
        assertThat(rows.size()).isEqualTo(1);
        JsonNode p = rows.get(0);
        assertThat(p.get("source").asText()).isEqualTo("PURCHASE");
        assertThat(bd(p.get("balance"))).isEqualByComparingTo("30.00");
        long pid = p.get("id").asLong();

        JsonNode partial = postJson("/api/payables/" + pid + "/payments", "{\"amount\":10.00}");
        assertThat(partial.get("payable").get("status").asText()).isEqualTo("PARTIAL");
        JsonNode paid = postJson("/api/payables/" + pid + "/payments", "{\"amount\":20.00}");
        assertThat(paid.get("payable").get("status").asText()).isEqualTo("PAID");
    }

    @Test
    void nonApSupplierReceipt_createsNoPayable_and_expensePayableCanBePaid() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("E", "0", "5.00");

        // Supplier WITHOUT AP → receiving creates no payable.
        postJson("/api/suppliers", """
            {"code":"NAP","name":"Cash Vendor","apEnabled":false,"payableDays":0}""");
        String poNumber = postJson("/api/purchase-orders", """
            {"supplier":"Cash Vendor","lines":[{"itemId":%d,"quantity":2,"unitPrice":5.00}]}""".formatted(itemId))
            .get("poNumber").asText();
        JsonNode gr = postJson("/api/goods-receipts", """
            {"poNumber":"%s","lines":[{"itemId":%d,"quantity":2}]}""".formatted(poNumber, itemId));
        String grNumber = gr.get("grNumber").asText();
        assertThat(getJson("/api/payables?search=" + grNumber).get("content").size()).isEqualTo(0);

        // Manual EXPENSE payable → pay in full.
        LocalDate due = LocalDate.now().plusDays(7);
        long pid = postJson("/api/payables", """
            {"category":"utilities","payeeName":"Meralco","description":"June bill","amount":1500.00,"dueDate":"%s"}"""
            .formatted(due)).get("id").asLong();
        JsonNode paid = postJson("/api/payables/" + pid + "/payments", "{\"amount\":1500.00}");
        assertThat(paid.get("payable").get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("payable").get("source").asText()).isEqualTo("EXPENSE");
    }
}
