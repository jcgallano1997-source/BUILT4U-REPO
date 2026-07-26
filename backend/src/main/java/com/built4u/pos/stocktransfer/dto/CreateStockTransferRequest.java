package com.built4u.pos.stocktransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Ship items from the current site to {@code destSiteId}. */
public record CreateStockTransferRequest(
    @NotNull Long destSiteId,
    @Size(max = 300) String remarks,
    @NotEmpty @Valid List<Line> lines
) {
    public record Line(
        @NotNull Long itemId,
        @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal quantity
    ) {}
}
