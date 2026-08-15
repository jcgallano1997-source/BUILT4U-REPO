package com.built4u.pos.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * POS checkout payload. Server snapshots each item's selling price (client prices
 * ignored). {@code adjustment} is a per-line discount; {@code discountAll} is a
 * transaction-level discount. All modes require payment >= grand total in this
 * phase (accounts-receivable / CHARGE credit arrives in a later phase).
 */
public record CreateSaleRequest(
    Long customerId,
    @NotBlank @Size(max = 50) String modeOfPayment,
    @NotNull @PositiveOrZero BigDecimal payment,
    @PositiveOrZero BigDecimal discountAll,
    @Size(max = 40) String voucherCode,
    @Size(max = 200) String reference,
    /** Optional split/multiple tender. When present & non-empty it drives payment;
     *  the scalar {@code modeOfPayment}/{@code payment} above are the single-tender fallback. */
    @Valid List<Tender> payments,
    /** Manager approval for a price override / line discount when the cashier lacks
     *  the PRICE_OVERRIDE module. Ignored when the cashier can self-authorize. */
    @Size(max = 50) String approvalUser,
    @Size(max = 100) String approvalPassword,
    @NotEmpty @Valid List<Line> lines
) {
    public record Line(
        @NotNull Long itemId,
        @NotNull @Positive BigDecimal quantity,
        @PositiveOrZero BigDecimal adjustment,
        /** Optional per-line price override. When set (and different from the catalog
         *  price) the line sells at this price and requires override authorization. */
        @PositiveOrZero BigDecimal unitPrice,
        @Size(max = 255) String overrideReason
    ) {}

    public record Tender(
        @NotBlank @Size(max = 50) String mode,
        @NotNull @PositiveOrZero BigDecimal amount,
        @Size(max = 200) String reference
    ) {}
}
