package com.built4u.pos.loyalty;

import com.built4u.pos.loyalty.dto.LoyaltyLedgerView;
import com.built4u.pos.loyalty.dto.RedeemRewardRequest;
import com.built4u.pos.loyalty.dto.RedeemRewardResult;
import com.built4u.pos.loyalty.dto.RewardDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cashier-facing loyalty actions (from the Customers screen). */
@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyRewardService rewardService;
    private final LoyaltyRedemptionService redemptionService;
    private final LoyaltyLedgerService ledgerService;

    /** Active reward catalog for the current site (Customers-screen redeem panel). */
    @GetMapping("/rewards")
    @PreAuthorize("hasAuthority('MOD_CUSTOMERS')")
    public ResponseEntity<List<RewardDto>> rewards() {
        return ResponseEntity.ok(rewardService.listActive());
    }

    @PostMapping("/redeem-reward")
    @PreAuthorize("hasAuthority('MOD_CUSTOMERS')")
    public ResponseEntity<RedeemRewardResult> redeemReward(@Valid @RequestBody RedeemRewardRequest req) {
        return ResponseEntity.ok(redemptionService.redeem(req));
    }

    /** Paged points-ledger history for a customer + balance reconciliation. */
    @GetMapping("/ledger")
    @PreAuthorize("hasAuthority('MOD_CUSTOMERS')")
    public ResponseEntity<LoyaltyLedgerView> ledger(
        @RequestParam("customerId") Long customerId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ledgerService.history(customerId, page, size));
    }
}
