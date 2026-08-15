package com.built4u.pos.item;

import com.built4u.pos.item.dto.ImportResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Bulk inventory import from a spreadsheet (gated by {@code MOD_INVENTORY_IMPORT}). */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class InventoryImportController {

    private final InventoryImportService service;

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_IMPORT')")
    public ResponseEntity<ImportResultDto> importItems(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.importXlsx(file));
    }

    /** Download a ready-to-fill .xlsx template (correct headers + example rows). */
    @GetMapping("/import/template")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_IMPORT')")
    public ResponseEntity<byte[]> template() {
        byte[] xlsx = service.buildTemplate();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-import-template.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(xlsx);
    }
}
