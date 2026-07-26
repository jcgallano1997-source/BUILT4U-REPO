package com.built4u.pos.shift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Compact row for the shift-history list. For OPEN shifts expectedCash is a live preview. */
public record ShiftSummaryDto(
    String shiftNumber,
    String cashier,
    String status,
    BigDecimal openingFloat,
    BigDecimal expectedCash,
    BigDecimal countedCash,
    BigDecimal cashVariance,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    int saleCount
) {}
