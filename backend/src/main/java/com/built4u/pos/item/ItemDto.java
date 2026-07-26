package com.built4u.pos.item;

import java.math.BigDecimal;

public record ItemDto(
    Long id,
    String code,
    String name,
    String description,
    Long catId,
    String categoryName,
    Long locId,
    String locationName,
    String uom,
    BigDecimal quantity,
    BigDecimal sellingPrice,
    BigDecimal costPrice,
    BigDecimal warning,
    BigDecimal critical,
    Long barcodeId,
    boolean active,
    StockLevel stockLevel
) {
    public enum StockLevel { OK, WARNING, CRITICAL }

    public static ItemDto from(Item i) {
        StockLevel level = computeStockLevel(i.getQuantity(), i.getWarning(), i.getCritical());
        return new ItemDto(
            i.getItemId(),
            i.getItemCode(),
            i.getItemName(),
            i.getItemDesc(),
            i.getCatId(),
            i.getCategory() == null ? null : i.getCategory().getCategoryName(),
            i.getLocId(),
            i.getLocation() == null ? null : i.getLocation().getLocation(),
            i.getUom(),
            i.getQuantity(),
            i.getSellingPrice(),
            i.getCostPrice(),
            i.getWarning(),
            i.getCritical(),
            i.getBarcodeId(),
            Boolean.TRUE.equals(i.getActive()),
            level
        );
    }

    private static StockLevel computeStockLevel(BigDecimal qty, BigDecimal warn, BigDecimal crit) {
        if (qty == null) return StockLevel.OK;
        if (crit != null && qty.compareTo(crit) <= 0) return StockLevel.CRITICAL;
        if (warn != null && qty.compareTo(warn) <= 0) return StockLevel.WARNING;
        return StockLevel.OK;
    }
}
