package com.built4u.pos.shift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full shift view. While OPEN, the totals/expectedCash are a LIVE recomputed
 * preview and countedCash/cashVariance/closedAt/closedBy are null. Once CLOSED
 * every figure is the verbatim snapshot written at close.
 */
public record ShiftDto(
    String shiftNumber,
    String cashier,
    String status,
    BigDecimal openingFloat,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    String closedBy,
    BigDecimal cashSalesTotal,
    BigDecimal cashRefundsTotal,
    BigDecimal cashIn,
    BigDecimal cashOut,
    BigDecimal expectedCash,
    BigDecimal countedCash,
    BigDecimal cashVariance,
    BigDecimal gcashTotal,
    BigDecimal paymayaTotal,
    BigDecimal bankTransferTotal,
    BigDecimal chequeTotal,
    BigDecimal chargeTotal,
    int saleCount,
    String closeNote,
    LocalDateTime creationDate,
    String createdBy
) {}
