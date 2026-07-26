package com.built4u.pos.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustStockRequest(
    @NotNull                   BigDecimal delta,
    @NotBlank @Size(max = 300) String reason
) {}
