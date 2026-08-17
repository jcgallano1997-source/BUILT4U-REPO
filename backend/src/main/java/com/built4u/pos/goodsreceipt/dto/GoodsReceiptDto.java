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
    List<Line> lines,
    /** Items whose moving-average cost rose on this receipt, with a markup-preserving
     *  suggested selling price. Only populated on create; empty on reads/lists. */
    List<RepriceSuggestion> repriceSuggestions
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

    public record RepriceSuggestion(
        Long itemId,
        String code,
        String name,
        BigDecimal oldCost,
        BigDecimal newCost,
        BigDecimal sellingPrice,
        BigDecimal suggestedPrice
    ) {}

    /** Copy with reprice suggestions attached (used by create()). */
    public GoodsReceiptDto withReprice(List<RepriceSuggestion> suggestions) {
        return new GoodsReceiptDto(grNumber, poNumber, supplier, reference, remarks,
            grandTotal, creationDate, createdBy, lines, suggestions);
    }
}
