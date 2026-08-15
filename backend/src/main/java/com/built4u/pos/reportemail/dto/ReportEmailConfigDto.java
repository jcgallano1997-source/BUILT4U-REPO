package com.built4u.pos.reportemail.dto;

import com.built4u.pos.reportemail.ReportEmailConfig;

import java.time.LocalDateTime;

public record ReportEmailConfigDto(
    String reportCode,
    String label,
    String recipientEmail,
    String subject,
    String body,
    String updatedBy,
    LocalDateTime updatedAt
) {
    public static ReportEmailConfigDto from(ReportEmailConfig c) {
        return new ReportEmailConfigDto(c.getReportCode(), c.getLabel(), c.getRecipientEmail(),
            c.getSubject(), c.getBody(), c.getUpdatedBy(), c.getUpdatedAt());
    }
}
