package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Slow / non-moving stock: active items with quantity on hand that haven't sold
 * in at least {@code minIdleDays} (or never). {@code totalIdleValue} is the cash
 * tied up at the current moving-average cost.
 */
public record DeadStockDto(
    int minIdleDays,
    int itemCount,
    BigDecimal totalIdleValue,
    List<Row> rows
) {
    public record Row(
        String code,
        String name,
        String category,
        BigDecimal onHand,
        String uom,
        BigDecimal unitCost,
        BigDecimal stockValue,
        String lastSold,     // ISO date, or null when never sold
        Integer daysIdle,    // null = never sold
        String bucket        // NEVER | 30-59 | 60-89 | 90+
    ) {}
}
