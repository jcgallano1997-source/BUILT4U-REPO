package com.built4u.pos.reportemail;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One user who receives a given report by email ({@code pos_report_email_recipient}).
 * The address itself is not stored — it is read from the user at send time, so
 * changing an address or deactivating an account takes effect immediately.
 */
@Entity
@Table(name = "pos_report_email_recipient")
@IdClass(ReportEmailRecipientId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportEmailRecipient {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "report_code", nullable = false, length = 60)
    private String reportCode;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @PrePersist
    void onCreate() {
        if (creationDate == null) creationDate = LocalDateTime.now();
    }
}
