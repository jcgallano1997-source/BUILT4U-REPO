package com.built4u.pos.sale.dto;

import java.math.BigDecimal;

public record SaleLineDto(
    Long itemId,
    String itemName,
    String uom,
    BigDecimal quantity,
    BigDecimal adjustment,
    BigDecimal unitCost,
    BigDecimal listPrice,
    BigDecimal unitCogs,
    String overrideReason,
    String approvedBy,
    BigDecimal subTotal,
    BigDecimal refundedQuantity,
    BigDecimal refundableQuantity
) {}
