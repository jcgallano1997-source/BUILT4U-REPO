package com.built4u.pos.loyalty;

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
 * A reward customers can redeem loyalty points for ({@code pos_loyalty_reward}),
 * a per-site catalog. {@code rewardType} ITEM → {@code itemId} links an inventory
 * item and redeeming decrements its stock by 1; FREETEXT → a free-text freebie
 * ({@code itemId} is null).
 */
@Entity
@Table(name = "pos_loyalty_reward")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "points_cost", nullable = false)
    private BigDecimal pointsCost;

    @Column(name = "reward_type", nullable = false, length = 10)
    private String rewardType;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 100;

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
