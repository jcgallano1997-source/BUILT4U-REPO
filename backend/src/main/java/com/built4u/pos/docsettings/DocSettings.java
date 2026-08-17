package com.built4u.pos.docsettings;

import com.built4u.pos.common.tenant.YesNoConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Per-site document/branding config ({@code pos_doc_settings}), keyed by site_id.
 * Consumed by the report-PDF letterhead and the sale-receipt PDF. No row for a
 * site ⇒ the service falls through to hard-coded defaults.
 */
@Entity
@Table(name = "pos_doc_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocSettings {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "contact_line", length = 150)
    private String contactLine;

    @Column(length = 40)
    private String tin;

    @Column(name = "footer_note", length = 255)
    private String footerNote;

    @Column(name = "accent_color", nullable = false, length = 7)
    @Builder.Default
    private String accentColor = "#1D4ED8";

    @Column(name = "receipt_title", nullable = false, length = 60)
    @Builder.Default
    private String receiptTitle = "SALES RECEIPT";

    @Column(name = "receipt_footer", nullable = false, length = 255)
    @Builder.Default
    private String receiptFooter = "Thank you!";

    // ── Logo (shared by report PDFs and receipts) ────────────────────────────
    @Lob
    @Column(name = "logo_image")
    private byte[] logoImage;

    @Column(name = "logo_mime", length = 40)
    private String logoMime;

    @Column(name = "logo_position", nullable = false, length = 10)
    @Builder.Default
    private String logoPosition = "LEFT";

    // ── Report-PDF page setup ────────────────────────────────────────────────
    @Column(name = "show_logo_pdf", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean showLogoPdf = true;

    @Column(name = "paper_size", nullable = false, length = 10)
    @Builder.Default
    private String paperSize = "A4";

    @Column(name = "orientation", nullable = false, length = 10)
    @Builder.Default
    private String orientation = "LANDSCAPE";

    @Column(name = "margin_preset", nullable = false, length = 10)
    @Builder.Default
    private String marginPreset = "NORMAL";

    @Column(name = "font_scale", nullable = false, length = 10)
    @Builder.Default
    private String fontScale = "NORMAL";

    @Column(name = "zebra_striping", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean zebraStriping = true;

    @Column(name = "show_page_numbers", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean showPageNumbers = true;

    @Column(name = "show_timestamp", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean showTimestamp = true;

    @Column(name = "show_printed_by", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean showPrintedBy = true;

    // ── Receipt customization ────────────────────────────────────────────────
    @Column(name = "show_logo_receipt", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean showLogoReceipt = false;

    @Column(name = "receipt_header_note", length = 255)
    private String receiptHeaderNote;

    @Column(name = "receipt_show_cashier", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean receiptShowCashier = true;

    @Column(name = "receipt_show_customer", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean receiptShowCustomer = true;

    @Column(name = "receipt_show_voucher", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean receiptShowVoucher = true;

    /** Physical receipt layout: THERMAL_80MM (default) or BOND_LETTER (US Letter). */
    @Column(name = "receipt_format", nullable = false, length = 20)
    @Builder.Default
    private String receiptFormat = "THERMAL_80MM";

    // ── Network receipt printer + cash drawer (per site) ─────────────────────
    /** LAN printer host/IP; blank ⇒ network printing unavailable for this site. */
    @Column(name = "receipt_printer_host", length = 100)
    private String receiptPrinterHost;

    @Column(name = "receipt_printer_port", nullable = false)
    @Builder.Default
    private Integer receiptPrinterPort = 9100;

    @Column(name = "receipt_printer_enabled", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean receiptPrinterEnabled = false;

    /** Kick the cash drawer when a sale receipt prints. */
    @Column(name = "open_drawer_on_sale", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean openDrawerOnSale = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
