package com.built4u.pos.shift.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cashier closes a shift, declaring the cash physically counted in the drawer. */
public record CloseShiftRequest(
    @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal countedCash,
    @Size(max = 500) String closeNote
) {}
