package com.built4u.pos.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * {@code subtotal} = the cart total AFTER per-line discounts but BEFORE the
 * voucher (the locked discount order).
 */
public record ValidateVoucherRequest(
    @NotBlank String code,
    @NotNull @PositiveOrZero BigDecimal subtotal,
    Long customerId
) {}
