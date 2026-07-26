package com.built4u.pos.loyalty;

import com.built4u.pos.loyalty.dto.LoyaltyConfigDto;
import com.built4u.pos.loyalty.dto.UpdateLoyaltyConfigRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin: set the % of points customers earn (gated by {@code MOD_LOYALTY_CONFIG}). */
@RestController
@RequestMapping("/api/admin/loyalty-config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_LOYALTY_CONFIG')")
public class LoyaltyConfigAdminController {

    private final LoyaltyConfigService service;

    @GetMapping
    public ResponseEntity<LoyaltyConfigDto> get() {
        return ResponseEntity.ok(service.getProfile());
    }

    @PutMapping
    public ResponseEntity<LoyaltyConfigDto> update(@Valid @RequestBody UpdateLoyaltyConfigRequest req) {
        return ResponseEntity.ok(service.save(req));
    }
}
