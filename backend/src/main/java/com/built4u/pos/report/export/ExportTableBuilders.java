package com.built4u.pos.report.export;

import com.built4u.pos.goodsreceipt.dto.GoodsReceiptDto;
import com.built4u.pos.item.ItemDto;
import com.built4u.pos.payable.dto.PayableDto;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderSummaryDto;
import com.built4u.pos.receivable.dto.ReceivableDto;
import com.built4u.pos.report.dto.InventoryValuationDto;
import com.built4u.pos.report.dto.SalesOverviewDto;
import com.built4u.pos.stocktransfer.dto.StockTransferDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Turns report DTOs into the format-neutral {@link ExportTable}. */
public final class ExportTableBuilders {

    private ExportTableBuilders() {}

    private static List<Object> row(Object... cells) { return Arrays.asList(cells); }

    private static String range(LocalDate from, LocalDate to) { return "Period: " + from + " to " + to; }

    public static ExportTable salesOverview(SalesOverviewDto d) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("Sales", (long) d.salesCount()));
        rows.add(row("Gross", d.gross()));
        rows.add(row("Line discounts", d.lineDiscounts()));
        rows.add(row("Order discounts", d.orderDiscounts()));
        rows.add(row("Net sales", d.netSales()));
        List<String> footer = new ArrayList<>();
        footer.add("By payment mode:");
        for (var m : d.byMode()) footer.add("  " + m.mode() + " — " + m.count() + " sale(s), " + m.total());
        footer.add("By day:");
        for (var dt : d.byDay()) footer.add("  " + dt.date() + " — " + dt.count() + " sale(s), " + dt.net());
        return new ExportTable("Sales Overview", List.of(range(d.from(), d.to())),
            List.of("Metric", "Value"), rows, footer);
    }

    public static ExportTable inventorySnapshot(List<ItemDto> items) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        for (ItemDto it : items) {
            BigDecimal value = nz(it.quantity()).multiply(nz(it.costPrice()));
            totalValue = totalValue.add(value);
            rows.add(row(it.code(), it.name(), it.categoryName(), it.locationName(),
                it.quantity(), it.uom(), it.costPrice(), value, it.active() ? "Active" : "Inactive"));
        }
        return new ExportTable("Inventory Snapshot", List.of(),
            List.of("Code", "Name", "Category", "Location", "Qty", "UOM", "Cost", "Value", "Status"),
            rows, List.of("Total stock value: " + totalValue));
    }

    public static ExportTable inventoryValuation(InventoryValuationDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var c : d.categories()) {
            rows.add(row(c.category(), c.itemCount(), c.totalQty(), c.totalValue()));
        }
        return new ExportTable("Inventory Valuation", List.of(),
            List.of("Category", "Items", "Total qty", "Total value"), rows,
            List.of("Grand qty: " + d.grandQty(), "Grand value: " + d.grandValue()));
    }

    public static ExportTable receivables(List<ReceivableDto> list) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal totalBal = BigDecimal.ZERO;
        for (ReceivableDto r : list) {
            totalBal = totalBal.add(nz(r.balance()));
            rows.add(row(r.salesNumber(), r.customerName(), r.dueDate(), r.originalAmount(),
                r.amountPaid(), r.balance(), r.status() + (r.overdue() ? " (overdue)" : "")));
        }
        return new ExportTable("Accounts Receivable", List.of(),
            List.of("Sale #", "Customer", "Due", "Original", "Paid", "Balance", "Status"),
            rows, List.of("Total outstanding: " + totalBal));
    }

    public static ExportTable payables(List<PayableDto> list) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal totalBal = BigDecimal.ZERO;
        for (PayableDto p : list) {
            totalBal = totalBal.add(nz(p.balance()));
            rows.add(row(p.payeeName(), p.source(), p.grNumber() != null ? p.grNumber() : p.poNumber(),
                p.dueDate(), p.originalAmount(), p.amountPaid(), p.balance(),
                p.status() + (p.overdue() ? " (overdue)" : "")));
        }
        return new ExportTable("Accounts Payable", List.of(),
            List.of("Payee", "Source", "Ref", "Due", "Original", "Paid", "Balance", "Status"),
            rows, List.of("Total outstanding: " + totalBal));
    }

    public static ExportTable purchaseOrders(List<PurchaseOrderSummaryDto> list) {
        List<List<Object>> rows = new ArrayList<>();
        for (PurchaseOrderSummaryDto p : list) {
            rows.add(row(p.poNumber(), p.supplier(), p.deliveryDate(), (long) p.lineCount(),
                p.grandTotal(), p.status(), p.creationDate(), p.createdBy()));
        }
        return new ExportTable("Purchase Orders", List.of(),
            List.of("PO #", "Supplier", "Delivery", "Lines", "Total", "Status", "Created", "By"),
            rows, List.of(list.size() + " purchase order(s)"));
    }

    public static ExportTable goodsReceipts(List<GoodsReceiptDto> list) {
        List<List<Object>> rows = new ArrayList<>();
        for (GoodsReceiptDto g : list) {
            rows.add(row(g.grNumber(), g.poNumber(), g.supplier(), g.reference(),
                g.grandTotal(), g.creationDate(), g.createdBy()));
        }
        return new ExportTable("Goods Receipts", List.of(),
            List.of("GR #", "PO #", "Supplier", "Reference", "Total", "Received", "By"),
            rows, List.of(list.size() + " goods receipt(s)"));
    }

    public static ExportTable stockTransfers(List<StockTransferDto> list) {
        List<List<Object>> rows = new ArrayList<>();
        for (StockTransferDto t : list) {
            rows.add(row(t.transferNumber(), t.sourceSiteName(), t.destSiteName(),
                (long) t.lineCount(), t.status(), t.shippedAt(), t.sentBy()));
        }
        return new ExportTable("Stock Transfers", List.of(),
            List.of("Transfer #", "From", "To", "Lines", "Status", "Shipped", "By"),
            rows, List.of(list.size() + " transfer(s)"));
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
