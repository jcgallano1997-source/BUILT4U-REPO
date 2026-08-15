package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Discounts &amp; Overrides Report — one row per sale line that was sold below its
 * catalog price (price override) or carried a line discount. Values are read from
 * the {@code pos_sale_item} snapshot ({@code list_price} vs {@code unit_cost}), so
 * the audit stays correct even if the item's catalog price later changes.
 */
public record DiscountOverrideDto(
    java.time.LocalDate from,
    java.time.LocalDate to,
    int lineCount,
    BigDecimal totalPriceOverride,
    BigDecimal totalLineDiscount,
    BigDecimal totalGiveback,
    List<Row> rows
) {
    public record Row(
        LocalDateTime when,
        String salesNumber,
        String cashier,
        String customer,
        String item,
        BigDecimal quantity,
        BigDecimal listPrice,      // catalog price snapshot
        BigDecimal chargedPrice,   // price actually charged (unit_cost)
        BigDecimal priceOverride,  // (listPrice − chargedPrice) × qty, ≥ 0 when discounted down
        BigDecimal lineDiscount,   // per-line adjustment
        BigDecimal giveback,       // priceOverride + lineDiscount
        String reason,
        String approvedBy,
        String saleStatus
    ) {}
}
