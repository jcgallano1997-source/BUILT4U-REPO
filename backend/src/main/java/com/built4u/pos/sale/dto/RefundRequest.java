package com.built4u.pos.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Refund payload. Each line refers to a line on the original sale; quantity is
 * what's being refunded now (must be <= remaining refundable qty for that line).
 */
public record RefundRequest(
    @Size(max = 300) String reason,
    @NotEmpty @Valid List<Line> lines
) {
    public record Line(
        @NotNull Long itemId,
        @NotNull @Positive java.math.BigDecimal quantity
    ) {}
}
