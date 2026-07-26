package com.built4u.pos.voucher;

import com.built4u.pos.voucher.dto.SaveVoucherRequest;
import com.built4u.pos.voucher.dto.VoucherDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin: create & manage discount voucher codes (gated by {@code MOD_VOUCHERS}). */
@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_VOUCHERS')")
public class VoucherAdminController {

    private final VoucherService service;

    @GetMapping
    public ResponseEntity<Page<VoucherDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
        @PageableDefault(size = 20, sort = "code") Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(search, includeInactive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoucherDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<VoucherDto> create(@Valid @RequestBody SaveVoucherRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoucherDto> update(@PathVariable("id") Long id,
                                             @Valid @RequestBody SaveVoucherRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
