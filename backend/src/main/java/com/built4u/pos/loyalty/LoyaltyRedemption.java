package com.built4u.pos.loyalty;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One redeemed reward ({@code pos_loyalty_redemption}) — the customer↔reward
 * link. Reward fields are snapshotted so a later catalog edit/delete never
 * rewrites history. The points debit is a REDEEM row in {@code pos_loyalty_ledger}.
 */
@Entity
@Table(name = "pos_loyalty_redemption")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "reward_id", nullable = false)
    private Long rewardId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "reward_name", nullable = false, length = 100)
    private String rewardName;

    @Column(name = "reward_type", nullable = false, length = 10)
    private String rewardType;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "points_spent", nullable = false)
    private BigDecimal pointsSpent;

    @Column(length = 300)
    private String note;

    @Column(name = "redeemed_at", nullable = false)
    @Builder.Default
    private LocalDateTime redeemedAt = LocalDateTime.now();

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
