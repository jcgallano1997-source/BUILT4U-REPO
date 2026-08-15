package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Every stock movement (in/out/adjust/transfer) in a period, per item. */
public record InventoryMovementDto(
    LocalDate from,
    LocalDate to,
    int count,
    List<Row> rows
) {
    public record Row(
        LocalDateTime date,
        String item,
        String type,
        String reference,
        BigDecimal qtyChange,
        BigDecimal balanceAfter,
        String by
    ) {}
}
