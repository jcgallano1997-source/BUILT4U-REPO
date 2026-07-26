package com.built4u.pos.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SavePaymentModeRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 60) String label,
    @NotBlank @Pattern(regexp = "NONE|PERCENT|FIXED") String surchargeType,
    @NotNull @PositiveOrZero BigDecimal surchargeValue,
    boolean isCash,
    boolean allowsPartial,
    boolean customerRequired,
    boolean accountsReceivable,
    @PositiveOrZero int arDueDays,
    @PositiveOrZero int sortOrder,
    boolean active
) {}
