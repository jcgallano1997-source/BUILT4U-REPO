package com.built4u.pos.reportemail.dto;

import com.built4u.pos.reportemail.ReportEmailConfig;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Per-report email settings. {@code recipients} are the chosen users (the normal
 * case); {@code recipientEmail} is an optional extra address for someone with no
 * user account. A report goes to both.
 */
public record ReportEmailConfigDto(
    String reportCode,
    String label,
    String recipientEmail,
    String subject,
    String body,
    List<RecipientUserDto> recipients,
    String updatedBy,
    LocalDateTime updatedAt
) {
    public static ReportEmailConfigDto from(ReportEmailConfig c, List<RecipientUserDto> recipients) {
        return new ReportEmailConfigDto(c.getReportCode(), c.getLabel(), c.getRecipientEmail(),
            c.getSubject(), c.getBody(), recipients, c.getUpdatedBy(), c.getUpdatedAt());
    }
}
