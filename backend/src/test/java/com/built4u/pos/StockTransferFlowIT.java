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
 * Cross-site stock transfer end-to-end against BUILT4U:
 *   ship MAIN→BR2 (source stock down, IN_TRANSIT) → receive at BR2 (dest stock
 *   up, item auto-created, RECEIVED); cancel restores source stock; the
 *   allow-list policy blocks a disallowed destination while permitting a listed
 *   one. Exercises two site contexts by re-logging into each site.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class StockTransferFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String auth;
    private void use(String token) { this.auth = "Bearer " + token; }

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
    private void postExpect(String url, String body, int expectStatus) throws Exception {
        mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is(expectStatus));
    }
    private static BigDecimal bd(JsonNode n) { return new BigDecimal(n.asText()); }

    /** Log into a site and return the access token. */
    private String token(String siteCode) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\",\"siteCode\":\"%s\"}".formatted(siteCode)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("accessToken").asText();
    }

    /** Create a site (returns its id) using the current token. */
    private long createSite(String code, String name) throws Exception {
        String res = mvc.perform(post("/api/admin/sites").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"%s\",\"name\":\"%s\",\"address\":null}".formatted(code, name)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    /** Grant the admin user access to the given site codes (keeps ADMIN role). */
    private void grantAdminSites(String... siteCodes) throws Exception {
        long adminId = -1;
        for (JsonNode u : getJson("/api/admin/users")) {
            if ("admin".equals(u.get("username").asText())) { adminId = u.get("id").asLong(); break; }
        }
        assertThat(adminId).isGreaterThan(0);
        String codes = String.join("\",\"", siteCodes);
        mvc.perform(put("/api/admin/users/" + adminId).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Administrator","active":true,"roleCodes":["ADMIN"],"siteCodes":["%s"]}""".formatted(codes)))
            .andExpect(status().isOk());
    }

    /** Create category + location + uom + item at the current site; return item id. */
    private long seedItem(String suffix, String qty) throws Exception {
        long catId = postJson("/api/categories", "{\"name\":\"XCat" + suffix + "\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"XLoc" + suffix + "\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"XU" + suffix + "\"}");
        return postJson("/api/items", """
            {"code":"XF%s","name":"Xfer %s","catId":%d,"locId":%d,"uom":"XU%s","quantity":%s,"sellingPrice":40.00}"""
            .formatted(suffix, suffix, catId, locId, suffix, qty)).get("id").asLong();
    }

    /** Give a site a category + location so transfer-receive can auto-create items. */
    private void seedRefs(String suffix) throws Exception {
        postJson("/api/categories", "{\"name\":\"DCat" + suffix + "\"}");
        postJson("/api/locations", "{\"name\":\"DLoc" + suffix + "\"}");
    }

    @Test
    void ship_thenReceive_movesStockAcrossSites() throws Exception {
        String main = token("MAIN");
        use(main);
        long br2 = createSite("BR2", "Branch 2");
        grantAdminSites("MAIN", "BR2");

        // Destination needs an active category + location to auto-create the item.
        String br2Tok = token("BR2");
        use(br2Tok); seedRefs("B2");

        // Ship 5 of an item that only exists at MAIN.
        use(main);
        long itemId = seedItem("S", "20");
        JsonNode shipped = postJson("/api/stock-transfers", """
            {"destSiteId":%d,"remarks":"restock","lines":[{"itemId":%d,"quantity":5}]}""".formatted(br2, itemId));
        String tn = shipped.get("header").get("transferNumber").asText();
        assertThat(shipped.get("header").get("status").asText()).isEqualTo("IN_TRANSIT");
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("15");

        // Receive at BR2 → RECEIVED; item auto-created there with qty 5.
        use(br2Tok);
        JsonNode received = postJson("/api/stock-transfers/" + tn + "/receive", "");
        assertThat(received.get("header").get("status").asText()).isEqualTo("RECEIVED");
        JsonNode br2Items = getJson("/api/items?search=XFS");
        assertThat(br2Items.size()).isEqualTo(1);
        assertThat(bd(br2Items.get(0).get("quantity"))).isEqualByComparingTo("5");
    }

    @Test
    void cancel_restoresSourceStock() throws Exception {
        String main = token("MAIN");
        use(main);
        long br2 = createSite("BR2", "Branch 2");
        grantAdminSites("MAIN", "BR2");

        long itemId = seedItem("C", "10");
        String tn = postJson("/api/stock-transfers", """
            {"destSiteId":%d,"lines":[{"itemId":%d,"quantity":4}]}""".formatted(br2, itemId))
            .get("header").get("transferNumber").asText();
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("6");

        // Cancel at source (still IN_TRANSIT) → stock restored to 10.
        JsonNode cancelled = postJson("/api/stock-transfers/" + tn + "/cancel", "");
        assertThat(cancelled.get("header").get("status").asText()).isEqualTo("CANCELLED");
        assertThat(bd(getJson("/api/items/" + itemId).get("quantity"))).isEqualByComparingTo("10");
    }

    @Test
    void policy_blocksDisallowedDestination_and_permitsListedOne() throws Exception {
        String main = token("MAIN");
        use(main);
        long br2 = createSite("BR2", "Branch 2");
        long br3 = createSite("BR3", "Branch 3");
        grantAdminSites("MAIN", "BR2", "BR3");
        long mainId = siteIdByCode("MAIN");
        long itemId = seedItem("P", "50");

        // Add a single rule MAIN→BR3 → policy becomes ENFORCED.
        postJson("/api/stock-transfer-policy", """
            {"sourceSiteId":%d,"destSiteId":%d}""".formatted(mainId, br3));

        // Destinations now list only BR3.
        JsonNode dests = getJson("/api/stock-transfers/destinations");
        assertThat(dests.size()).isEqualTo(1);
        assertThat(dests.get(0).get("id").asLong()).isEqualTo(br3);

        // Ship to BR2 (not allowed) → 400; ship to BR3 (allowed) → OK.
        postExpect("/api/stock-transfers", """
            {"destSiteId":%d,"lines":[{"itemId":%d,"quantity":2}]}""".formatted(br2, itemId), 400);
        JsonNode ok = postJson("/api/stock-transfers", """
            {"destSiteId":%d,"lines":[{"itemId":%d,"quantity":2}]}""".formatted(br3, itemId));
        assertThat(ok.get("header").get("status").asText()).isEqualTo("IN_TRANSIT");
    }

    /** Resolve a site's id from the admin sites list. */
    private long siteIdByCode(String code) throws Exception {
        for (JsonNode s : getJson("/api/admin/sites")) {
            if (code.equals(s.get("code").asText())) return s.get("id").asLong();
        }
        throw new AssertionError("site " + code + " not found");
    }
}
