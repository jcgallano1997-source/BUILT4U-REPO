package com.built4u.pos.receivable.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Record a collection against a receivable. */
public record RecordPaymentRequest(
    @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal amount,
    @Size(max = 300) String note
) {}
