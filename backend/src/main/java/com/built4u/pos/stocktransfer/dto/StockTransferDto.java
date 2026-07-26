package com.built4u.pos.stocktransfer.dto;

import com.built4u.pos.stocktransfer.StockTransfer;

import java.time.LocalDateTime;

public record StockTransferDto(
    Long id,
    String transferNumber,
    Long sourceSiteId,
    String sourceSiteName,
    Long destSiteId,
    String destSiteName,
    String status,
    String remarks,
    LocalDateTime shippedAt,
    String sentBy,
    LocalDateTime receivedAt,
    String receivedBy,
    LocalDateTime cancelledAt,
    String cancelledBy,
    int lineCount
) {
    public static StockTransferDto from(StockTransfer s, String sourceSiteName,
                                         String destSiteName, int lineCount) {
        return new StockTransferDto(
            s.getId(), s.getTransferNumber(),
            s.getSourceSiteId(), sourceSiteName,
            s.getDestSiteId(), destSiteName,
            s.getStatus(), s.getRemarks(),
            s.getShippedAt(), s.getSentBy(),
            s.getReceivedAt(), s.getReceivedBy(),
            s.getCancelledAt(), s.getCancelledBy(),
            lineCount);
    }
}
