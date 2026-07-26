package com.built4u.pos.purchaseorder.dto;

import java.math.BigDecimal;

public record PurchaseOrderLineDto(
    Long itemId,
    String itemCode,
    String itemName,
    String itemDesc,
    String uom,
    BigDecimal orderedQty,
    BigDecimal receivedQty,
    BigDecimal remainingQty,
    BigDecimal unitPrice,
    BigDecimal subTotal
) {}
