package com.built4u.pos.purchaseorder;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Approval audit sidecar. Holds the (approved_at, approved_by, auto_approved)
 * metadata that doesn't fit on the denormalized {@code pos_purchase_order}
 * line table. One row exists only once the PO has reached APPROVED.
 */
@Entity
@Table(name = "pos_po_approval")
@IdClass(PoApproval.PoApprovalId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoApproval {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "po_number", nullable = false, length = 100)
    private String poNumber;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", nullable = false, length = 50)
    private String approvedBy;

    /** Y / N — 'Y' when stamped by auto-approval at create time. */
    @Column(name = "auto_approved", nullable = false, length = 1)
    @Builder.Default
    private String autoApproved = "N";

    public boolean isAutoApproved() { return "Y".equalsIgnoreCase(autoApproved); }

    @EqualsAndHashCode
    public static class PoApprovalId implements Serializable {
        private Long siteId;
        private String poNumber;
        public PoApprovalId() {}
        public PoApprovalId(Long siteId, String poNumber) {
            this.siteId = siteId;
            this.poNumber = poNumber;
        }
    }
}
