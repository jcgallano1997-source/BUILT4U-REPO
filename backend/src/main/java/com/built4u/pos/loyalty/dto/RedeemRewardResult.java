package com.built4u.pos.loyalty.dto;

import java.math.BigDecimal;

public record RedeemRewardResult(
    String rewardName,
    String rewardType,
    String itemName,
    BigDecimal pointsSpent,
    BigDecimal newBalance
) {}
