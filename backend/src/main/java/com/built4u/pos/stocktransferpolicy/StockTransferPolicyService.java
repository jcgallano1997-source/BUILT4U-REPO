package com.built4u.pos.stocktransferpolicy;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.site.Site;
import com.built4u.pos.site.SiteRepository;
import com.built4u.pos.stocktransferpolicy.dto.AddPolicyRequest;
import com.built4u.pos.stocktransferpolicy.dto.PolicyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Cross-site stock transfer allow-list. Empty table → OPEN (any site may ship
 * to any other); ≥ 1 row → ENFORCED (only listed pairs permitted at ship time).
 * Existing IN_TRANSIT transfers shipped before a tightening are not
 * retroactively blocked — the policy is a write-time gate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransferPolicyService {

    private final StockTransferPolicyRepository policyRepository;
    private final SiteRepository siteRepository;

    @Transactional(readOnly = true)
    public boolean enforced() {
        return policyRepository.count() > 0;
    }

    /** True when the (source → dest) pair is permitted under the current policy. */
    @Transactional(readOnly = true)
    public boolean isAllowed(Long sourceSiteId, Long destSiteId) {
        if (!enforced()) return true;
        return policyRepository.existsBySourceSiteIdAndDestSiteId(sourceSiteId, destSiteId);
    }

    /** Throws a friendly error with the offending site names when the pair is disallowed. */
    @Transactional(readOnly = true)
    public void requireAllowed(Long sourceSiteId, Long destSiteId) {
        if (isAllowed(sourceSiteId, destSiteId)) return;
        throw new BadRequestException(
            "Stock transfer from " + siteName(sourceSiteId) + " to " + siteName(destSiteId)
            + " is not allowed by the current Stock Transfer Policy. An administrator must add "
            + "this pair under Administration → Stock Transfer Policy.");
    }

    /**
     * Filter a candidate destination list to those allowed for the given source
     * site. If the policy is OPEN, returns the input unchanged.
     */
    @Transactional(readOnly = true)
    public List<Long> filterAllowedDests(Long sourceSiteId, List<Long> candidateDestIds) {
        if (!enforced()) return candidateDestIds;
        var allowed = new HashSet<>(policyRepository.findAllowedDestSiteIds(sourceSiteId));
        return candidateDestIds.stream().filter(allowed::contains).toList();
    }

    /** Full rule list (with site code/name decorations) for the admin page. */
    @Transactional(readOnly = true)
    public List<PolicyDto> list() {
        var rows = policyRepository.findAllByOrderBySourceSiteIdAscDestSiteIdAsc();
        if (rows.isEmpty()) return List.of();
        Map<Long, Site> byId = new HashMap<>();
        for (var s : siteRepository.findAll()) byId.put(s.getId(), s);
        return rows.stream().map(p -> {
            Site src = byId.get(p.getSourceSiteId());
            Site dst = byId.get(p.getDestSiteId());
            return PolicyDto.from(p,
                src != null ? src.getCode() : "#" + p.getSourceSiteId(),
                src != null ? src.getName() : "#" + p.getSourceSiteId(),
                dst != null ? dst.getCode() : "#" + p.getDestSiteId(),
                dst != null ? dst.getName() : "#" + p.getDestSiteId());
        }).toList();
    }

    @Transactional
    public PolicyDto add(AddPolicyRequest req) {
        if (req.sourceSiteId() == null || req.destSiteId() == null) {
            throw new BadRequestException("sourceSiteId and destSiteId are required.");
        }
        if (req.sourceSiteId().equals(req.destSiteId())) {
            throw new BadRequestException("Source and destination must differ.");
        }
        Site src = siteRepository.findById(req.sourceSiteId())
            .orElseThrow(() -> new NotFoundException("Source site " + req.sourceSiteId() + " not found"));
        Site dst = siteRepository.findById(req.destSiteId())
            .orElseThrow(() -> new NotFoundException("Destination site " + req.destSiteId() + " not found"));
        if (policyRepository.existsBySourceSiteIdAndDestSiteId(req.sourceSiteId(), req.destSiteId())) {
            throw new BadRequestException(
                "This pair is already allowed: " + src.getName() + " → " + dst.getName() + ".");
        }
        StockTransferPolicy saved = policyRepository.save(StockTransferPolicy.builder()
            .sourceSiteId(req.sourceSiteId())
            .destSiteId(req.destSiteId())
            .build());
        log.info("Stock transfer policy rule added: {} ({}) → {} ({})",
            src.getCode(), src.getId(), dst.getCode(), dst.getId());
        return PolicyDto.from(saved, src.getCode(), src.getName(), dst.getCode(), dst.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!policyRepository.existsById(id)) {
            throw new NotFoundException("Policy rule " + id + " not found");
        }
        policyRepository.deleteById(id);
        log.info("Stock transfer policy rule {} deleted", id);
    }

    private String siteName(Long siteId) {
        return siteRepository.findById(siteId).map(Site::getName).orElse("#" + siteId);
    }
}
