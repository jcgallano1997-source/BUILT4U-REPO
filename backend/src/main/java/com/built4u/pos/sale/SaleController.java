package com.built4u.pos.sale;

import com.built4u.pos.sale.dto.CreateSaleRequest;
import com.built4u.pos.sale.dto.RefundRequest;
import com.built4u.pos.sale.dto.ReturnDto;
import com.built4u.pos.sale.dto.SaleDto;
import com.built4u.pos.sale.dto.SaleSummaryDto;
import com.built4u.pos.printer.ReceiptPrintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SaleReceiptPdfService receiptPdfService;
    private final ReceiptPrintService receiptPrintService;

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

    /** Printable PDF receipt for a sale (uses the site's document branding). */
    @GetMapping(value = "/{salesNumber}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('MOD_SALES','MOD_POS')")
    public ResponseEntity<byte[]> receipt(
        @PathVariable("salesNumber") String salesNumber,
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        byte[] body = receiptPdfService.generate(salesNumber, format);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt-" + salesNumber + ".pdf\"")
            .body(body);
    }

    /** Print the sale receipt to the site's network thermal printer (opens the drawer if configured). */
    @PostMapping("/{salesNumber}/print")
    @PreAuthorize("hasAnyAuthority('MOD_SALES','MOD_POS')")
    public ResponseEntity<Void> print(@PathVariable("salesNumber") String salesNumber) {
        receiptPrintService.printReceipt(salesNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/returns/{returnNumber}")
    @PreAuthorize("hasAnyAuthority('MOD_SALES','MOD_POS')")
    public ResponseEntity<ReturnDto> getReturn(@PathVariable("returnNumber") String returnNumber) {
        return ResponseEntity.ok(saleService.getReturn(returnNumber));
    }
}
