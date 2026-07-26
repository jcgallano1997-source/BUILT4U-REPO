package com.built4u.pos.site;

import com.built4u.pos.site.dto.CreateSiteRequest;
import com.built4u.pos.site.dto.SiteSummaryDto;
import com.built4u.pos.site.dto.UpdateSiteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sites")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_SITES')")
public class SiteAdminController {

    private final SiteAdminService siteAdminService;

    @GetMapping
    public ResponseEntity<List<SiteSummaryDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(siteAdminService.list(search, includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteSummaryDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(siteAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<SiteSummaryDto> create(@Valid @RequestBody CreateSiteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteAdminService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiteSummaryDto> update(@PathVariable("id") Long id,
                                                 @Valid @RequestBody UpdateSiteRequest req) {
        return ResponseEntity.ok(siteAdminService.update(id, req));
    }
}
