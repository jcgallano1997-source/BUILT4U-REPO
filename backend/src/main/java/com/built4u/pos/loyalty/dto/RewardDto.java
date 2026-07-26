package com.built4u.pos.loyalty.dto;

import com.built4u.pos.loyalty.LoyaltyReward;

import java.math.BigDecimal;

public record RewardDto(
    Long id,
    String name,
    String description,
    BigDecimal pointsCost,
    String rewardType,
    Long itemId,
    String itemName,
    int sortOrder,
    boolean active
) {
    public static RewardDto from(LoyaltyReward r, String itemName) {
        return new RewardDto(
            r.getId(), r.getName(), r.getDescription(), r.getPointsCost(),
            r.getRewardType(), r.getItemId(), itemName, r.getSortOrder(), r.isActive());
    }
}
