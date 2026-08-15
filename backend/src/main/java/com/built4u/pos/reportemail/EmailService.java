package com.built4u.pos.reportemail;

import com.built4u.pos.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails (report attachments) via the Resend HTTP API.
 *
 * <p><b>Inert until keyed.</b> With no {@code app.mail.resend-api-key} configured
 * (the default), {@link #isEnabled()} is false and any send throws a clear
 * "email not configured" error — nothing leaves the server. Drop in a key (and
 * a verified {@code app.mail.from}) to switch it on; no code change required.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final ObjectMapper objectMapper;

    @Value("${app.mail.resend-api-key:}")
    private String apiKey;

    @Value("${app.mail.from:Built4U <onboarding@resend.dev>}")
    private String from;

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    /** True once a provider key is configured; false keeps the feature inert. */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Send {@code body} to {@code to} with one file attachment. Throws if not configured or on API error. */
    public void sendReportEmail(String to, String subject, String body, String filename, byte[] attachment) {
        if (!isEnabled()) {
            throw new BadRequestException(
                "Report email is not configured. An administrator must set app.mail.resend-api-key "
                + "(and app.mail.from) to enable delivery.");
        }
        try {
            Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "text", body,
                "attachments", List.of(Map.of(
                    "filename", filename,
                    "content", Base64.getEncoder().encodeToString(attachment)
                ))
            );
            HttpRequest req = HttpRequest.newBuilder(URI.create(RESEND_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("Resend send failed ({}): {}", res.statusCode(), res.body());
                throw new BadRequestException("Email delivery failed (provider returned " + res.statusCode() + ").");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Email delivery failed: " + e.getMessage());
        }
    }
}
