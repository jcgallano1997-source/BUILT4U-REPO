package com.built4u.pos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin CRUD (sites / roles / users) against BUILT4U. @Transactional → the test's
 * writes roll back, so it's fully repeatable. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AdminFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String adminToken() throws Exception {
        // Login runs in a nested transaction that commits the refresh token; keep
        // the seeded admin usable across the test transaction.
        String body = """
            {"username":"admin","password":"admin123","siteCode":"MAIN"}""";
        String res = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("accessToken").asText();
    }

    @Test
    void admin_can_manage_sites_roles_and_users() throws Exception {
        String token = adminToken();
        var auth = "Bearer " + token;

        // --- Sites: module meta + create + list ---
        mvc.perform(get("/api/admin/roles/_meta/modules").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(45)));

        mvc.perform(post("/api/admin/sites").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"BR2","name":"Branch 2","address":"123 Main St"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code", is("BR2")))
            .andExpect(jsonPath("$.active", is(true)));

        mvc.perform(get("/api/admin/sites").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code", hasItems("MAIN", "BR2")));

        // --- Roles: create a custom role ---
        mvc.perform(post("/api/admin/roles").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"VIEWER","name":"Report Viewer","description":"Reports only","moduleCodes":["SALES_REPORTS"]}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code", is("VIEWER")))
            .andExpect(jsonPath("$.builtIn", is(false)))
            .andExpect(jsonPath("$.moduleCodes", hasItem("SALES_REPORTS")));

        // --- Users: pickers + create + list ---
        mvc.perform(get("/api/admin/users/_meta/roles").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code", hasItems("ADMIN", "MANAGER", "CASHIER", "VIEWER")));

        mvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"cashier1","fullName":"Cashier One","initialPassword":"cashier123",
                     "roleCodes":["CASHIER"],"siteCodes":["MAIN"]}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username", is("cashier1")))
            .andExpect(jsonPath("$.roles[*].code", hasItem("CASHIER")))
            .andExpect(jsonPath("$.sites[*].code", hasItem("MAIN")));

        mvc.perform(get("/api/admin/users").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].username", hasItems("admin", "cashier1")));

        // roll back the writes (keep BUILT4U clean)
        if (TestTransaction.isActive()) TestTransaction.flagForRollback();
    }

    @Test
    void duplicate_site_code_conflicts() throws Exception {
        var auth = "Bearer " + adminToken();
        // MAIN already exists (seed) → 409
        mvc.perform(post("/api/admin/sites").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"MAIN","name":"Dup","address":null}"""))
            .andExpect(status().isConflict());
    }
}
