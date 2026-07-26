package com.built4u.pos.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * POS checkout payload. Server snapshots each item's selling price (client prices
 * ignored). {@code adjustment} is a per-line discount; {@code discountAll} is a
 * transaction-level discount. All modes require payment >= grand total in this
 * phase (accounts-receivable / CHARGE credit arrives in a later phase).
 */
public record CreateSaleRequest(
    @NotBlank @Size(max = 50) String modeOfPayment,
    @NotNull @PositiveOrZero BigDecimal payment,
    @PositiveOrZero BigDecimal discountAll,
    @Size(max = 200) String reference,
    @NotEmpty @Valid List<Line> lines
) {
    public record Line(
        @NotNull Long itemId,
        @NotNull @Positive BigDecimal quantity,
        @PositiveOrZero BigDecimal adjustment
    ) {}
}
