package com.built4u.pos.payment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A configurable payment method ({@code pos_payment_mode}), per site.
 *
 * <p>{@code surchargeType} NONE → no fee; PERCENT → {@code surchargeValue} is a %,
 * FIXED → it's ₱. {@code cash} = reconciled against the drawer in Shift.
 * {@code accountsReceivable}/{@code customerRequired}/{@code arDueDays} feed the
 * Accounts-Receivable module (later phase).
 */
@Entity
@Table(name = "pos_payment_mode")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(name = "surcharge_type", nullable = false, length = 10)
    @Builder.Default
    private String surchargeType = "NONE";

    @Column(name = "surcharge_value", nullable = false)
    @Builder.Default
    private BigDecimal surchargeValue = BigDecimal.ZERO;

    @Column(name = "is_cash", nullable = false)
    @Builder.Default
    private boolean cash = false;

    @Column(name = "allows_partial", nullable = false)
    @Builder.Default
    private boolean allowsPartial = false;

    @Column(name = "customer_required", nullable = false)
    @Builder.Default
    private boolean customerRequired = false;

    @Column(name = "accounts_receivable", nullable = false)
    @Builder.Default
    private boolean accountsReceivable = false;

    @Column(name = "ar_due_days", nullable = false)
    @Builder.Default
    private int arDueDays = 30;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedDate @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;
}
