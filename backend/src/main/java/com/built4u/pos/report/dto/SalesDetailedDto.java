package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Line-level detail of completed sales in a period (revenue audit). */
public record SalesDetailedDto(
    LocalDate from,
    LocalDate to,
    int saleCount,
    int lineCount,
    BigDecimal totalQty,
    BigDecimal totalAmount,
    BigDecimal totalCogs,
    BigDecimal totalMargin,
    List<Line> lines
) {
    public record Line(
        LocalDateTime date,
        String salesNumber,
        String customer,
        String mode,
        String item,
        String category,
        BigDecimal qty,
        String uom,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal lineTotal,
        BigDecimal unitCogs,   // moving-average cost per unit, snapshot at sale
        BigDecimal lineCogs,   // unitCogs × qty
        BigDecimal margin      // lineTotal − lineCogs
    ) {}
}
