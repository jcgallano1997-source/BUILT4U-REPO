package com.built4u.pos.common.error;

import com.built4u.pos.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only error-log API for in-app debugging. Gated by {@code MOD_ERROR_LOG},
 *  which is admin/IT-only — the business OWNER role deliberately excludes it. */
@RestController
@RequestMapping("/api/admin/error-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_ERROR_LOG')")
public class ErrorLogController {

    private final ErrorLogService service;

    /** Newest-first, optionally filtered by site code. */
    @GetMapping
    public List<ErrorLog> list(
        @RequestParam(name = "siteCode", required = false) String siteCode,
        @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return service.recent(siteCode, limit);
    }

    /** Full detail (incl. stack trace) for one error row. */
    @GetMapping("/{id}")
    public ErrorLog get(@PathVariable("id") Long id) {
        ErrorLog row = service.get(id);
        if (row == null) throw new NotFoundException("Error log not found: " + id);
        return row;
    }
}
