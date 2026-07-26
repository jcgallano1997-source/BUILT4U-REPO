package com.built4u.pos.sale;

import com.built4u.pos.sale.dto.CreateSaleRequest;
import com.built4u.pos.sale.dto.RefundRequest;
import com.built4u.pos.sale.dto.ReturnDto;
import com.built4u.pos.sale.dto.SaleDto;
import com.built4u.pos.sale.dto.SaleSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_POS')")
    public ResponseEntity<SaleDto> checkout(@Valid @RequestBody CreateSaleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.checkout(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_SALES','MOD_POS')")
    public ResponseEntity<List<SaleSummaryDto>> list(
        @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(saleService.list(status));
    }

    @GetMapping("/{salesNumber}")
    @PreAuthorize("hasAnyAuthority('MOD_SALES','MOD_POS')")
    public ResponseEntity<SaleDto> get(@PathVariable("salesNumber") String salesNumber) {
        return ResponseEntity.ok(saleService.get(salesNumber));
    }

    @PostMapping("/{salesNumber}/void")
    @PreAuthorize("hasAuthority('MOD_SALES')")
    public ResponseEntity<SaleDto> voidSale(@PathVariable("salesNumber") String salesNumber) {
        return ResponseEntity.ok(saleService.voidSale(salesNumber));
    }

    @PostMapping("/{salesNumber}/refund")
    @PreAuthorize("hasAuthority('MOD_SALES')")
    public ResponseEntity<ReturnDto> refund(@PathVariable("salesNumber") String salesNumber,
                                            @Valid @RequestBody RefundRequest req) {
        return ResponseEntity.ok(saleService.refund(salesNumber, req));
    }

    @GetMapping("/returns/{returnNumber}")
    @PreAuthorize("hasAnyAuthority('MOD_SALES','MOD_POS')")
    public ResponseEntity<ReturnDto> getReturn(@PathVariable("returnNumber") String returnNumber) {
        return ResponseEntity.ok(saleService.getReturn(returnNumber));
    }
}
