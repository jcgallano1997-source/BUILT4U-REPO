package com.built4u.pos.sale.dto;

import java.math.BigDecimal;

/** One tender on a sale — for the receipt and the sale detail view. */
public record PaymentLineDto(
    String mode,
    BigDecimal amount,     // applied
    BigDecimal tendered,
    BigDecimal change,
    String reference
) {}
