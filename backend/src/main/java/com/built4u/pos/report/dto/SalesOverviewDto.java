package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Sales summary over a date range: headline totals + per-mode + per-day breakdowns. */
public record SalesOverviewDto(
    LocalDate from,
    LocalDate to,
    long salesCount,
    BigDecimal gross,
    BigDecimal lineDiscounts,
    BigDecimal orderDiscounts,
    BigDecimal netSales,
    List<ModeTotal> byMode,
    List<DayTotal> byDay
) {
    public record ModeTotal(String mode, long count, BigDecimal total) {}
    public record DayTotal(LocalDate date, long count, BigDecimal net) {}
}
