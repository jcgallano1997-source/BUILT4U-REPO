package com.built4u.pos.loyalty.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Earn % (0–100) and ₱/redeemed-point (≥ 0). */
public record UpdateLoyaltyConfigRequest(
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal pointsRate,
    @NotNull @DecimalMin("0") BigDecimal redeemValue
) {}
