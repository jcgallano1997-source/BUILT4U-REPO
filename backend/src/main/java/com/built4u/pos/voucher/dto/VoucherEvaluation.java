package com.built4u.pos.voucher.dto;

import java.math.BigDecimal;

/**
 * Outcome of evaluating a voucher against a cart (no consumption). When
 * {@code valid} is false, {@code reason}/{@code message} explain why and
 * {@code discountAmount} is zero. {@code reason} ∈ {NOT_FOUND, INACTIVE,
 * NOT_STARTED, EXPIRED, BELOW_MIN_SPEND, USAGE_EXHAUSTED}.
 */
public record VoucherEvaluation(
    boolean valid,
    String code,
    String reason,
    String message,
    String description,
    String discountType,
    BigDecimal discountAmount
) {
    public static VoucherEvaluation invalid(String code, String reason, String message) {
        return new VoucherEvaluation(false, code, reason, message, null, null, BigDecimal.ZERO);
    }

    public static VoucherEvaluation ok(String code, String description,
                                       String discountType, BigDecimal amount) {
        return new VoucherEvaluation(true, code, null, null, description, discountType, amount);
    }
}
