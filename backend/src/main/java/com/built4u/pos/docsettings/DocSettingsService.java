package com.built4u.pos.docsettings;

import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.docsettings.dto.DocSettingsDto;
import com.built4u.pos.docsettings.dto.UpdateDocSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Per-site document/branding settings. {@link #resolve()} returns the effective
 * config for the current site (a stored row, or a defaults instance) for the
 * PDF exporters; the admin get/save power the settings screen.
 */
@Service
@RequiredArgsConstructor
public class DocSettingsService {

    private final DocSettingsRepository repo;

    /** Effective settings for the current site — never null (defaults when no row). */
    @Transactional(readOnly = true)
    public DocSettings resolve() {
        Long siteId = TenantContext.getSiteId();
        if (siteId != null) {
            var own = repo.findById(siteId);
            if (own.isPresent()) return own.get();
        }
        return DocSettings.builder().siteId(siteId).build();
    }

    @Transactional(readOnly = true)
    public DocSettingsDto getProfile() {
        long siteId = TenantContext.requireSiteId();
        return repo.findById(siteId)
            .map(d -> DocSettingsDto.from(d, false))
            .orElseGet(() -> DocSettingsDto.from(DocSettings.builder().siteId(siteId).build(), true));
    }

    /**
     * Business identity + shared logo placement (gated by DOC_SETTINGS). This is the
     * base branding shown on both the report PDF letterhead and the sale receipt.
     */
    @Transactional
    public DocSettingsDto saveIdentity(UpdateDocSettingsRequest r) {
        DocSettings d = row();
        d.setBusinessName(blankToNull(r.businessName()));
        d.setAddressLine(blankToNull(r.addressLine()));
        d.setContactLine(blankToNull(r.contactLine()));
        d.setTin(blankToNull(r.tin()));
        if (r.logoPosition() != null) d.setLogoPosition(oneOf(r.logoPosition(), d.getLogoPosition(), "LEFT", "CENTER", "RIGHT"));
        return stamp(d);
    }

    /** Report-PDF layout (gated by PDF_CONFIG): footer note, accent, page setup, footer toggles. */
    @Transactional
    public DocSettingsDto savePdf(UpdateDocSettingsRequest r) {
        DocSettings d = row();
        d.setFooterNote(blankToNull(r.footerNote()));
        if (r.accentColor() != null && !r.accentColor().isBlank()) d.setAccentColor(r.accentColor().trim());
        if (r.showLogoPdf() != null) d.setShowLogoPdf(r.showLogoPdf());
        if (r.paperSize() != null) d.setPaperSize(oneOf(r.paperSize(), d.getPaperSize(), "A4", "LETTER"));
        if (r.orientation() != null) d.setOrientation(oneOf(r.orientation(), d.getOrientation(), "LANDSCAPE", "PORTRAIT"));
        if (r.marginPreset() != null) d.setMarginPreset(oneOf(r.marginPreset(), d.getMarginPreset(), "NARROW", "NORMAL", "WIDE"));
        if (r.fontScale() != null) d.setFontScale(oneOf(r.fontScale(), d.getFontScale(), "SMALL", "NORMAL", "LARGE"));
        if (r.zebraStriping() != null) d.setZebraStriping(r.zebraStriping());
        if (r.showPageNumbers() != null) d.setShowPageNumbers(r.showPageNumbers());
        if (r.showTimestamp() != null) d.setShowTimestamp(r.showTimestamp());
        if (r.showPrintedBy() != null) d.setShowPrintedBy(r.showPrintedBy());
        return stamp(d);
    }

    /** Sale-receipt customization (gated by RECEIPT_CONFIG): title/footer/note, toggles, physical format. */
    @Transactional
    public DocSettingsDto saveReceipt(UpdateDocSettingsRequest r) {
        DocSettings d = row();
        if (r.receiptTitle() != null && !r.receiptTitle().isBlank()) d.setReceiptTitle(r.receiptTitle().trim());
        if (r.receiptFooter() != null && !r.receiptFooter().isBlank()) d.setReceiptFooter(r.receiptFooter().trim());
        d.setReceiptHeaderNote(blankToNull(r.receiptHeaderNote()));
        if (r.showLogoReceipt() != null) d.setShowLogoReceipt(r.showLogoReceipt());
        if (r.receiptShowCashier() != null) d.setReceiptShowCashier(r.receiptShowCashier());
        if (r.receiptShowCustomer() != null) d.setReceiptShowCustomer(r.receiptShowCustomer());
        if (r.receiptShowVoucher() != null) d.setReceiptShowVoucher(r.receiptShowVoucher());
        if (r.receiptFormat() != null) d.setReceiptFormat(oneOf(r.receiptFormat(), d.getReceiptFormat(), "THERMAL_80MM", "BOND_LETTER"));
        return stamp(d);
    }

    private DocSettings row() {
        long siteId = TenantContext.requireSiteId();
        return repo.findById(siteId).orElseGet(() -> DocSettings.builder().siteId(siteId).build());
    }

    private DocSettingsDto stamp(DocSettings d) {
        d.setUpdatedAt(LocalDateTime.now());
        d.setUpdatedBy(currentUsername());
        return DocSettingsDto.from(repo.save(d), false);
    }

    /** Store (or replace) the per-site logo image. */
    @Transactional
    public void saveLogo(byte[] bytes, String mime) {
        long siteId = TenantContext.requireSiteId();
        DocSettings d = repo.findById(siteId).orElseGet(() -> DocSettings.builder().siteId(siteId).build());
        d.setLogoImage(bytes);
        d.setLogoMime(mime);
        d.setUpdatedAt(LocalDateTime.now());
        d.setUpdatedBy(currentUsername());
        repo.save(d);
    }

    /** Remove the per-site logo image. */
    @Transactional
    public void deleteLogo() {
        long siteId = TenantContext.requireSiteId();
        repo.findById(siteId).ifPresent(d -> {
            d.setLogoImage(null);
            d.setLogoMime(null);
            d.setUpdatedAt(LocalDateTime.now());
            d.setUpdatedBy(currentUsername());
            repo.save(d);
        });
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    /** Normalize a choice to one of {@code allowed} (upper-cased), else keep {@code fallback}. */
    private static String oneOf(String value, String fallback, String... allowed) {
        if (value == null) return fallback;
        String v = value.trim().toUpperCase();
        for (String a : allowed) if (a.equals(v)) return a;
        return fallback;
    }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SYSTEM" : a.getName();
    }
}
