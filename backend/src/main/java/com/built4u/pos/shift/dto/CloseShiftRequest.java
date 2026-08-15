package com.built4u.pos.shift.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cashier closes a shift, declaring the cash physically counted. When
 * {@code denominations} are supplied, counted cash is derived from the tally
 * (Σ denom × qty) rather than trusting the typed {@code countedCash}.
 */
public record CloseShiftRequest(
    @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal countedCash,
    @Size(max = 500) String closeNote,
    @Valid List<DenomCount> denominations
) {
    public record DenomCount(
        @NotNull @Positive BigDecimal denom,
        @NotNull @PositiveOrZero Integer qty
    ) {}
}
