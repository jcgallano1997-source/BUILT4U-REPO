package com.built4u.pos.printer;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.docsettings.DocSettings;
import com.built4u.pos.docsettings.DocSettingsService;
import com.built4u.pos.sale.SaleService;
import com.built4u.pos.sale.dto.SaleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Prints sale receipts to the per-site network thermal printer and drives the
 * cash drawer. Reads the printer config from {@link DocSettings}; the byte stream
 * comes from {@link EscPos} and the send from {@link NetworkReceiptPrinter}.
 */
@Service
@RequiredArgsConstructor
public class ReceiptPrintService {

    private final SaleService saleService;
    private final DocSettingsService docSettingsService;
    private final NetworkReceiptPrinter printer;

    /** Print a sale receipt; opens the drawer too when the site opts in. */
    public void printReceipt(String salesNumber) {
        DocSettings brand = requireEnabledPrinter();
        SaleDto sale = saleService.get(salesNumber);
        boolean drawer = Boolean.TRUE.equals(brand.getOpenDrawerOnSale());
        printer.send(brand.getReceiptPrinterHost(), port(brand), EscPos.receipt(sale, brand, drawer));
    }

    /** Print a short test slip (and kick the drawer) to confirm setup. */
    public void printTest() {
        DocSettings brand = requireConfiguredHost();
        printer.send(brand.getReceiptPrinterHost(), port(brand), EscPos.testSlip(brand, true));
    }

    /** Fire the cash drawer without printing anything. */
    public void openDrawer() {
        DocSettings brand = requireConfiguredHost();
        printer.send(brand.getReceiptPrinterHost(), port(brand), EscPos.openDrawer());
    }

    // ── Same jobs, handed to the browser to relay via a local print agent ─────

    /** The receipt job for {@code salesNumber}, for a client-side print agent to send. */
    public PrintJobDto receiptJob(String salesNumber) {
        DocSettings brand = requireEnabledPrinter();
        SaleDto sale = saleService.get(salesNumber);
        boolean drawer = Boolean.TRUE.equals(brand.getOpenDrawerOnSale());
        return job(brand, EscPos.receipt(sale, brand, drawer));
    }

    /** The test-slip job, for a client-side print agent to send. */
    public PrintJobDto testJob() {
        DocSettings brand = requireConfiguredHost();
        return job(brand, EscPos.testSlip(brand, true));
    }

    /** The drawer-kick job, for a client-side print agent to send. */
    public PrintJobDto openDrawerJob() {
        DocSettings brand = requireConfiguredHost();
        return job(brand, EscPos.openDrawer());
    }

    private static PrintJobDto job(DocSettings brand, byte[] data) {
        return new PrintJobDto(brand.getReceiptPrinterHost(), port(brand),
            java.util.Base64.getEncoder().encodeToString(data));
    }

    // ── guards ────────────────────────────────────────────────────────────────

    private DocSettings requireEnabledPrinter() {
        DocSettings brand = docSettingsService.resolve();
        if (!Boolean.TRUE.equals(brand.getReceiptPrinterEnabled())) {
            throw new BadRequestException("Receipt printing is turned off for this site (enable it in Document settings)");
        }
        return requireHost(brand);
    }

    private DocSettings requireConfiguredHost() {
        return requireHost(docSettingsService.resolve());
    }

    private DocSettings requireHost(DocSettings brand) {
        if (brand.getReceiptPrinterHost() == null || brand.getReceiptPrinterHost().isBlank()) {
            throw new BadRequestException("No printer host set for this site");
        }
        return brand;
    }

    private static int port(DocSettings brand) {
        return brand.getReceiptPrinterPort() == null ? 9100 : brand.getReceiptPrinterPort();
    }
}
