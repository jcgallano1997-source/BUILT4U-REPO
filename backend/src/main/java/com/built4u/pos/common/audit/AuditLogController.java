package com.built4u.pos.common.audit;

import com.built4u.pos.common.audit.dto.AuditLogDto;
import com.built4u.pos.report.export.ExportResponses;
import com.built4u.pos.report.export.ExportTable;
import com.built4u.pos.report.export.ExportTableBuilders;
import com.built4u.pos.report.export.ReportPdfExporter;
import com.built4u.pos.report.export.ReportXlsxExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/** Audit Log — the single source of truth (gated by {@code MOD_AUDIT_LOG}). */
@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_AUDIT_LOG')")
public class AuditLogController {

    private final AuditLogService service;
    private final ReportPdfExporter pdfExporter;
    private final ReportXlsxExporter xlsxExporter;

    @GetMapping
    public ResponseEntity<?> list(
        @RequestParam(value = "username", required = false) String username,
        @RequestParam(value = "entity", required = false) String entity,
        @RequestParam(value = "action", required = false) String action,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "format", required = false) String format,
        @PageableDefault(size = 30) Pageable pageable
    ) throws IOException {
        String fmt = ExportResponses.normalize(format);
        if (ExportResponses.isExport(fmt)) {
            var all = service.list(username, entity, action, from, to, q,
                PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
            ExportTable table = ExportTableBuilders.auditLog(all);
            return ExportResponses.binaryResponse(table, fmt, "audit-log", pdfExporter, xlsxExporter);
        }
        Page<AuditLogDto> page = service.list(username, entity, action, from, to, q, pageable);
        return ResponseEntity.ok(page);
    }
}
