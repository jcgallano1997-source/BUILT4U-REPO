package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per-customer purchase summary over a period (completed sales with a customer
 * attached; walk-ins excluded). Sorted by total spent, highest first.
 */
public record CustomerPurchaseDto(
    LocalDate from,
    LocalDate to,
    List<Row> rows
) {
    public record Row(
        String customer,
        long saleCount,
        BigDecimal totalSpent,
        BigDecimal avgSale,
        String lastPurchase   // ISO date
    ) {}
}
