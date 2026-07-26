package com.built4u.pos.purchaseorder;

import com.built4u.pos.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderDto;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_PURCHASE_ORDERS','MOD_GOODS_RECEIPTS')")
    public ResponseEntity<List<PurchaseOrderSummaryDto>> list(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "supplier", required = false) String supplier,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to
    ) {
        return ResponseEntity.ok(purchaseOrderService.list(status, supplier, from, to));
    }

    /** DRAFT POs that route to the current user (or all DRAFTs for ADMIN). */
    @GetMapping("/pending-my-approval")
    @PreAuthorize("hasAuthority('MOD_PURCHASE_ORDERS')")
    public ResponseEntity<List<PurchaseOrderSummaryDto>> pendingMyApproval() {
        return ResponseEntity.ok(purchaseOrderService.listPendingMyApproval());
    }

    @GetMapping("/{poNumber}")
    @PreAuthorize("hasAnyAuthority('MOD_PURCHASE_ORDERS','MOD_GOODS_RECEIPTS')")
    public ResponseEntity<PurchaseOrderDto> get(@PathVariable("poNumber") String poNumber) {
        return ResponseEntity.ok(purchaseOrderService.get(poNumber));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_PURCHASE_ORDERS')")
    public ResponseEntity<PurchaseOrderDto> create(@Valid @RequestBody CreatePurchaseOrderRequest req) {
        return ResponseEntity.ok(purchaseOrderService.create(req));
    }

    @PostMapping("/{poNumber}/approve")
    @PreAuthorize("hasAuthority('MOD_PURCHASE_ORDERS')")
    public ResponseEntity<PurchaseOrderDto> approve(@PathVariable("poNumber") String poNumber) {
        return ResponseEntity.ok(purchaseOrderService.setStatus(poNumber, PurchaseOrderStatus.APPROVED));
    }

    @PostMapping("/{poNumber}/cancel")
    @PreAuthorize("hasAuthority('MOD_PURCHASE_ORDERS')")
    public ResponseEntity<PurchaseOrderDto> cancel(@PathVariable("poNumber") String poNumber) {
        return ResponseEntity.ok(purchaseOrderService.setStatus(poNumber, PurchaseOrderStatus.CANCELLED));
    }
}
