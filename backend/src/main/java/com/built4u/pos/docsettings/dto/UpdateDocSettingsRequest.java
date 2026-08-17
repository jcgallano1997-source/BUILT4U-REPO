package com.built4u.pos.docsettings.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDocSettingsRequest(
    @Size(max = 150) String businessName,
    @Size(max = 255) String addressLine,
    @Size(max = 150) String contactLine,
    @Size(max = 40)  String tin,
    @Size(max = 255) String footerNote,
    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "accentColor must be a #RRGGBB hex colour") String accentColor,
    @Size(max = 60)  String receiptTitle,
    @Size(max = 255) String receiptFooter,

    // ── Logo placement & report-PDF page setup ───────────────────────────────
    String logoPosition,
    Boolean showLogoPdf,
    String paperSize,
    String orientation,
    String marginPreset,
    String fontScale,
    Boolean zebraStriping,
    Boolean showPageNumbers,
    Boolean showTimestamp,
    Boolean showPrintedBy,

    // ── Receipt customization ────────────────────────────────────────────────
    Boolean showLogoReceipt,
    @Size(max = 255) String receiptHeaderNote,
    Boolean receiptShowCashier,
    Boolean receiptShowCustomer,
    Boolean receiptShowVoucher,
    String receiptFormat,

    // ── Network printer + drawer ─────────────────────────────────────────────
    @Size(max = 100) String receiptPrinterHost,
    Integer receiptPrinterPort,
    Boolean receiptPrinterEnabled,
    Boolean openDrawerOnSale
) {}
