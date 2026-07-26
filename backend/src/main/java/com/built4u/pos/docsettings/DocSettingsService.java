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

    @Transactional
    public DocSettingsDto save(UpdateDocSettingsRequest r) {
        long siteId = TenantContext.requireSiteId();
        DocSettings d = repo.findById(siteId).orElseGet(() -> DocSettings.builder().siteId(siteId).build());
        d.setBusinessName(blankToNull(r.businessName()));
        d.setAddressLine(blankToNull(r.addressLine()));
        d.setContactLine(blankToNull(r.contactLine()));
        d.setTin(blankToNull(r.tin()));
        d.setFooterNote(blankToNull(r.footerNote()));
        if (r.accentColor() != null && !r.accentColor().isBlank()) d.setAccentColor(r.accentColor().trim());
        if (r.receiptTitle() != null && !r.receiptTitle().isBlank()) d.setReceiptTitle(r.receiptTitle().trim());
        if (r.receiptFooter() != null && !r.receiptFooter().isBlank()) d.setReceiptFooter(r.receiptFooter().trim());
        d.setUpdatedAt(LocalDateTime.now());
        d.setUpdatedBy(currentUsername());
        return DocSettingsDto.from(repo.save(d), false);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SYSTEM" : a.getName();
    }
}
