package com.built4u.pos.stocktransferpolicy.dto;

import com.built4u.pos.stocktransferpolicy.StockTransferPolicy;

import java.time.LocalDateTime;

/** Row for the Stock Transfer Policy admin page (decorated with site names). */
public record PolicyDto(
    Long id,
    Long sourceSiteId,
    String sourceSiteCode,
    String sourceSiteName,
    Long destSiteId,
    String destSiteCode,
    String destSiteName,
    LocalDateTime creationDate,
    String createdBy
) {
    public static PolicyDto from(
        StockTransferPolicy p,
        String sourceSiteCode, String sourceSiteName,
        String destSiteCode, String destSiteName
    ) {
        return new PolicyDto(
            p.getId(),
            p.getSourceSiteId(), sourceSiteCode, sourceSiteName,
            p.getDestSiteId(),   destSiteCode,   destSiteName,
            p.getCreationDate(), p.getCreatedBy()
        );
    }
}
