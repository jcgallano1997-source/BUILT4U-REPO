package com.built4u.pos.stocktransferpolicy;

import com.built4u.pos.stocktransferpolicy.dto.AddPolicyRequest;
import com.built4u.pos.stocktransferpolicy.dto.PolicyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Stock Transfer Policy admin. ADMIN-only via MOD_STOCK_TRANSFER_POLICY. */
@RestController
@RequestMapping("/api/stock-transfer-policy")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_STOCK_TRANSFER_POLICY')")
public class StockTransferPolicyController {

    private final StockTransferPolicyService service;

    /** Status + full rule list. {@code enforced=false} = OPEN (any → any allowed). */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok(Map.of(
            "enforced", service.enforced(),
            "rules", service.list()
        ));
    }

    @PostMapping
    public ResponseEntity<PolicyDto> add(@Valid @RequestBody AddPolicyRequest req) {
        return ResponseEntity.ok(service.add(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
