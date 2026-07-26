package com.built4u.pos.voucher;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per time a voucher is applied to a sale ({@code pos_voucher_redemption}).
 * Doubles as the sale↔voucher link. {@code reversed} is set when a full VOID
 * releases the redemption (the voucher's used_count is decremented too).
 */
@Entity
@Table(name = "pos_voucher_redemption")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "sales_number", nullable = false, length = 100)
    private String salesNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "redeemed_at", nullable = false)
    @Builder.Default
    private LocalDateTime redeemedAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean reversed = false;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
