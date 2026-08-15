package com.built4u.pos.reportemail;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Per-site, per-report email delivery config ({@code pos_report_email_config}).
 * Holds the saved recipient plus optional subject/body overrides used when a
 * report is emailed. No row ⇒ generated defaults + the global default recipient.
 */
@Entity
@Table(name = "pos_report_email_config")
@IdClass(ReportEmailConfigId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportEmailConfig {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "report_code", nullable = false, length = 60)
    private String reportCode;

    @Column(length = 100)
    private String label;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(length = 255)
    private String subject;

    @Column(length = 2000)
    private String body;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
