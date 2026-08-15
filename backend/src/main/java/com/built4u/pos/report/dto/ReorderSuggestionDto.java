package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reorder Suggestions Report — active items at/below their warning or critical
 * threshold, with a suggested reorder quantity (bring stock up to 2× the reorder
 * point) and its estimated cost at the current moving-average cost.
 */
public record ReorderSuggestionDto(
    int itemCount,
    BigDecimal totalSuggestedCost,
    List<Row> rows
) {
    public record Row(
        String code,
        String name,
        String category,
        String status,        // OUT_OF_STOCK | CRITICAL | LOW
        BigDecimal onHand,
        String uom,
        BigDecimal warning,
        BigDecimal critical,
        BigDecimal suggestedQty,
        BigDecimal unitCost,
        BigDecimal estimatedCost
    ) {}
}
