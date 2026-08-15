package com.built4u.pos.site;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.site.dto.CreateSiteRequest;
import com.built4u.pos.site.dto.SiteSummaryDto;
import com.built4u.pos.site.dto.UpdateSiteRequest;
import com.built4u.pos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin operations on {@link Site}: list / get / create / update.
 *
 * <p>Site code is immutable after creation (it's referenced in JWT claims and
 * refresh-token bindings). Hard delete is not exposed (site_id is referenced by
 * all business data) — use {@code active=false} instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SiteAdminService {

    private final SiteRepository siteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SiteSummaryDto> list(String search, boolean includeInactive) {
        String needle = search == null ? "" : search.toLowerCase().trim();
        return siteRepository.findAllByOrderByCodeAsc().stream()
            .filter(s -> includeInactive || s.isActive())
            .filter(s -> needle.isEmpty()
                || s.getCode().toLowerCase().contains(needle)
                || s.getName().toLowerCase().contains(needle)
                || (s.getAddress() != null && s.getAddress().toLowerCase().contains(needle)))
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public SiteSummaryDto get(Long id) {
        return toSummary(loadSite(id));
    }

    @Transactional
    public SiteSummaryDto create(CreateSiteRequest req) {
        String code = req.code().trim().toUpperCase();
        if (siteRepository.existsByCode(code)) {
            throw new ConflictException("Site code '" + code + "' is already used");
        }
        Site s = Site.builder()
            .code(code)
            .name(req.name().trim())
            .address(blankToNull(req.address()))
            .active("Y")
            .createdAt(LocalDateTime.now())
            .build();
        Site saved = siteRepository.save(s);

        // The business OWNER supervises every site — auto-grant the master
        // account access to each newly created site (no manual step needed).
        userRepository.findByUsername("owner").ifPresent(owner -> {
            owner.getSites().add(saved);
            userRepository.save(owner);
            log.info("Auto-linked owner account to new site {}", saved.getCode());
        });

        log.info("Admin {} created site {} ({})", currentUsername(), saved.getCode(), saved.getName());
        return toSummary(saved);
    }

    @Transactional
    public SiteSummaryDto update(Long id, UpdateSiteRequest req) {
        Site s = loadSite(id);
        boolean wasActive = s.isActive();
        boolean nowActive = Boolean.TRUE.equals(req.active());

        // Don't let the last active site be deactivated (users could no longer log in).
        if (wasActive && !nowActive && siteRepository.countActive() <= 1) {
            throw new BadRequestException(
                "Cannot deactivate site '" + s.getCode() +
                "' — at least one site must remain active so users can log in");
        }

        s.setName(req.name().trim());
        s.setAddress(blankToNull(req.address()));
        s.setActive(nowActive ? "Y" : "N");
        Site saved = siteRepository.save(s);
        log.info("Admin {} updated site {} (active={})", currentUsername(), saved.getCode(), saved.isActive());
        return toSummary(saved);
    }

    private Site loadSite(Long id) {
        return siteRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Site " + id + " not found"));
    }

    private SiteSummaryDto toSummary(Site s) {
        return new SiteSummaryDto(
            s.getId(), s.getCode(), s.getName(), s.getAddress(),
            s.isActive(), siteRepository.countUsers(s.getId()), s.getCreatedAt());
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
