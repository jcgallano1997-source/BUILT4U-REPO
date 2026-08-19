package com.built4u.pos.reportemail;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.report.export.ExportResponses;
import com.built4u.pos.report.export.ExportTable;
import com.built4u.pos.report.export.ReportPdfExporter;
import com.built4u.pos.report.export.ReportXlsxExporter;
import com.built4u.pos.reportemail.dto.RecipientUserDto;
import com.built4u.pos.reportemail.dto.ReportEmailConfigDto;
import com.built4u.pos.reportemail.dto.UpdateRecipientRequest;
import com.built4u.pos.user.User;
import com.built4u.pos.user.UserRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
    private final ReportEmailRecipientRepository recipientRepo;
    private final UserRepository userRepository;
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

        // Addresses are resolved now, not stored: a changed address or a
        // deactivated account takes effect on the very next send.
        Map<String, String> byLower = new LinkedHashMap<>();
        for (ReportEmailRecipient r : recipientRepo.findBySiteIdAndReportCode(siteId, reportCode)) {
            userRepository.findById(r.getUserId())
                .filter(User::isActive)
                .map(User::getEmail)
                .filter(ReportEmailService::notBlank)
                .ifPresent(e -> byLower.putIfAbsent(e.trim().toLowerCase(), e.trim()));
        }
        if (cfg != null && notBlank(cfg.getRecipientEmail())) {
            byLower.putIfAbsent(cfg.getRecipientEmail().trim().toLowerCase(), cfg.getRecipientEmail().trim());
        }
        if (byLower.isEmpty() && notBlank(defaultRecipient)) {
            byLower.put(defaultRecipient.trim().toLowerCase(), defaultRecipient.trim());
        }
        if (byLower.isEmpty()) {
            throw new BadRequestException(
                "No email recipient configured for this report. Set one in Admin → Report email.");
        }
        List<String> to = List.copyOf(byLower.values());

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
        Map<String, List<RecipientUserDto>> byReport = new HashMap<>();
        for (ReportEmailRecipient r : recipientRepo.findBySiteId(siteId)) {
            userRepository.findById(r.getUserId()).ifPresent(u -> byReport
                .computeIfAbsent(r.getReportCode(), k -> new ArrayList<>())
                .add(toRecipient(u)));
        }
        return repo.findBySiteIdOrderByReportCodeAsc(siteId).stream()
            .map(c -> ReportEmailConfigDto.from(c, byReport.getOrDefault(c.getReportCode(), List.of())))
            .toList();
    }

    /** Every active user, so the admin can pick recipients by name (email may be null). */
    @Transactional(readOnly = true)
    public List<RecipientUserDto> recipientCandidates() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
            .filter(User::isActive)
            .map(ReportEmailService::toRecipient)
            .toList();
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
        repo.save(c);

        // Replace the recipient set wholesale — simpler than diffing, and the
        // list is a handful of rows.
        recipientRepo.deleteBySiteIdAndReportCode(siteId, reportCode);
        List<RecipientUserDto> saved = new ArrayList<>();
        for (Long userId : new LinkedHashSet<>(r.userIds() == null ? List.<Long>of() : r.userIds())) {
            User u = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User " + userId + " not found"));
            if (!notBlank(u.getEmail())) {
                throw new BadRequestException(
                    u.getFullName() + " has no email address — set one on their user account first.");
            }
            recipientRepo.save(ReportEmailRecipient.builder()
                .siteId(siteId).reportCode(reportCode).userId(userId)
                .createdBy(currentUsername()).build());
            saved.add(toRecipient(u));
        }
        return ReportEmailConfigDto.from(c, saved);
    }

    private static RecipientUserDto toRecipient(User u) {
        return new RecipientUserDto(u.getId(), u.getUsername(), u.getFullName(),
            notBlank(u.getEmail()) ? u.getEmail().trim() : null);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SYSTEM" : a.getName();
    }
}
