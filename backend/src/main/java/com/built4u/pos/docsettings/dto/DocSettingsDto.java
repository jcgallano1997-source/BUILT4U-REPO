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

    // ── Logo & report-PDF page setup ─────────────────────────────────────────
    boolean hasLogo,
    String logoMime,
    String logoPosition,
    boolean showLogoPdf,
    String paperSize,
    String orientation,
    String marginPreset,
    String fontScale,
    boolean zebraStriping,
    boolean showPageNumbers,
    boolean showTimestamp,
    boolean showPrintedBy,

    // ── Receipt customization ────────────────────────────────────────────────
    boolean showLogoReceipt,
    String receiptHeaderNote,
    boolean receiptShowCashier,
    boolean receiptShowCustomer,
    boolean receiptShowVoucher,
    String receiptFormat,

    String updatedBy,
    LocalDateTime updatedAt
) {
    public static DocSettingsDto from(DocSettings d, boolean usingDefault) {
        return new DocSettingsDto(d.getSiteId(), usingDefault,
            d.getBusinessName(), d.getAddressLine(), d.getContactLine(), d.getTin(),
            d.getFooterNote(), d.getAccentColor(), d.getReceiptTitle(), d.getReceiptFooter(),
            d.getLogoImage() != null && d.getLogoImage().length > 0, d.getLogoMime(),
            d.getLogoPosition(), Boolean.TRUE.equals(d.getShowLogoPdf()),
            d.getPaperSize(), d.getOrientation(), d.getMarginPreset(), d.getFontScale(),
            Boolean.TRUE.equals(d.getZebraStriping()), Boolean.TRUE.equals(d.getShowPageNumbers()),
            Boolean.TRUE.equals(d.getShowTimestamp()), Boolean.TRUE.equals(d.getShowPrintedBy()),
            Boolean.TRUE.equals(d.getShowLogoReceipt()), d.getReceiptHeaderNote(),
            Boolean.TRUE.equals(d.getReceiptShowCashier()), Boolean.TRUE.equals(d.getReceiptShowCustomer()),
            Boolean.TRUE.equals(d.getReceiptShowVoucher()), d.getReceiptFormat(),
            d.getUpdatedBy(), d.getUpdatedAt());
    }
}
