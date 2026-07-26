package com.built4u.pos.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReturnDto(
    String returnNumber,
    String salesNumber,
    BigDecimal totalRefunded,
    String reason,
    LocalDateTime creationDate,
    String createdBy,
    List<Line> lines
) {
    public record Line(
        Long itemId,
        String itemName,
        String uom,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal subTotal
    ) {}
}
