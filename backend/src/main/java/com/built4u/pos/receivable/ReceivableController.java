package com.built4u.pos.receivable;

import com.built4u.pos.receivable.dto.ReceivableDetailDto;
import com.built4u.pos.receivable.dto.ReceivableDto;
import com.built4u.pos.receivable.dto.RecordPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Accounts Receivable — list, detail, and collections (gated by {@code MOD_RECEIVABLES}). */
@RestController
@RequestMapping("/api/receivables")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_RECEIVABLES')")
public class ReceivableController {

    private final ReceivableService service;

    @GetMapping
    public ResponseEntity<Page<ReceivableDto>> list(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "customerId", required = false) Long customerId,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(status, customerId, search, overdue, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceivableDetailDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<ReceivableDetailDto> recordPayment(
        @PathVariable("id") Long id,
        @Valid @RequestBody RecordPaymentRequest req
    ) {
        return ResponseEntity.ok(service.recordPayment(id, req.amount(), req.note()));
    }
}
