package com.built4u.pos.poapprover;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A user who may be chosen as a PO approver. Holders of the built-in OWNER role
 * are eligible without a row here (see {@link PoApproverService}), so the owner
 * can't be removed from the pool. Mapped to {@code pos_po_approver_pool}.
 */
@Entity
@Table(name = "pos_po_approver_pool")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoApproverPool {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @PrePersist
    void onCreate() {
        if (creationDate == null) creationDate = LocalDateTime.now();
    }
}
