package com.built4u.pos.shift;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A cashier shift ({@code pos_shift}). Keyed by (siteId, shiftNumber); belongs to
 * a cashier (the JWT username, which JPA auditing also stamps into
 * {@code Sale.createdBy}). Sales/refunds attribute to the shift by that username
 * within the window [openedAt, closedAt]. Reconciliation figures are snapshotted
 * at close so a later void/refund can't rewrite a printed report.
 */
@Entity
@Table(name = "pos_shift")
@IdClass(ShiftId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id @Column(name = "shift_number", nullable = false, length = 100)
    private String shiftNumber;

    @Column(nullable = false, length = 50)
    private String cashier;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = ShiftStatus.OPEN.name();

    @Column(name = "opening_float", nullable = false)
    private BigDecimal openingFloat;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 50)
    private String closedBy;

    @Column(name = "counted_cash")
    private BigDecimal countedCash;

    @Column(name = "expected_cash")
    private BigDecimal expectedCash;

    @Column(name = "cash_variance")
    private BigDecimal cashVariance;

    @Column(name = "cash_sales_total", nullable = false)
    @Builder.Default
    private BigDecimal cashSalesTotal = BigDecimal.ZERO;

    @Column(name = "cash_refunds_total", nullable = false)
    @Builder.Default
    private BigDecimal cashRefundsTotal = BigDecimal.ZERO;

    @Column(name = "cash_in_total", nullable = false)
    @Builder.Default
    private BigDecimal cashInTotal = BigDecimal.ZERO;

    @Column(name = "cash_out_total", nullable = false)
    @Builder.Default
    private BigDecimal cashOutTotal = BigDecimal.ZERO;

    @Column(name = "noncash_gcash_total", nullable = false)
    @Builder.Default
    private BigDecimal noncashGcashTotal = BigDecimal.ZERO;

    @Column(name = "noncash_paymaya_total", nullable = false)
    @Builder.Default
    private BigDecimal noncashPaymayaTotal = BigDecimal.ZERO;

    @Column(name = "noncash_bank_total", nullable = false)
    @Builder.Default
    private BigDecimal noncashBankTotal = BigDecimal.ZERO;

    @Column(name = "noncash_cheque_total", nullable = false)
    @Builder.Default
    private BigDecimal noncashChequeTotal = BigDecimal.ZERO;

    @Column(name = "noncash_charge_total", nullable = false)
    @Builder.Default
    private BigDecimal noncashChargeTotal = BigDecimal.ZERO;

    @Column(name = "sale_count", nullable = false)
    @Builder.Default
    private int saleCount = 0;

    @Column(name = "close_note", length = 500)
    private String closeNote;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
