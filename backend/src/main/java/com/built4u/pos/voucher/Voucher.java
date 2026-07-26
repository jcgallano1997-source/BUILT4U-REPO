package com.built4u.pos.voucher;

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
 * A discount voucher code ({@code pos_voucher}), per site.
 *
 * <p>{@code discountType=FIXED} → {@code discountValue} is a peso amount;
 * {@code PERCENT} → it's a percent and {@code maxDiscount} caps the ₱ off.
 * {@code usageLimit} null = unlimited; {@code usedCount} is bumped atomically at
 * redeem time. {@code validFrom}/{@code validTo} are inclusive day bounds.
 */
@Entity
@Table(name = "pos_voucher")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(length = 150)
    private String description;

    /** FIXED | PERCENT */
    @Column(name = "discount_type", nullable = false, length = 10)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    /** Cap on the ₱ discount for PERCENT vouchers (null = no cap). */
    @Column(name = "max_discount")
    private BigDecimal maxDiscount;

    /** Minimum sale subtotal to qualify (null = none). */
    @Column(name = "min_spend")
    private BigDecimal minSpend;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    /** Total redemptions allowed (null = unlimited). */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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
