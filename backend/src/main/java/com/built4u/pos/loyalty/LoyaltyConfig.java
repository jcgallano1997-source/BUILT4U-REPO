package com.built4u.pos.loyalty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-site loyalty config ({@code pos_loyalty_config}), keyed by site_id.
 * {@code pointsRate} is a PERCENT (e.g. 5.00 = 5% of the grand total, floored to
 * whole points on earn). {@code redeemValue} is the informational ₱ value of a
 * point. No per-site row ⇒ the service falls through to a 5% default.
 */
@Entity
@Table(name = "pos_loyalty_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyConfig {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "points_rate", nullable = false)
    @Builder.Default
    private BigDecimal pointsRate = new BigDecimal("5");

    @Column(name = "redeem_value", nullable = false)
    @Builder.Default
    private BigDecimal redeemValue = BigDecimal.ONE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
