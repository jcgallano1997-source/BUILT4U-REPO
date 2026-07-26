package com.built4u.pos.loyalty;

import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.loyalty.dto.LoyaltyConfigDto;
import com.built4u.pos.loyalty.dto.UpdateLoyaltyConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Resolves the loyalty earn rate for the current site: site row → hard default
 * 5%. {@link #resolveRate()} returns the rate as a FRACTION (0.05) ready to
 * multiply a grand total.
 */
@Service
@RequiredArgsConstructor
public class LoyaltyConfigService {

    private static final BigDecimal DEFAULT_PERCENT = new BigDecimal("5");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final LoyaltyConfigRepository repo;

    /** Effective config row for the current site (never null; site → default). */
    @Transactional(readOnly = true)
    public LoyaltyConfig resolve() {
        Long siteId = TenantContext.getSiteId();
        if (siteId != null) {
            var own = repo.findById(siteId);
            if (own.isPresent()) return own.get();
        }
        return LoyaltyConfig.builder().siteId(siteId).pointsRate(DEFAULT_PERCENT).build();
    }

    /** Earn rate as a fraction, e.g. 5% → 0.05. */
    @Transactional(readOnly = true)
    public BigDecimal resolveRate() {
        BigDecimal pct = resolve().getPointsRate();
        if (pct == null || pct.signum() < 0) pct = DEFAULT_PERCENT;
        return pct.divide(HUNDRED, 6, RoundingMode.HALF_UP);
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoyaltyConfigDto getProfile() {
        long siteId = TenantContext.requireSiteId();
        var own = repo.findById(siteId);
        if (own.isPresent()) return toDto(own.get(), false);
        return new LoyaltyConfigDto(siteId, true, DEFAULT_PERCENT, BigDecimal.ONE, null, null);
    }

    @Transactional
    public LoyaltyConfigDto save(UpdateLoyaltyConfigRequest r) {
        long siteId = TenantContext.requireSiteId();
        LoyaltyConfig c = repo.findById(siteId)
            .orElseGet(() -> LoyaltyConfig.builder().siteId(siteId).build());
        c.setPointsRate(r.pointsRate());
        c.setRedeemValue(r.redeemValue());
        c.setUpdatedAt(LocalDateTime.now());
        c.setUpdatedBy(currentUsername());
        repo.save(c);
        return toDto(c, false);
    }

    private LoyaltyConfigDto toDto(LoyaltyConfig c, boolean usingDefault) {
        return new LoyaltyConfigDto(c.getSiteId(), usingDefault,
            c.getPointsRate(),
            c.getRedeemValue() == null ? BigDecimal.ONE : c.getRedeemValue(),
            c.getUpdatedBy(), c.getUpdatedAt());
    }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SYSTEM" : a.getName();
    }
}
