package com.built4u.pos.voucher;

import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.voucher.dto.ValidateVoucherRequest;
import com.built4u.pos.voucher.dto.VoucherEvaluation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** POS-facing voucher check. Returns an evaluation (HTTP 200 even when invalid). */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherRedeemService redeemService;

    @PostMapping("/validate")
    @PreAuthorize("hasAnyAuthority('MOD_POS','MOD_SALES')")
    public ResponseEntity<VoucherEvaluation> validate(@Valid @RequestBody ValidateVoucherRequest req) {
        Long siteId = TenantContext.requireSiteId();
        return ResponseEntity.ok(redeemService.evaluate(siteId, req.code(), req.subtotal(), req.customerId()));
    }
}
