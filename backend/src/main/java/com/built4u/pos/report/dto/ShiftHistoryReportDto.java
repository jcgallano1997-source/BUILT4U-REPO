package com.built4u.pos.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One row of the Shift History Report — a shift's full cash-reconciliation
 * snapshot plus the sales rung during its {@code [openedAt, closedAt]} window.
 * {@code sales} is empty when the shift had no transactions. Open shifts use
 * {@code now} as the window end for the sale lookup.
 */
public record ShiftHistoryReportDto(
    String shiftNumber,
    String cashier,
    String status,
    BigDecimal openingFloat,
    BigDecimal countedCash,
    BigDecimal expectedCash,
    BigDecimal cashVariance,
    BigDecimal cashSalesTotal,
    BigDecimal cashRefundsTotal,
    BigDecimal cashInTotal,
    BigDecimal cashOutTotal,
    BigDecimal noncashGcashTotal,
    BigDecimal noncashPaymayaTotal,
    BigDecimal noncashBankTotal,
    BigDecimal noncashChequeTotal,
    BigDecimal noncashChargeTotal,
    int saleCount,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    String closedBy,
    String closeNote,
    List<SaleRow> sales
) {
    /** One sale rung during a shift's window. */
    public record SaleRow(
        String salesNumber,
        LocalDateTime when,
        String modeOfPayment,
        String customerName,
        BigDecimal grandTotal,
        String status
    ) {}
}
