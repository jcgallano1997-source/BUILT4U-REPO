package com.built4u.pos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Per-IP login rate limiter. Runs in its own context (a low max-attempts via
 * {@code @TestPropertySource}) so its 127.0.0.1 counter is isolated from the
 * rest of the suite. Bad-credential logins avoid touching any real account.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.security.login-rate.max-attempts=3",
    "app.security.login-rate.window-seconds=900"
})
class RateLimitFlowIT {

    @Autowired MockMvc mvc;

    private static final String BODY = """
        {"username":"nobody-rl","password":"x","siteCode":"MAIN"}""";

    @Test
    void loginIsRateLimitedPerIp() throws Exception {
        // First 3 attempts pass the limiter (rejected by auth as 401, not 429).
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        }
        // The 4th trips the limiter.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().is(429));
    }
}
