package com.built4u.pos.supplier;

import com.built4u.pos.supplier.dto.CreateSupplierRequest;
import com.built4u.pos.supplier.dto.UpdateSupplierRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    private static final String READ_ANY =
        "hasAnyAuthority('MOD_SUPPLIERS','MOD_PURCHASE_ORDERS','MOD_GOODS_RECEIPTS','MOD_PAYABLES')";

    @GetMapping
    @PreAuthorize(READ_ANY)
    public ResponseEntity<List<SupplierDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(supplierService.list(search, includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ANY)
    public ResponseEntity<SupplierDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(supplierService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_SUPPLIERS')")
    public ResponseEntity<SupplierDto> create(@Valid @RequestBody CreateSupplierRequest req) {
        return ResponseEntity.ok(supplierService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_SUPPLIERS')")
    public ResponseEntity<SupplierDto> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateSupplierRequest req) {
        return ResponseEntity.ok(supplierService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_SUPPLIERS')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        supplierService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
