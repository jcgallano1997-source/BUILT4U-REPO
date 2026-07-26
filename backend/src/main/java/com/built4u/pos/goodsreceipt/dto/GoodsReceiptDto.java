package com.built4u.pos.goodsreceipt.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceiptDto(
    String grNumber,
    String poNumber,
    String supplier,
    String reference,
    String remarks,
    BigDecimal grandTotal,
    LocalDateTime creationDate,
    String createdBy,
    List<Line> lines
) {
    public record Line(
        Long itemId,
        String itemCode,
        String itemName,
        String uom,
        BigDecimal quantity,
        BigDecimal supPrice,
        BigDecimal subTotal
    ) {}
}
