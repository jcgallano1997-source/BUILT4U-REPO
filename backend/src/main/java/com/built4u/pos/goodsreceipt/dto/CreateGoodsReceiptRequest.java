package com.built4u.pos.goodsreceipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateGoodsReceiptRequest(
    @Size(max = 100) String poNumber,
    @Size(max = 100) String supplier,
    @Size(max = 200) String reference,
    @Size(max = 200) String remarks,
    @NotEmpty @Valid List<Line> lines
) {
    public record Line(
        @NotNull            Long itemId,
        @NotNull @Positive  BigDecimal quantity,
        @PositiveOrZero     BigDecimal unitCost
    ) {}
}
