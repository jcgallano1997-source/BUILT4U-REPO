package com.built4u.pos.payment;

import com.built4u.pos.payment.dto.PaymentModeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** POS-facing: the active payment modes for the current site. */
@RestController
@RequestMapping("/api/payment-modes")
@RequiredArgsConstructor
public class PaymentModeController {

    private final PaymentModeService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_POS','MOD_SALES')")
    public ResponseEntity<List<PaymentModeDto>> active() {
        return ResponseEntity.ok(service.resolveActive());
    }
}
