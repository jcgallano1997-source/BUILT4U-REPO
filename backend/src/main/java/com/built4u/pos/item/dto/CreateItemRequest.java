package com.built4u.pos.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateItemRequest(
    @NotBlank @Size(max = 50)  String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 300)           String description,
    @NotNull                   Long catId,
    @NotNull                   Long locId,
    @NotBlank @Size(max = 50)  String uom,
    @NotNull @PositiveOrZero   BigDecimal quantity,
    @NotNull @PositiveOrZero   BigDecimal sellingPrice,
    @PositiveOrZero            BigDecimal costPrice,
    @PositiveOrZero            BigDecimal warning,
    @PositiveOrZero            BigDecimal critical,
    @PositiveOrZero            Long barcodeId
) {}
