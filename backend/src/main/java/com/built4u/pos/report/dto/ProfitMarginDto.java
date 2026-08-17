package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Profit &amp; margin per item over a period, from the per-sale-line moving-average
 * cost snapshot (unit_cogs). Excludes VOIDED sales. Margin% = margin / revenue.
 */
public record ProfitMarginDto(
    LocalDate from,
    LocalDate to,
    BigDecimal totalRevenue,
    BigDecimal totalCogs,
    BigDecimal totalMargin,
    BigDecimal marginPct,
    List<Row> rows
) {
    public record Row(
        String item,
        String category,
        BigDecimal qtySold,
        BigDecimal revenue,
        BigDecimal cogs,
        BigDecimal margin,
        BigDecimal marginPct
    ) {}
}
