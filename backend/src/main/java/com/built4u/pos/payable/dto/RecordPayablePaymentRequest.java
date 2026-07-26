package com.built4u.pos.payable.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Record a disbursement against a payable. */
public record RecordPayablePaymentRequest(
    @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal amount,
    @Size(max = 300) String note
) {}
