package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Completed sales grouped by cashier over a period (excludes VOIDED). */
public record SalesByCashierDto(
    LocalDate from,
    LocalDate to,
    List<Row> rows
) {
    public record Row(
        String cashier,
        long saleCount,
        BigDecimal gross,
        BigDecimal discounts,
        BigDecimal net,
        BigDecimal avgSale
    ) {}
}
