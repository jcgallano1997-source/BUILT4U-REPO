package com.built4u.pos.sale.dto;

import java.math.BigDecimal;

public record SaleLineDto(
    Long itemId,
    String itemName,
    String uom,
    BigDecimal quantity,
    BigDecimal adjustment,
    BigDecimal unitCost,
    BigDecimal subTotal,
    BigDecimal refundedQuantity,
    BigDecimal refundableQuantity
) {}
