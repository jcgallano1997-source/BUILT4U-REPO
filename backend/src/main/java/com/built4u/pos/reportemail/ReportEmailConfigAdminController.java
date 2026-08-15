package com.built4u.pos.reportemail;

import com.built4u.pos.reportemail.dto.ReportEmailConfigDto;
import com.built4u.pos.reportemail.dto.UpdateRecipientRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin: per-report email recipients / subject / body (gated by {@code MOD_DOC_SETTINGS}).
 * {@code enabled} reflects whether a mail provider key is configured; when false,
 * saving recipients is allowed but actual sends stay inert.
 */
@RestController
@RequestMapping("/api/admin/report-email")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_EMAIL_CONFIG')")
public class ReportEmailConfigAdminController {

    private final ReportEmailService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok(Map.of(
            "enabled", service.deliveryEnabled(),
            "configs", service.list()));
    }

    @PutMapping("/{reportCode}")
    public ResponseEntity<ReportEmailConfigDto> save(
        @PathVariable("reportCode") String reportCode,
        @Valid @RequestBody UpdateRecipientRequest req
    ) {
        return ResponseEntity.ok(service.save(reportCode, req));
    }
}
