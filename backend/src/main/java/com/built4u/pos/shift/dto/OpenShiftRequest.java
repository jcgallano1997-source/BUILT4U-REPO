package com.built4u.pos.shift.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Cashier opens a shift with the starting cash counted into the drawer. */
public record OpenShiftRequest(
    @NotNull @PositiveOrZero @Digits(integer = 36, fraction = 2) BigDecimal openingFloat
) {}
