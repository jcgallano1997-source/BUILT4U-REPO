package com.built4u.pos;

import com.built4u.pos.common.error.ErrorLog;
import com.built4u.pos.common.error.ErrorLogRepository;
import com.built4u.pos.common.error.ErrorLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error log against BUILT4U. record() commits in a REQUIRES_NEW transaction, so
 * this test is intentionally NOT @Transactional — it records one synthetic
 * error, reads it back through the admin API (checking redaction), then deletes
 * the row so BUILT4U stays clean.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ErrorLogFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ErrorLogService errorLogService;
    @Autowired ErrorLogRepository errorLogRepository;

    @Test
    void recordedError_isReadableViaApi_and_redacted() throws Exception {
        String ref = "T" + Long.toString(System.nanoTime()).substring(9); // unique-ish, ≤8 chars
        errorLogService.record(ref, "POST", "/api/test/boom", "admin", "MAIN", "Main Store",
            new IllegalStateException("boom with password=hunter2 leaked"));

        Long createdId = null;
        try {
            var row = errorLogRepository.findAll().stream()
                .filter(e -> ref.equals(e.getRef())).findFirst().orElseThrow();
            createdId = row.getId();

            String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String auth = "Bearer " + json.readTree(login).get("accessToken").asText();

            String res = mvc.perform(get("/api/admin/error-logs?limit=50").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JsonNode list = json.readTree(res);
            JsonNode found = null;
            for (JsonNode e : list) if (ref.equals(e.get("ref").asText())) { found = e; break; }
            assertThat(found).isNotNull();
            assertThat(found.get("username").asText()).isEqualTo("admin");
            assertThat(found.get("exceptionClass").asText()).contains("IllegalStateException");
            // The password value is masked.
            assertThat(found.get("message").asText()).doesNotContain("hunter2");
            assertThat(found.get("message").asText()).contains("***");
        } finally {
            if (createdId != null) errorLogRepository.deleteById(createdId);
        }
    }

    @Test
    void redact_masksCredentialFragments() {
        assertThat(ErrorLogService.redact("token: abc123 and secret=zzz"))
            .doesNotContain("abc123").doesNotContain("zzz").contains("***");
        assertThat(ErrorLogService.redact(null)).isNull();
    }
}
