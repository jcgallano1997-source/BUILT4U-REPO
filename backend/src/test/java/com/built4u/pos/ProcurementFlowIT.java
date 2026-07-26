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
 * Procurement end-to-end against BUILT4U: PO auto-approval, receiving that
 * bumps stock + advances PO status, over-receive rejection, approver routing
 * (DRAFT → admin approves), a direct (PO-less) receipt, and PO cancel.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ProcurementFlowIT {

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
    private void putExpect(String url, String body, int expectStatus) throws Exception {
        mvc.perform(put(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is(expectStatus));
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

    /** Create category + location + uom + item, return the item id. */
    private long seedItem(String suffix, String qty) throws Exception {
        long catId = postJson("/api/categories", "{\"name\":\"PCat" + suffix + "\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"PLoc" + suffix + "\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"PC" + suffix + "\"}");
        return postJson("/api/items", """
            {"code":"P%s","name":"Widget %s","catId":%d,"locId":%d,"uom":"PC%s","quantity":%s,"sellingPrice":30.00}"""
            .formatted(suffix, suffix, catId, locId, suffix, qty)).get("id").asLong();
    }

    @Test
    void po_autoApproves_receiveAdvancesStatus_and_overReceiveRejected() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("A", "10");

        // Admin has no approver mapping → PO auto-approves on create.
        JsonNode po = postJson("/api/purchase-orders", """
            {"supplier":"Acme Corp","deliveryDate":"2026-08-01","remarks":"rush",
             "lines":[{"itemId":%d,"quantity":5,"unitPrice":8.00}]}""".formatted(itemId));
        String poNumber = po.get("poNumber").asText();
        assertThat(po.get("status").asText()).isEqualTo("APPROVED");
        assertThat(po.get("autoApproved").asBoolean()).isTrue();
        assertThat(bd(po.get("grandTotal"))).isEqualByComparingTo("40.00");
        assertThat(bd(po.get("lines").get(0).get("remainingQty"))).isEqualByComparingTo("5");

        // Receive 2 (no unitCost → falls back to PO price 8) → stock 10→12, PARTIALLY_RECEIVED.
        JsonNode gr1 = postJson("/api/goods-receipts", """
            {"poNumber":"%s","lines":[{"itemId":%d,"quantity":2}]}""".formatted(poNumber, itemId));
        assertThat(bd(gr1.get("grandTotal"))).isEqualByComparingTo("16.00");
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("12");
        JsonNode poAfter1 = getJson("/api/purchase-orders/" + poNumber);
        assertThat(poAfter1.get("status").asText()).isEqualTo("PARTIALLY_RECEIVED");
        assertThat(bd(poAfter1.get("lines").get(0).get("receivedQty"))).isEqualByComparingTo("2");
        assertThat(bd(poAfter1.get("lines").get(0).get("remainingQty"))).isEqualByComparingTo("3");

        // Receive remaining 3 → stock 12→15, RECEIVED; cost price refreshed to 8.00.
        postJson("/api/goods-receipts", """
            {"poNumber":"%s","lines":[{"itemId":%d,"quantity":3}]}""".formatted(poNumber, itemId));
        JsonNode itemAfter = getJson("/api/items/" + itemId);
        assertThat(bd(itemAfter.get("quantity"))).isEqualByComparingTo("15");
        assertThat(bd(itemAfter.get("costPrice"))).isEqualByComparingTo("8.00");
        assertThat(getJson("/api/purchase-orders/" + poNumber).get("status").asText()).isEqualTo("RECEIVED");

        // Over-receive against a RECEIVED PO is rejected (400).
        postExpect("/api/goods-receipts", """
            {"poNumber":"%s","lines":[{"itemId":%d,"quantity":1}]}""".formatted(poNumber, itemId), 400);
    }

    @Test
    void approverRouting_makesDraft_thenAdminApproves() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("B", "0");

        // A second user to route approvals to.
        long buyerId = postJson("/api/admin/users", """
            {"username":"buyerB","fullName":"Buyer B","initialPassword":"buyer12345",
             "roleCodes":["MANAGER"],"siteCodes":["MAIN"]}""").get("id").asLong();

        // Find admin's user id from the approver list, then route admin → buyerB.
        long adminId = -1;
        for (JsonNode row : getJson("/api/po-approvers")) {
            if ("admin".equals(row.get("username").asText())) { adminId = row.get("userId").asLong(); break; }
        }
        assertThat(adminId).isGreaterThan(0);
        putExpect("/api/po-approvers/" + adminId, "{\"approverUserId\":%d}".formatted(buyerId), 204);

        // With a mapping in place, admin's PO now starts as DRAFT.
        JsonNode po = postJson("/api/purchase-orders", """
            {"supplier":"Beta Supply","lines":[{"itemId":%d,"quantity":4,"unitPrice":5.00}]}""".formatted(itemId));
        String poNumber = po.get("poNumber").asText();
        assertThat(po.get("status").asText()).isEqualTo("DRAFT");
        assertThat(po.get("autoApproved").asBoolean()).isFalse();
        // Admin is wildcard → allowed to approve regardless of routing.
        assertThat(po.get("canCurrentUserApprove").asBoolean()).isTrue();

        // Pending-my-approval (admin = wildcard) sees the DRAFT.
        assertThat(getJson("/api/purchase-orders/pending-my-approval").size()).isGreaterThanOrEqualTo(1);

        // Approve → APPROVED, autoApproved false, approvedBy admin.
        JsonNode approved = postJson("/api/purchase-orders/" + poNumber + "/approve", "");
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.get("autoApproved").asBoolean()).isFalse();
        assertThat(approved.get("approvedBy").asText()).isEqualTo("admin");
    }

    @Test
    void directReceipt_bumpsStock_and_poCancelBlocksReceiving() throws Exception {
        loginAsAdmin();
        long itemId = seedItem("C", "1");

        // Direct GR (no PO): stock 1→5, supplier is free text.
        postJson("/api/goods-receipts", """
            {"supplier":"Walk-in Vendor","reference":"INV-77",
             "lines":[{"itemId":%d,"quantity":4,"unitCost":12.50}]}""".formatted(itemId));
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("5");

        // Auto-approved PO, then cancel it → CANCELLED, and receiving is blocked.
        String poNumber = postJson("/api/purchase-orders", """
            {"supplier":"Gamma","lines":[{"itemId":%d,"quantity":3,"unitPrice":9.00}]}""".formatted(itemId))
            .get("poNumber").asText();
        JsonNode cancelled = postJson("/api/purchase-orders/" + poNumber + "/cancel", "");
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        postExpect("/api/goods-receipts", """
            {"poNumber":"%s","lines":[{"itemId":%d,"quantity":1}]}""".formatted(poNumber, itemId), 400);
    }
}
