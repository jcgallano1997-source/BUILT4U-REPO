package com.built4u.pos.docsettings;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.docsettings.dto.DocSettingsDto;
import com.built4u.pos.docsettings.dto.UpdateDocSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Admin: document branding & templating for report PDFs and sale receipts. Split
 * into independently-permissionable sections so access can be separated per role:
 * business identity + logo ({@code DOC_SETTINGS}), report-PDF layout ({@code PDF_CONFIG}),
 * and the sale receipt ({@code RECEIPT_CONFIG}).
 */
@RestController
@RequestMapping("/api/admin/doc-settings")
@RequiredArgsConstructor
public class DocSettingsController {

    private static final long MAX_LOGO_BYTES = 512 * 1024;

    private final DocSettingsService service;

    /** Read the full profile — allowed to anyone who manages any document section. */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_DOC_SETTINGS','MOD_PDF_CONFIG','MOD_RECEIPT_CONFIG')")
    public ResponseEntity<DocSettingsDto> get() {
        return ResponseEntity.ok(service.getProfile());
    }

    @PutMapping("/identity")
    @PreAuthorize("hasAuthority('MOD_DOC_SETTINGS')")
    public ResponseEntity<DocSettingsDto> updateIdentity(@Valid @RequestBody UpdateDocSettingsRequest req) {
        return ResponseEntity.ok(service.saveIdentity(req));
    }

    @PutMapping("/pdf")
    @PreAuthorize("hasAuthority('MOD_PDF_CONFIG')")
    public ResponseEntity<DocSettingsDto> updatePdf(@Valid @RequestBody UpdateDocSettingsRequest req) {
        return ResponseEntity.ok(service.savePdf(req));
    }

    @PutMapping("/receipt")
    @PreAuthorize("hasAuthority('MOD_RECEIPT_CONFIG')")
    public ResponseEntity<DocSettingsDto> updateReceipt(@Valid @RequestBody UpdateDocSettingsRequest req) {
        return ResponseEntity.ok(service.saveReceipt(req));
    }

    @PostMapping("/logo")
    @PreAuthorize("hasAuthority('MOD_DOC_SETTINGS')")
    public ResponseEntity<DocSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BadRequestException("No file uploaded.");
        String mime = file.getContentType();
        if (mime == null || !(mime.equalsIgnoreCase("image/png") || mime.equalsIgnoreCase("image/jpeg"))) {
            throw new BadRequestException("Logo must be a PNG or JPEG image.");
        }
        if (file.getSize() > MAX_LOGO_BYTES) throw new BadRequestException("Logo must be 512 KB or smaller.");
        service.saveLogo(file.getBytes(), mime.toLowerCase());
        return ResponseEntity.ok(service.getProfile());
    }

    @GetMapping("/logo")
    @PreAuthorize("hasAnyAuthority('MOD_DOC_SETTINGS','MOD_PDF_CONFIG','MOD_RECEIPT_CONFIG')")
    public ResponseEntity<byte[]> getLogo() {
        DocSettings d = service.resolve();
        byte[] img = d.getLogoImage();
        if (img == null || img.length == 0) return ResponseEntity.notFound().build();
        String mime = d.getLogoMime() == null || d.getLogoMime().isBlank() ? "image/png" : d.getLogoMime();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).body(img);
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasAuthority('MOD_DOC_SETTINGS')")
    public ResponseEntity<DocSettingsDto> deleteLogo() {
        service.deleteLogo();
        return ResponseEntity.ok(service.getProfile());
    }
}
