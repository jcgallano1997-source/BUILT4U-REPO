package com.built4u.pos.report;

import com.built4u.pos.goodsreceipt.GoodsReceiptService;
import com.built4u.pos.payable.PayableService;
import com.built4u.pos.purchaseorder.PurchaseOrderService;
import com.built4u.pos.receivable.ReceivableService;
import com.built4u.pos.report.export.ExportResponses;
import com.built4u.pos.report.export.ExportTable;
import com.built4u.pos.report.export.ExportTableBuilders;
import com.built4u.pos.report.export.ReportPdfExporter;
import com.built4u.pos.report.export.ReportXlsxExporter;
import com.built4u.pos.reportemail.ReportEmailService;
import com.built4u.pos.stocktransfer.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Reporting hub. Every endpoint returns JSON by default, a downloadable file with
 * {@code ?format=pdf|xlsx}, or — with {@code &email=true} — mails that file to the
 * report's configured recipient (inert until a mail provider key is set).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final PurchaseOrderService purchaseOrderService;
    private final GoodsReceiptService goodsReceiptService;
    private final StockTransferService stockTransferService;
    private final ReportEmailService reportEmailService;
    private final ReportPdfExporter pdf;
    private final ReportXlsxExporter xlsx;

    private ResponseEntity<?> render(Object json, ExportTable table, String format, String base, boolean email) throws IOException {
        String fmt = ExportResponses.normalize(format);
        if (ExportResponses.isExport(fmt)) {
            if (email) return reportEmailService.deliver(base, fmt, table);
            return ExportResponses.binaryResponse(table, fmt, base, pdf, xlsx);
        }
        return ResponseEntity.ok(json);
    }

    private static LocalDate parse(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return fallback; }
    }

    @GetMapping("/sales-overview")
    @PreAuthorize("hasAuthority('MOD_SALES_REPORTS')")
    public ResponseEntity<?> salesOverview(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.salesOverview(fromD, toD);
        return render(dto, ExportTableBuilders.salesOverview(dto), format, "sales-overview", email);
    }

    @GetMapping("/sales-detailed")
    @PreAuthorize("hasAuthority('MOD_SALES_REPORTS')")
    public ResponseEntity<?> salesDetailed(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.salesDetailed(fromD, toD);
        return render(dto, ExportTableBuilders.salesDetailed(dto), format, "sales-detailed", email);
    }

    @GetMapping("/inventory-snapshot")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_SNAPSHOT')")
    public ResponseEntity<?> inventorySnapshot(
        @RequestParam(value = "asOf", required = false) String asOf,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate asOfD = parse(asOf, null);
        var items = reportService.inventorySnapshot(asOfD);
        return render(items, ExportTableBuilders.inventorySnapshot(items, asOfD), format, "inventory-snapshot", email);
    }

    @GetMapping("/inventory-movement")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_MOVEMENT')")
    public ResponseEntity<?> inventoryMovement(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.inventoryMovement(fromD, toD);
        return render(dto, ExportTableBuilders.inventoryMovement(dto), format, "inventory-movement", email);
    }

    @GetMapping("/inventory-valuation")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_VALUATION')")
    public ResponseEntity<?> inventoryValuation(
        @RequestParam(value = "asOf", required = false) String asOf,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate asOfD = parse(asOf, null);
        var dto = reportService.inventoryValuation(asOfD);
        return render(dto, ExportTableBuilders.inventoryValuation(dto, asOfD), format, "inventory-valuation", email);
    }

    @GetMapping("/shift-history")
    @PreAuthorize("hasAuthority('MOD_SHIFT_HISTORY_REPORT')")
    public ResponseEntity<?> shiftHistory(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var data = reportService.shiftHistory(status, search, fromD, toD);
        return render(data, ExportTableBuilders.shiftHistoryDetail(data), format, "shift-history", email);
    }

    @GetMapping("/profit-margin")
    @PreAuthorize("hasAuthority('MOD_PROFIT_REPORT')")
    public ResponseEntity<?> profitMargin(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.profitMargin(fromD, toD);
        return render(dto, ExportTableBuilders.profitMargin(dto), format, "profit-margin", email);
    }

    @GetMapping("/sales-by-cashier")
    @PreAuthorize("hasAuthority('MOD_SALES_ANALYTICS')")
    public ResponseEntity<?> salesByCashier(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.salesByCashier(fromD, toD);
        return render(dto, ExportTableBuilders.salesByCashier(dto), format, "sales-by-cashier", email);
    }

    @GetMapping("/sales-by-hour")
    @PreAuthorize("hasAuthority('MOD_SALES_ANALYTICS')")
    public ResponseEntity<?> salesByHour(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.salesByHour(fromD, toD);
        return render(dto, ExportTableBuilders.salesByHour(dto), format, "sales-by-hour", email);
    }

    @GetMapping("/dead-stock")
    @PreAuthorize("hasAuthority('MOD_DEAD_STOCK_REPORT')")
    public ResponseEntity<?> deadStock(
        @RequestParam(value = "minIdleDays", defaultValue = "30") int minIdleDays,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var dto = reportService.deadStock(Math.max(0, minIdleDays));
        return render(dto, ExportTableBuilders.deadStock(dto), format, "dead-stock", email);
    }

    @GetMapping("/customer-purchases")
    @PreAuthorize("hasAuthority('MOD_CUSTOMER_REPORT')")
    public ResponseEntity<?> customerPurchases(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.customerPurchases(fromD, toD);
        return render(dto, ExportTableBuilders.customerPurchases(dto), format, "customer-purchases", email);
    }

    @GetMapping("/reorder")
    @PreAuthorize("hasAuthority('MOD_REORDER_REPORT')")
    public ResponseEntity<?> reorderSuggestions(
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var dto = reportService.reorderSuggestions();
        return render(dto, ExportTableBuilders.reorderSuggestions(dto), format, "reorder-suggestions", email);
    }

    @GetMapping("/discounts-overrides")
    @PreAuthorize("hasAuthority('MOD_DISCOUNTS_REPORT')")
    public ResponseEntity<?> discountsOverrides(
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.discountsOverrides(fromD, toD);
        return render(dto, ExportTableBuilders.discountsOverrides(dto), format, "discounts-overrides", email);
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('MOD_RECEIVABLES_REPORT')")
    public ResponseEntity<?> receivables(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var list = receivableService.list(status, null, null, overdue,
            PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
        return render(list, ExportTableBuilders.receivables(list), format, "receivables", email);
    }

    @GetMapping("/payables")
    @PreAuthorize("hasAuthority('MOD_PAYABLES_REPORT')")
    public ResponseEntity<?> payables(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var list = payableService.list(status, source, null, null, overdue,
            PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
        return render(list, ExportTableBuilders.payables(list), format, "payables", email);
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('MOD_PURCHASE_ORDERS_REPORT')")
    public ResponseEntity<?> purchaseOrders(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "supplier", required = false) String supplier,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var list = purchaseOrderService.list(status, supplier, from, to);
        return render(list, ExportTableBuilders.purchaseOrders(list), format, "purchase-orders", email);
    }

    @GetMapping("/goods-receipts")
    @PreAuthorize("hasAuthority('MOD_GOODS_RECEIPTS_REPORT')")
    public ResponseEntity<?> goodsReceipts(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var list = goodsReceiptService.list(search, source, from, to);
        return render(list, ExportTableBuilders.goodsReceipts(list), format, "goods-receipts", email);
    }

    @GetMapping("/stock-transfers")
    @PreAuthorize("hasAuthority('MOD_STOCK_TRANSFER_REPORT')")
    public ResponseEntity<?> stockTransfers(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "direction", required = false) String direction,
        @RequestParam(value = "format", required = false) String format,
        @RequestParam(value = "email", defaultValue = "false") boolean email
    ) throws IOException {
        var list = stockTransferService.list(status, direction, null, null, null,
            PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
        return render(list, ExportTableBuilders.stockTransfers(list), format, "stock-transfers", email);
    }
}
