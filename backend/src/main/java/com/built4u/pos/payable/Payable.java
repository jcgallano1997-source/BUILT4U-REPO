package com.built4u.pos.payable;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One amount the site owes (table {@code pos_payable}). Two sources: PURCHASE
 * (auto-created at Goods Receipt for AP-enabled suppliers — po/gr/supplier_id
 * populated) and EXPENSE (manually entered — payee_name + category populated,
 * no PO/GR/supplier). Mirror of {@link com.built4u.pos.receivable.Receivable}.
 */
@Entity
@Table(name = "pos_payable")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String source = PayableSource.PURCHASE.name();

    @Column(length = 60)
    private String category;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(name = "gr_number", length = 100)
    private String grNumber;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "payee_name", nullable = false, length = 150)
    private String payeeName;

    @Column(length = 300)
    private String description;

    @Column(name = "original_amount", nullable = false)
    private BigDecimal originalAmount;

    @Column(name = "amount_paid", nullable = false)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = PayableStatus.OPEN.name();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy
    @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;
}
