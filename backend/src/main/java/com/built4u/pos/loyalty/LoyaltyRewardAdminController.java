package com.built4u.pos.loyalty;

import com.built4u.pos.loyalty.dto.RewardDto;
import com.built4u.pos.loyalty.dto.SaveRewardRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin: manage the loyalty reward catalog (gated by {@code MOD_LOYALTY_REWARDS}). */
@RestController
@RequestMapping("/api/admin/loyalty-rewards")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_LOYALTY_REWARDS')")
public class LoyaltyRewardAdminController {

    private final LoyaltyRewardService service;

    @GetMapping
    public ResponseEntity<List<RewardDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<RewardDto> create(@Valid @RequestBody SaveRewardRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RewardDto> update(@PathVariable("id") Long id,
                                            @Valid @RequestBody SaveRewardRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
