package com.built4u.pos.loyalty.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedeemRewardRequest(
    @NotNull Long customerId,
    @NotNull Long rewardId,
    @Size(max = 300) String note
) {}
