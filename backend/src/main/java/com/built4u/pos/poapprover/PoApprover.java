package com.built4u.pos.poapprover;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Per-user PO approver mapping. PK is the creator's {@code user_id}; the value
 * is the {@code approver_user_id}. Absence of a row = "auto-approve my POs on
 * create". Business-wide (not site-scoped): a user's approver is the same
 * whatever site they're logged into. Mapped to {@code pos_po_approver}.
 */
@Entity
@Table(name = "pos_po_approver")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoApprover {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "approver_user_id", nullable = false)
    private Long approverUserId;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @LastModifiedDate
    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy
    @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;

    @PrePersist
    void onCreate() {
        if (creationDate == null) creationDate = LocalDateTime.now();
    }
}
