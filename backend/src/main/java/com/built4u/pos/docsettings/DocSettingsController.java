package com.built4u.pos.docsettings;

import com.built4u.pos.docsettings.dto.DocSettingsDto;
import com.built4u.pos.docsettings.dto.UpdateDocSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin: document branding for report PDFs and sale receipts (gated by {@code MOD_DOC_SETTINGS}). */
@RestController
@RequestMapping("/api/admin/doc-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_DOC_SETTINGS')")
public class DocSettingsController {

    private final DocSettingsService service;

    @GetMapping
    public ResponseEntity<DocSettingsDto> get() {
        return ResponseEntity.ok(service.getProfile());
    }

    @PutMapping
    public ResponseEntity<DocSettingsDto> update(@Valid @RequestBody UpdateDocSettingsRequest req) {
        return ResponseEntity.ok(service.save(req));
    }
}
