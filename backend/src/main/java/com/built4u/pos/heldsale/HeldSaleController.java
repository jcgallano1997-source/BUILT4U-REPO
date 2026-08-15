package com.built4u.pos.heldsale;

import com.built4u.pos.heldsale.dto.HeldSaleDto;
import com.built4u.pos.heldsale.dto.HeldSaleSummaryDto;
import com.built4u.pos.heldsale.dto.SaveHeldSaleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Park (save) and recall in-progress POS carts. Gated by {@code MOD_POS}. */
@RestController
@RequestMapping("/api/held-sales")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_POS')")
public class HeldSaleController {

    private final HeldSaleService service;

    @GetMapping
    public List<HeldSaleSummaryDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public HeldSaleDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PostMapping
    public HeldSaleDto save(@Valid @RequestBody SaveHeldSaleRequest req) {
        return service.save(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
