package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Completed sales grouped by hour-of-day over a period (peak-hour analysis). */
public record SalesByHourDto(
    LocalDate from,
    LocalDate to,
    List<Row> rows
) {
    public record Row(
        String hour,        // e.g. "14:00-15:00"
        long saleCount,
        BigDecimal net,
        BigDecimal avgSale
    ) {}
}
