package com.built4u.pos.payable;

import com.built4u.pos.payable.dto.CreateExpenseRequest;
import com.built4u.pos.payable.dto.PayableDetailDto;
import com.built4u.pos.payable.dto.PayableDto;
import com.built4u.pos.payable.dto.RecordPayablePaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Accounts Payable (gated by {@code MOD_PAYABLES}). */
@RestController
@RequestMapping("/api/payables")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_PAYABLES')")
public class PayableController {

    private final PayableService service;

    @GetMapping
    public ResponseEntity<Page<PayableDto>> list(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "supplierId", required = false) Long supplierId,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(status, source, supplierId, search, overdue, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayableDetailDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    /** Manually create an expense payable (utilities, reimbursements, etc.). */
    @PostMapping
    public ResponseEntity<PayableDto> createExpense(@Valid @RequestBody CreateExpenseRequest req) {
        return ResponseEntity.ok(service.createExpense(req));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PayableDetailDto> recordPayment(
        @PathVariable("id") Long id,
        @Valid @RequestBody RecordPayablePaymentRequest req
    ) {
        return ResponseEntity.ok(service.recordPayment(id, req.amount(), req.note()));
    }
}
