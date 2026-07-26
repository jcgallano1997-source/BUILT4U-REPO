package com.built4u.pos.payment;

import com.built4u.pos.payment.dto.PaymentModeDto;
import com.built4u.pos.payment.dto.SavePaymentModeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin: configure payment methods & surcharges (gated by MOD_PAYMENT_MODES). */
@RestController
@RequestMapping("/api/admin/payment-modes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_PAYMENT_MODES')")
public class PaymentModeAdminController {

    private final PaymentModeService service;

    @GetMapping
    public ResponseEntity<List<PaymentModeDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<PaymentModeDto> create(@Valid @RequestBody SavePaymentModeRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentModeDto> update(@PathVariable("id") Long id, @Valid @RequestBody SavePaymentModeRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
