package com.built4u.pos.stocktransfer.dto;

import java.util.List;

/** A stock transfer header + its line items. */
public record StockTransferDetailDto(
    StockTransferDto header,
    List<StockTransferItemDto> items
) {}
