package com.built4u.pos.docsettings.dto;

import com.built4u.pos.docsettings.DocSettings;

import java.time.LocalDateTime;

public record DocSettingsDto(
    Long siteId,
    boolean usingDefault,
    String businessName,
    String addressLine,
    String contactLine,
    String tin,
    String footerNote,
    String accentColor,
    String receiptTitle,
    String receiptFooter,
    String updatedBy,
    LocalDateTime updatedAt
) {
    public static DocSettingsDto from(DocSettings d, boolean usingDefault) {
        return new DocSettingsDto(d.getSiteId(), usingDefault,
            d.getBusinessName(), d.getAddressLine(), d.getContactLine(), d.getTin(),
            d.getFooterNote(), d.getAccentColor(), d.getReceiptTitle(), d.getReceiptFooter(),
            d.getUpdatedBy(), d.getUpdatedAt());
    }
}
