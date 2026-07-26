package com.built4u.pos;

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
 * Reference-data + inventory CRUD against BUILT4U (V3 schema). @Transactional →
 * rolls back. MOCK web env avoids the loopback issue. Also proves the composite-key
 * + sequence entities validate against the pos_ schema and site-scoping works.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class InventoryFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token() throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("accessToken").asText();
    }

    private long idOf(String responseJson) throws Exception {
        return json.readTree(responseJson).get("id").asLong();
    }

    @Test
    void full_reference_and_inventory_flow() throws Exception {
        var auth = "Bearer " + token();

        // Category
        String catRes = mvc.perform(post("/api/categories").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Beverages"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Beverages")))
            .andReturn().getResponse().getContentAsString();
        long catId = idOf(catRes);

        // Location
        String locRes = mvc.perform(post("/api/locations").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Aisle 1","capacity":100}"""))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        long locId = idOf(locRes);

        // UOM
        mvc.perform(post("/api/uoms").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"uom":"PCS"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uom", is("PCS")));

        // Item — joins Category + Location, stock level OK (qty 10 > warning 5)
        String itemRes = mvc.perform(post("/api/items").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"code":"ITM001","name":"Cola 500ml","catId":%d,"locId":%d,"uom":"PCS",
                     "quantity":10,"sellingPrice":25.00,"costPrice":18.00,"warning":5,"critical":2}"""
                    .formatted(catId, locId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("ITM001")))
            .andExpect(jsonPath("$.categoryName", is("Beverages")))
            .andExpect(jsonPath("$.locationName", is("Aisle 1")))
            .andExpect(jsonPath("$.stockLevel", is("OK")))
            .andReturn().getResponse().getContentAsString();
        long itemId = idOf(itemRes);

        // List
        mvc.perform(get("/api/items").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code", hasItem("ITM001")));

        // Adjust stock down to 1 → CRITICAL (<= critical 2)
        mvc.perform(post("/api/items/" + itemId + "/adjust").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"delta":-9,"reason":"Sold at counter"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity", is(1)))
            .andExpect(jsonPath("$.stockLevel", is("CRITICAL")));

        // Duplicate item code → 409
        mvc.perform(post("/api/items").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"code":"ITM001","name":"Dup","catId":%d,"locId":%d,"uom":"PCS",
                     "quantity":0,"sellingPrice":1.00}""".formatted(catId, locId)))
            .andExpect(status().isConflict());
    }

    @Test
    void item_with_unknown_category_is_404() throws Exception {
        var auth = "Bearer " + token();
        mvc.perform(post("/api/items").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"code":"X1","name":"X","catId":999999,"locId":999999,"uom":"NOPE",
                     "quantity":0,"sellingPrice":1.00}"""))
            .andExpect(status().isNotFound());
    }
}
