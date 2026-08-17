package com.built4u.pos.printer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Printer/drawer setup actions for the current site's network thermal printer.
 * Gated by RECEIPT_CONFIG (the same permission that owns receipt/printer settings)
 * — POS receipt printing itself lives on the sales endpoint.
 */
@RestController
@RequestMapping("/api/printer")
@RequiredArgsConstructor
public class PrinterController {

    private final ReceiptPrintService printService;

    /** Print a short test slip and kick the drawer — confirms the printer is reachable. */
    @PostMapping("/test")
    @PreAuthorize("hasAnyAuthority('MOD_RECEIPT_CONFIG','MOD_POS')")
    public ResponseEntity<Void> test() {
        printService.printTest();
        return ResponseEntity.noContent().build();
    }

    /** Open the cash drawer without printing (e.g. mid-shift cash handling). */
    @PostMapping("/open-drawer")
    @PreAuthorize("hasAnyAuthority('MOD_RECEIPT_CONFIG','MOD_POS')")
    public ResponseEntity<Void> openDrawer() {
        printService.openDrawer();
        return ResponseEntity.noContent().build();
    }
}
