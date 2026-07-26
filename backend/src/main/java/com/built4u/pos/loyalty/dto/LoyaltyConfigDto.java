package com.built4u.pos.loyalty.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code pointsRate} is a percent. {@code usingDefault} = no site row yet. */
public record LoyaltyConfigDto(
    Long siteId,
    boolean usingDefault,
    BigDecimal pointsRate,
    BigDecimal redeemValue,
    String updatedBy,
    LocalDateTime updatedAt
) {}
