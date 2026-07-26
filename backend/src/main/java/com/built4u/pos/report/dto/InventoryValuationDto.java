package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.util.List;

/** Current stock value grouped by category, plus grand totals. */
public record InventoryValuationDto(
    BigDecimal grandQty,
    BigDecimal grandValue,
    List<CategoryValuation> categories
) {
    public record CategoryValuation(String category, long itemCount, BigDecimal totalQty, BigDecimal totalValue) {}
}
