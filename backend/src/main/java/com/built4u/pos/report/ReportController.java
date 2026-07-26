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
import com.built4u.pos.stocktransfer.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

/** Reporting hub. Every endpoint returns JSON by default, or a downloadable file with {@code ?format=pdf|xlsx}. */
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
    private final ReportPdfExporter pdf;
    private final ReportXlsxExporter xlsx;

    private ResponseEntity<?> render(Object json, ExportTable table, String format, String base) throws IOException {
        String fmt = ExportResponses.normalize(format);
        if (ExportResponses.isExport(fmt)) return ExportResponses.binaryResponse(table, fmt, base, pdf, xlsx);
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
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        LocalDate toD = parse(to, LocalDate.now());
        LocalDate fromD = parse(from, toD.minusDays(30));
        var dto = reportService.salesOverview(fromD, toD);
        return render(dto, ExportTableBuilders.salesOverview(dto), format, "sales-overview");
    }

    @GetMapping("/inventory-snapshot")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_SNAPSHOT')")
    public ResponseEntity<?> inventorySnapshot(@RequestParam(value = "format", required = false) String format) throws IOException {
        var items = reportService.inventorySnapshot();
        return render(items, ExportTableBuilders.inventorySnapshot(items), format, "inventory-snapshot");
    }

    @GetMapping("/inventory-valuation")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_VALUATION')")
    public ResponseEntity<?> inventoryValuation(@RequestParam(value = "format", required = false) String format) throws IOException {
        var dto = reportService.inventoryValuation();
        return render(dto, ExportTableBuilders.inventoryValuation(dto), format, "inventory-valuation");
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('MOD_RECEIVABLES_REPORT')")
    public ResponseEntity<?> receivables(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        var list = receivableService.list(status, null, null, overdue,
            PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
        return render(list, ExportTableBuilders.receivables(list), format, "receivables");
    }

    @GetMapping("/payables")
    @PreAuthorize("hasAuthority('MOD_PAYABLES_REPORT')")
    public ResponseEntity<?> payables(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        var list = payableService.list(status, source, null, null, overdue,
            PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
        return render(list, ExportTableBuilders.payables(list), format, "payables");
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('MOD_PURCHASE_ORDERS_REPORT')")
    public ResponseEntity<?> purchaseOrders(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "supplier", required = false) String supplier,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        var list = purchaseOrderService.list(status, supplier, from, to);
        return render(list, ExportTableBuilders.purchaseOrders(list), format, "purchase-orders");
    }

    @GetMapping("/goods-receipts")
    @PreAuthorize("hasAuthority('MOD_GOODS_RECEIPTS_REPORT')")
    public ResponseEntity<?> goodsReceipts(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to,
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        var list = goodsReceiptService.list(search, source, from, to);
        return render(list, ExportTableBuilders.goodsReceipts(list), format, "goods-receipts");
    }

    @GetMapping("/stock-transfers")
    @PreAuthorize("hasAuthority('MOD_STOCK_TRANSFER_REPORT')")
    public ResponseEntity<?> stockTransfers(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "direction", required = false) String direction,
        @RequestParam(value = "format", required = false) String format
    ) throws IOException {
        var list = stockTransferService.list(status, direction, null, null, null,
            PageRequest.of(0, ExportResponses.EXPORT_ROW_CAP)).getContent();
        return render(list, ExportTableBuilders.stockTransfers(list), format, "stock-transfers");
    }
}
