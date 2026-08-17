package com.built4u.pos.item.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Lightweight selling-price change (reprice-on-receive prompt). */
public record UpdateSellingPriceRequest(
    @NotNull @PositiveOrZero BigDecimal sellingPrice
) {}
