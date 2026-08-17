package com.built4u.pos.report.export;

import com.built4u.pos.common.audit.dto.AuditLogDto;
import com.built4u.pos.goodsreceipt.dto.GoodsReceiptDto;
import com.built4u.pos.item.ItemDto;
import com.built4u.pos.payable.dto.PayableDto;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderSummaryDto;
import com.built4u.pos.receivable.dto.ReceivableDto;
import com.built4u.pos.report.dto.CustomerPurchaseDto;
import com.built4u.pos.report.dto.DeadStockDto;
import com.built4u.pos.report.dto.DiscountOverrideDto;
import com.built4u.pos.report.dto.InventoryMovementDto;
import com.built4u.pos.report.dto.InventoryValuationDto;
import com.built4u.pos.report.dto.ProfitMarginDto;
import com.built4u.pos.report.dto.ReorderSuggestionDto;
import com.built4u.pos.report.dto.SalesByCashierDto;
import com.built4u.pos.report.dto.SalesByHourDto;
import com.built4u.pos.report.dto.SalesDetailedDto;
import com.built4u.pos.report.dto.SalesOverviewDto;
import com.built4u.pos.report.dto.ShiftHistoryReportDto;
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

    public static ExportTable salesDetailed(SalesDetailedDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var l : d.lines()) {
            rows.add(row(l.date(), l.salesNumber(), l.customer(), l.mode(), l.item(), l.category(),
                l.qty(), l.uom(), l.unitPrice(), l.lineDiscount(), l.lineTotal(),
                l.unitCogs(), l.lineCogs(), l.margin()));
        }
        return new ExportTable("Sales — Detailed", List.of(range(d.from(), d.to())),
            List.of("Date", "Sale #", "Customer", "Mode", "Item", "Category", "Qty", "UOM",
                "Unit price", "Line disc.", "Line total", "Unit cost", "Line cost", "Margin"),
            rows, List.of(
                d.saleCount() + " completed sale(s), " + d.lineCount() + " line(s)",
                "Total qty: " + d.totalQty(),
                "Total (line subtotals): " + d.totalAmount(),
                "Total cost of goods: " + d.totalCogs() + "  ·  Gross margin: " + d.totalMargin()));
    }

    public static ExportTable inventoryMovement(InventoryMovementDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var m : d.rows()) {
            rows.add(row(m.date(), m.item(), m.type(), m.reference(), m.qtyChange(), m.balanceAfter(), m.by()));
        }
        return new ExportTable("Inventory Movement", List.of(range(d.from(), d.to())),
            List.of("Date", "Item", "Type", "Reference", "Qty change", "Balance after", "By"),
            rows, List.of(d.count() + " movement(s)"));
    }

    /** {@code asOf} null ⇒ live snapshot; otherwise a subtitle marks the historical date. */
    private static List<String> asOfSubtitle(LocalDate asOf) {
        return asOf == null ? List.of() : List.of("As of " + asOf);
    }

    public static ExportTable inventorySnapshot(List<ItemDto> items) {
        return inventorySnapshot(items, null);
    }

    public static ExportTable inventorySnapshot(List<ItemDto> items, LocalDate asOf) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        for (ItemDto it : items) {
            BigDecimal value = nz(it.quantity()).multiply(nz(it.costPrice()));
            totalValue = totalValue.add(value);
            rows.add(row(it.code(), it.name(), it.categoryName(), it.locationName(),
                it.quantity(), it.uom(), it.costPrice(), value, it.active() ? "Active" : "Inactive"));
        }
        return new ExportTable("Inventory Snapshot", asOfSubtitle(asOf),
            List.of("Code", "Name", "Category", "Location", "Qty", "UOM", "Cost", "Value", "Status"),
            rows, List.of("Total stock value: " + totalValue));
    }

    public static ExportTable inventoryValuation(InventoryValuationDto d) {
        return inventoryValuation(d, null);
    }

    public static ExportTable inventoryValuation(InventoryValuationDto d, LocalDate asOf) {
        List<List<Object>> rows = new ArrayList<>();
        for (var c : d.categories()) {
            rows.add(row(c.category(), c.itemCount(), c.totalQty(), c.totalValue()));
        }
        return new ExportTable("Inventory Valuation", asOfSubtitle(asOf),
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

    public static ExportTable shiftHistoryDetail(List<ShiftHistoryReportDto> data) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal totalCashSales = BigDecimal.ZERO, totalCashRefunds = BigDecimal.ZERO, totalSales = BigDecimal.ZERO;
        long openShifts = 0, closedShifts = 0;
        int totalSaleRows = 0;
        for (ShiftHistoryReportDto s : data) {
            // One header row per shift (sale columns blank), then one row per sale
            // (shift columns blank apart from the shift # for join clarity).
            rows.add(row(s.shiftNumber(), s.cashier(), s.status(), s.openedAt(),
                s.closedAt(), nz(s.openingFloat()), nz(s.countedCash()), nz(s.expectedCash()),
                nz(s.cashVariance()), nz(s.cashSalesTotal()), nz(s.cashRefundsTotal()),
                nz(s.cashInTotal()), nz(s.cashOutTotal()), (long) s.saleCount(),
                "", "", "", "", "", ""));
            for (var sa : s.sales()) {
                rows.add(row(s.shiftNumber(), "", "", "", "", "", "", "", "", "", "", "", "", "",
                    sa.salesNumber(), sa.when(), sa.modeOfPayment(),
                    sa.customerName() == null ? "walk-in" : sa.customerName(),
                    nz(sa.grandTotal()), sa.status()));
                totalSaleRows++;
                if (!"VOIDED".equalsIgnoreCase(sa.status())) totalSales = totalSales.add(nz(sa.grandTotal()));
            }
            totalCashSales = totalCashSales.add(nz(s.cashSalesTotal()));
            totalCashRefunds = totalCashRefunds.add(nz(s.cashRefundsTotal()));
            if ("OPEN".equalsIgnoreCase(s.status())) openShifts++; else closedShifts++;
        }
        return new ExportTable("Shift History Report",
            List.of(data.size() + " shift(s) · " + totalSaleRows + " sale(s) during these shifts"),
            List.of("Shift #", "Cashier", "Status", "Opened", "Closed",
                "Opening float", "Counted cash", "Expected cash", "Variance",
                "Cash sales total", "Cash refunds total", "Cash in", "Cash out", "Sale count",
                "Sale #", "Sale when", "Mode", "Customer", "Grand total", "Sale status"),
            rows, List.of(
                "Shift counts: open " + openShifts + " · closed " + closedShifts,
                "Cash sales total (filtered): " + totalCashSales + "  ·  Cash refunds total: " + totalCashRefunds,
                "All-modes sales total (excl. voided): " + totalSales));
    }

    public static ExportTable discountsOverrides(DiscountOverrideDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.when(), r.salesNumber(), r.cashier(),
                r.customer() == null || r.customer().isBlank() ? "walk-in" : r.customer(),
                r.item(), r.quantity(), r.listPrice(), r.chargedPrice(),
                r.priceOverride(), r.lineDiscount(), r.giveback(),
                r.reason() == null ? "" : r.reason(),
                r.approvedBy() == null ? "" : r.approvedBy(), r.saleStatus()));
        }
        return new ExportTable("Discounts & Overrides", List.of(range(d.from(), d.to())),
            List.of("When", "Sale #", "Cashier", "Customer", "Item", "Qty",
                "List price", "Charged", "Price override", "Line discount", "Total giveback",
                "Reason", "Approved by", "Sale status"),
            rows, List.of(
                d.lineCount() + " line(s) with a price override or discount",
                "Price overrides (excl. voided): " + d.totalPriceOverride()
                    + "  ·  Line discounts: " + d.totalLineDiscount(),
                "Total giveback (excl. voided): " + d.totalGiveback()));
    }

    public static ExportTable reorderSuggestions(ReorderSuggestionDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.code(), r.name(), r.category() == null ? "" : r.category(),
                r.status().replace('_', ' '), r.onHand(), r.uom(),
                r.warning(), r.critical(), r.suggestedQty(), r.unitCost(), r.estimatedCost()));
        }
        return new ExportTable("Reorder Suggestions", List.of(),
            List.of("Code", "Name", "Category", "Status", "On hand", "UOM",
                "Warning", "Critical", "Suggested qty", "Unit cost", "Est. cost"),
            rows, List.of(
                d.itemCount() + " item(s) need reordering",
                "Estimated reorder cost: " + d.totalSuggestedCost()));
    }

    public static ExportTable profitMargin(ProfitMarginDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.item(), r.category() == null ? "" : r.category(), r.qtySold(),
                r.revenue(), r.cogs(), r.margin(), r.marginPct() + "%"));
        }
        return new ExportTable("Profit & Margin", List.of(range(d.from(), d.to())),
            List.of("Item", "Category", "Qty sold", "Revenue", "Cost of goods", "Margin", "Margin %"),
            rows, List.of(
                "Revenue: " + d.totalRevenue() + "  ·  Cost of goods: " + d.totalCogs(),
                "Gross margin: " + d.totalMargin() + "  (" + d.marginPct() + "%)"));
    }

    public static ExportTable salesByCashier(SalesByCashierDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.cashier(), r.saleCount(), r.gross(), r.discounts(), r.net(), r.avgSale()));
        }
        return new ExportTable("Sales by Cashier", List.of(range(d.from(), d.to())),
            List.of("Cashier", "Sales", "Gross", "Discounts", "Net sales", "Avg sale"),
            rows, List.of(d.rows().size() + " cashier(s)"));
    }

    public static ExportTable salesByHour(SalesByHourDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.hour(), r.saleCount(), r.net(), r.avgSale()));
        }
        return new ExportTable("Sales by Hour", List.of(range(d.from(), d.to())),
            List.of("Hour", "Sales", "Net sales", "Avg sale"),
            rows, List.of(d.rows().size() + " active hour(s)"));
    }

    public static ExportTable deadStock(DeadStockDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.code(), r.name(), r.category() == null ? "" : r.category(),
                r.onHand(), r.uom(), r.unitCost(), r.stockValue(),
                r.lastSold() == null ? "never" : r.lastSold(),
                r.daysIdle() == null ? "—" : r.daysIdle(), r.bucket()));
        }
        return new ExportTable("Dead Stock", List.of("Idle at least " + d.minIdleDays() + " day(s)"),
            List.of("Code", "Name", "Category", "On hand", "UOM", "Unit cost", "Stock value",
                "Last sold", "Days idle", "Bucket"),
            rows, List.of(
                d.itemCount() + " slow/dead item(s)",
                "Value tied up: " + d.totalIdleValue()));
    }

    public static ExportTable customerPurchases(CustomerPurchaseDto d) {
        List<List<Object>> rows = new ArrayList<>();
        for (var r : d.rows()) {
            rows.add(row(r.customer(), r.saleCount(), r.totalSpent(), r.avgSale(), r.lastPurchase()));
        }
        return new ExportTable("Customer Purchases", List.of(range(d.from(), d.to())),
            List.of("Customer", "Sales", "Total spent", "Avg sale", "Last purchase"),
            rows, List.of(d.rows().size() + " customer(s)"));
    }

    public static ExportTable auditLog(List<AuditLogDto> list) {
        List<List<Object>> rows = new ArrayList<>();
        for (AuditLogDto a : list) {
            rows.add(row(a.occurredAt(), a.username(), a.action(), a.entityName(),
                a.entityId(), a.reference(), a.changes()));
        }
        return new ExportTable("Audit Log", List.of(),
            List.of("When", "User", "Action", "Entity", "Entity id", "Reference", "Changes"),
            rows, List.of(list.size() + " change(s)"));
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
