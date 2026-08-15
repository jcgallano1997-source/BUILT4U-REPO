package com.built4u.pos.reportemail;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.report.export.ExportResponses;
import com.built4u.pos.report.export.ExportTable;
import com.built4u.pos.report.export.ReportPdfExporter;
import com.built4u.pos.report.export.ReportXlsxExporter;
import com.built4u.pos.reportemail.dto.ReportEmailConfigDto;
import com.built4u.pos.reportemail.dto.UpdateRecipientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Emails a generated report to its configured recipient, reusing the same PDF/xlsx
 * exporters as the download path. Subject/body come from the per-report config
 * (generated defaults when blank); recipient falls back to the global
 * {@code app.mail.default-recipient}. Actual delivery is inert until a provider
 * key is set (see {@link EmailService}).
 */
@Service
@RequiredArgsConstructor
public class ReportEmailService {

    private final ReportEmailConfigRepository repo;
    private final ReportPdfExporter pdfExporter;
    private final ReportXlsxExporter xlsxExporter;
    private final EmailService emailService;

    @Value("${app.mail.default-recipient:}")
    private String defaultRecipient;

    /** True once the provider is keyed — lets the UI show/hide the Email action. */
    public boolean deliveryEnabled() {
        return emailService.isEnabled();
    }

    public ResponseEntity<Void> deliver(String reportCode, String fmt, ExportTable table) throws IOException {
        long siteId = TenantContext.requireSiteId();
        ReportEmailConfig cfg = repo.findBySiteIdAndReportCode(siteId, reportCode).orElse(null);

        String to = cfg != null && notBlank(cfg.getRecipientEmail()) ? cfg.getRecipientEmail().trim()
            : (notBlank(defaultRecipient) ? defaultRecipient.trim() : null);
        if (to == null) {
            throw new BadRequestException(
                "No email recipient configured for this report. Set one in Admin → Report email.");
        }

        String label = cfg != null && notBlank(cfg.getLabel()) ? cfg.getLabel() : reportCode;
        String subject = cfg != null && notBlank(cfg.getSubject()) ? cfg.getSubject().trim()
            : "Built4U report — " + label;
        String body = cfg != null && notBlank(cfg.getBody()) ? cfg.getBody()
            : "Hi,\n\nAttached is the " + label + " report generated from Built4U.\n\n— Built4U";

        boolean pdf = ExportResponses.FORMAT_PDF.equals(fmt);
        byte[] bytes = pdf ? pdfExporter.export(table) : xlsxExporter.export(table);
        String filename = reportCode + "-" + LocalDate.now() + (pdf ? ".pdf" : ".xlsx");
        emailService.sendReportEmail(to, subject, body, filename, bytes);
        return ResponseEntity.noContent().build();
    }

    // ── Admin CRUD ───────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ReportEmailConfigDto> list() {
        long siteId = TenantContext.requireSiteId();
        return repo.findBySiteIdOrderByReportCodeAsc(siteId).stream().map(ReportEmailConfigDto::from).toList();
    }

    @Transactional
    public ReportEmailConfigDto save(String reportCode, UpdateRecipientRequest r) {
        long siteId = TenantContext.requireSiteId();
        ReportEmailConfig c = repo.findBySiteIdAndReportCode(siteId, reportCode)
            .orElseGet(() -> ReportEmailConfig.builder().siteId(siteId).reportCode(reportCode).build());
        c.setLabel(blankToNull(r.label()));
        c.setRecipientEmail(blankToNull(r.recipientEmail()));
        c.setSubject(blankToNull(r.subject()));
        c.setBody(blankToNull(r.body()));
        c.setUpdatedAt(LocalDateTime.now());
        c.setUpdatedBy(currentUsername());
        return ReportEmailConfigDto.from(repo.save(c));
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SYSTEM" : a.getName();
    }
}
