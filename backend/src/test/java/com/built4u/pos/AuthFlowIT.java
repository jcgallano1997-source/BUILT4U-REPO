package com.built4u.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end auth slice test against the BUILT4U schema (no Tomcat — MOCK web
 * env, so it runs even where the NIO loopback self-pipe is unavailable).
 *
 * <p>Relies on the V2 seed (admin/MAIN) + DataSeeder rewriting the placeholder
 * hash to bcrypt(app.security.seed-admin-password) = "admin123" on context start.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void login_returnsTokens_and_me_and_refresh_work() throws Exception {
        // 1) login as the seeded admin
        String loginBody = """
            {"username":"admin","password":"admin123","siteCode":"MAIN"}""";

        String loginJson = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
            .andExpect(jsonPath("$.refreshToken", not(emptyOrNullString())))
            .andExpect(jsonPath("$.user.username", is("admin")))
            .andExpect(jsonPath("$.user.roles", hasItem("ADMIN")))
            .andExpect(jsonPath("$.site.code", is("MAIN")))
            .andReturn().getResponse().getContentAsString();

        JsonNode node = json.readTree(loginJson);
        String access = node.get("accessToken").asText();
        String refresh = node.get("refreshToken").asText();

        // 2) /me with the access token
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username", is("admin")))
            .andExpect(jsonPath("$.modules", hasItem("POS")));   // ADMIN wildcard expands to all modules

        // 3) refresh rotates and returns a new pair
        String refreshBody = json.createObjectNode().put("refreshToken", refresh).toString();
        mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())));
    }

    @Test
    void login_wrongPassword_is401() throws Exception {
        String body = """
            {"username":"admin","password":"wrongwrong","siteCode":"MAIN"}""";
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }
}
