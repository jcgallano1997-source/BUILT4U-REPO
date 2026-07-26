package com.built4u.pos.uom;

import com.built4u.pos.uom.dto.CreateUomRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uoms")
@RequiredArgsConstructor
public class UomController {

    private final UomService uomService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_UOMS','MOD_INVENTORY','MOD_STOCKTAKE','MOD_GOODS_RECEIPTS')")
    public ResponseEntity<List<UomDto>> list(
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(uomService.list(includeInactive));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_UOMS')")
    public ResponseEntity<UomDto> create(@Valid @RequestBody CreateUomRequest req) {
        return ResponseEntity.ok(uomService.create(req));
    }

    /** Update only toggles active. Body shape: {"active": true|false}. */
    @PutMapping("/{uom}")
    @PreAuthorize("hasAuthority('MOD_UOMS')")
    public ResponseEntity<UomDto> setActive(@PathVariable("uom") String uom, @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(uomService.setActive(uom, Boolean.TRUE.equals(body.get("active"))));
    }

    @DeleteMapping("/{uom}")
    @PreAuthorize("hasAuthority('MOD_UOMS')")
    public ResponseEntity<Void> delete(@PathVariable("uom") String uom) {
        uomService.softDelete(uom);
        return ResponseEntity.noContent().build();
    }
}
