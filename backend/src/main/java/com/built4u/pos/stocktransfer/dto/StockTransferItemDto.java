package com.built4u.pos.stocktransfer.dto;

import com.built4u.pos.stocktransfer.StockTransferItem;

import java.math.BigDecimal;

public record StockTransferItemDto(
    Long id,
    Long sourceItemId,
    String itemCode,
    String itemName,
    String uom,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal lineTotal
) {
    public static StockTransferItemDto from(StockTransferItem i) {
        BigDecimal qty  = i.getQuantity()  == null ? BigDecimal.ZERO : i.getQuantity();
        BigDecimal cost = i.getUnitCost()  == null ? BigDecimal.ZERO : i.getUnitCost();
        return new StockTransferItemDto(
            i.getId(), i.getSourceItemId(), i.getItemCode(), i.getItemName(),
            i.getUom(), qty, cost, qty.multiply(cost));
    }
}
