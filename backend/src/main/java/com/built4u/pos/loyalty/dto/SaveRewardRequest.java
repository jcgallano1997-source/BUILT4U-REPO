package com.built4u.pos.loyalty.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SaveRewardRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 300) String description,
    @NotNull @DecimalMin(value = "0.01") BigDecimal pointsCost,
    @NotBlank @Pattern(regexp = "ITEM|FREETEXT") String rewardType,
    Long itemId,
    @PositiveOrZero int sortOrder,
    boolean active
) {}
