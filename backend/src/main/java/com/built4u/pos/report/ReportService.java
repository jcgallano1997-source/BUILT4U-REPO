package com.built4u.pos.report;

import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.customer.Customer;
import com.built4u.pos.customer.CustomerRepository;
import com.built4u.pos.item.ItemDto;
import com.built4u.pos.item.ItemService;
import com.built4u.pos.report.dto.DiscountOverrideDto;
import com.built4u.pos.report.dto.InventoryMovementDto;
import com.built4u.pos.report.dto.InventoryValuationDto;
import com.built4u.pos.report.dto.ReorderSuggestionDto;
import com.built4u.pos.report.dto.SalesDetailedDto;
import com.built4u.pos.report.dto.SalesOverviewDto;
import com.built4u.pos.report.dto.ShiftHistoryReportDto;
import com.built4u.pos.sale.Sale;
import com.built4u.pos.sale.SaleItem;
import com.built4u.pos.sale.SaleItemRepository;
import com.built4u.pos.sale.SalePaymentRepository;
import com.built4u.pos.sale.SaleRepository;
import com.built4u.pos.shift.Shift;
import com.built4u.pos.shift.ShiftRepository;
import com.built4u.pos.transactionlog.TransactionLog;
import com.built4u.pos.transactionlog.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sales + inventory report queries (the reports that don't map to an existing list endpoint). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomerRepository customerRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final ShiftRepository shiftRepository;
    private final ItemService itemService;

    @Transactional(readOnly = true)
    public SalesOverviewDto salesOverview(LocalDate from, LocalDate to) {
        long siteId = TenantContext.requireSiteId();
        List<Sale> sales = saleRepository.findCompletedInRange(
            siteId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        BigDecimal gross = BigDecimal.ZERO, lineDisc = BigDecimal.ZERO,
                   orderDisc = BigDecimal.ZERO, net = BigDecimal.ZERO;
        Map<String, long[]> modeCount = new LinkedHashMap<>();
        Map<String, BigDecimal> modeTotal = new LinkedHashMap<>();
        Map<LocalDate, long[]> dayCount = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> dayNet = new LinkedHashMap<>();

        for (Sale s : sales) {
            gross = gross.add(nz(s.getTotal()));
            lineDisc = lineDisc.add(nz(s.getTotalDiscItem()));
            orderDisc = orderDisc.add(nz(s.getDiscountAll()));
            net = net.add(nz(s.getGrandTotal()));

            LocalDate day = s.getCreationDate() == null ? from : s.getCreationDate().toLocalDate();
            dayCount.computeIfAbsent(day, k -> new long[1])[0]++;
            dayNet.merge(day, nz(s.getGrandTotal()), BigDecimal::add);
        }

        // "By payment mode" comes from the applied tenders so a split sale counts
        // under each method it used; the mode totals reconcile to net sales.
        for (var p : salePaymentRepository.findForCompletedSalesInRange(
                siteId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            String mode = p.getMode() == null ? "—" : p.getMode();
            modeCount.computeIfAbsent(mode, k -> new long[1])[0]++;
            modeTotal.merge(mode, nz(p.getAmount()), BigDecimal::add);
        }

        List<SalesOverviewDto.ModeTotal> byMode = new ArrayList<>();
        modeTotal.forEach((m, t) -> byMode.add(new SalesOverviewDto.ModeTotal(m, modeCount.get(m)[0], t)));
        List<SalesOverviewDto.DayTotal> byDay = new ArrayList<>();
        dayNet.forEach((d, t) -> byDay.add(new SalesOverviewDto.DayTotal(d, dayCount.get(d)[0], t)));

        return new SalesOverviewDto(from, to, sales.size(), gross, lineDisc, orderDisc, net, byMode, byDay);
    }

    /** Current stock snapshot (all items, including inactive). */
    @Transactional(readOnly = true)
    public List<ItemDto> inventorySnapshot() {
        return itemService.list(null, null, null, true, null);
    }

    /**
     * Historical stock snapshot as of the end of {@code asOf}. Reconstructs each
     * item's quantity by taking today's balance and backing out every journalled
     * movement dated after {@code asOf}. Items created after {@code asOf} (they
     * didn't exist yet) are excluded. A null / today / future date returns the
     * live snapshot. Accuracy depends on every stock change being journalled —
     * opening balances, edits, imports, sales, adjustments and transfers all are.
     */
    @Transactional(readOnly = true)
    public List<ItemDto> inventorySnapshot(LocalDate asOf) {
        List<ItemDto> current = inventorySnapshot();
        if (asOf == null || !asOf.isBefore(LocalDate.now())) return current;

        long siteId = TenantContext.requireSiteId();
        LocalDateTime cutoff = asOf.plusDays(1).atStartOfDay();   // start of the day after asOf
        Map<Long, BigDecimal> deltaAfter = new HashMap<>();
        for (TransactionLog t : transactionLogRepository.findFrom(siteId, cutoff)) {
            deltaAfter.merge(t.getItemId(), signedChange(t), BigDecimal::add);
        }

        List<ItemDto> out = new ArrayList<>();
        for (ItemDto it : current) {
            if (it.createdAt() != null && !it.createdAt().isBefore(cutoff)) continue;  // not born yet
            BigDecimal asOfQty = nz(it.quantity()).subtract(deltaAfter.getOrDefault(it.id(), BigDecimal.ZERO));
            out.add(it.withQuantity(asOfQty));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public InventoryValuationDto inventoryValuation() {
        return valuationFrom(inventorySnapshot());
    }

    /** Stock value grouped by category as of the end of {@code asOf} (uses current cost). */
    @Transactional(readOnly = true)
    public InventoryValuationDto inventoryValuation(LocalDate asOf) {
        return valuationFrom(inventorySnapshot(asOf));
    }

    private InventoryValuationDto valuationFrom(List<ItemDto> items) {
        Map<String, long[]> count = new LinkedHashMap<>();
        Map<String, BigDecimal> qty = new LinkedHashMap<>();
        Map<String, BigDecimal> value = new LinkedHashMap<>();
        BigDecimal grandQty = BigDecimal.ZERO, grandValue = BigDecimal.ZERO;

        for (ItemDto it : items) {
            String cat = it.categoryName() == null ? "—" : it.categoryName();
            BigDecimal q = nz(it.quantity());
            BigDecimal v = q.multiply(nz(it.costPrice()));
            count.computeIfAbsent(cat, k -> new long[1])[0]++;
            qty.merge(cat, q, BigDecimal::add);
            value.merge(cat, v, BigDecimal::add);
            grandQty = grandQty.add(q);
            grandValue = grandValue.add(v);
        }
        List<InventoryValuationDto.CategoryValuation> cats = new ArrayList<>();
        qty.forEach((c, q) -> cats.add(new InventoryValuationDto.CategoryValuation(
            c, count.get(c)[0], q, value.get(c))));
        return new InventoryValuationDto(grandQty, grandValue, cats);
    }

    /** Line-level detail of completed sales in a period (revenue audit). */
    @Transactional(readOnly = true)
    public SalesDetailedDto salesDetailed(LocalDate from, LocalDate to) {
        long siteId = TenantContext.requireSiteId();
        List<Sale> sales = saleRepository.findCompletedInRange(
            siteId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        Map<Long, String> custName = new HashMap<>();
        for (Customer c : customerRepository.findBySiteIdOrderByCustomerNameAsc(siteId)) {
            custName.put(c.getCustomerId(), c.getCustomerName());
        }
        Map<Long, String> itemCategory = new HashMap<>();
        for (ItemDto it : inventorySnapshot()) {
            itemCategory.put(it.id(), it.categoryName());
        }

        List<SalesDetailedDto.Line> lines = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO, totalAmount = BigDecimal.ZERO,
                   totalCogs = BigDecimal.ZERO, totalMargin = BigDecimal.ZERO;
        for (Sale s : sales) {
            String customer = s.getCustomerId() == null ? ""
                : custName.getOrDefault(s.getCustomerId(), "#" + s.getCustomerId());
            for (SaleItem si : saleItemRepository.findBySiteIdAndSalesNumberOrderByItemIdAsc(siteId, s.getSalesNumber())) {
                BigDecimal qty = nz(si.getQuantity());
                BigDecimal lineTotal = nz(si.getSubTotal());
                BigDecimal unitCogs = nz(si.getUnitCogs());
                BigDecimal lineCogs = unitCogs.multiply(qty);
                BigDecimal margin = lineTotal.subtract(lineCogs);
                lines.add(new SalesDetailedDto.Line(
                    s.getCreationDate(), s.getSalesNumber(), customer, s.getModeOfPayment(),
                    si.getItemDesc(), itemCategory.get(si.getItemId()), qty, si.getUom(),
                    nz(si.getUnitCost()), nz(si.getAdjustment()), lineTotal, unitCogs, lineCogs, margin));
                totalQty = totalQty.add(qty);
                totalAmount = totalAmount.add(lineTotal);
                totalCogs = totalCogs.add(lineCogs);
                totalMargin = totalMargin.add(margin);
            }
        }
        return new SalesDetailedDto(from, to, sales.size(), lines.size(), totalQty, totalAmount, totalCogs, totalMargin, lines);
    }

    /**
     * Discounts &amp; Overrides audit: every sale line sold below its catalog price
     * (price override) or carrying a line discount, in the period. Read purely from
     * the {@code pos_sale_item} snapshot (list_price vs unit_cost + adjustment), so
     * it stays correct even after an item's catalog price changes. Money totals
     * exclude VOIDED sales; the rows still list them (status column) for the audit.
     */
    @Transactional(readOnly = true)
    public DiscountOverrideDto discountsOverrides(LocalDate from, LocalDate to) {
        long siteId = TenantContext.requireSiteId();
        List<Sale> sales = saleRepository.findInRange(
            siteId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        Map<Long, String> custName = new HashMap<>();
        for (Customer c : customerRepository.findBySiteIdOrderByCustomerNameAsc(siteId)) {
            custName.put(c.getCustomerId(), c.getCustomerName());
        }

        List<DiscountOverrideDto.Row> rows = new ArrayList<>();
        BigDecimal totalOverride = BigDecimal.ZERO, totalDiscount = BigDecimal.ZERO, totalGiveback = BigDecimal.ZERO;
        for (Sale s : sales) {
            boolean voided = "VOIDED".equalsIgnoreCase(s.getStatus());
            String customer = s.getCustomerId() == null ? ""
                : custName.getOrDefault(s.getCustomerId(), "#" + s.getCustomerId());
            for (SaleItem si : saleItemRepository.findBySiteIdAndSalesNumberOrderByItemIdAsc(siteId, s.getSalesNumber())) {
                BigDecimal listPrice = si.getListPrice() == null ? si.getUnitCost() : si.getListPrice();
                BigDecimal charged = nz(si.getUnitCost());
                BigDecimal adj = nz(si.getAdjustment());
                BigDecimal priceOverride = listPrice.subtract(charged).multiply(nz(si.getQuantity()));
                boolean overridden = priceOverride.signum() != 0 || adj.signum() > 0;
                if (!overridden) continue;
                BigDecimal giveback = priceOverride.add(adj);
                rows.add(new DiscountOverrideDto.Row(
                    s.getCreationDate(), s.getSalesNumber(), s.getCreatedBy(), customer,
                    si.getItemDesc(), nz(si.getQuantity()), listPrice, charged,
                    priceOverride, adj, giveback, si.getOverrideReason(), si.getApprovedBy(), s.getStatus()));
                if (!voided) {
                    totalOverride = totalOverride.add(priceOverride);
                    totalDiscount = totalDiscount.add(adj);
                    totalGiveback = totalGiveback.add(giveback);
                }
            }
        }
        return new DiscountOverrideDto(from, to, rows.size(), totalOverride, totalDiscount, totalGiveback, rows);
    }

    /**
     * Reorder Suggestions: active items whose on-hand is at/below their warning or
     * critical threshold. Suggested qty brings stock up to 2× the reorder point
     * (warning, or critical when warning is unset); estimated cost uses the current
     * moving-average cost. Sorted out-of-stock → critical → low, then by name.
     */
    @Transactional(readOnly = true)
    public ReorderSuggestionDto reorderSuggestions() {
        List<ReorderSuggestionDto.Row> rows = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ItemDto it : inventorySnapshot()) {
            if (!it.active() || it.stockLevel() == ItemDto.StockLevel.OK) continue;
            BigDecimal onHand = nz(it.quantity());
            BigDecimal reorderPoint = it.warning() != null ? it.warning()
                : it.critical() != null ? it.critical() : BigDecimal.ZERO;
            // Bring stock up to 2× the reorder point; never suggest a negative qty.
            BigDecimal suggested = reorderPoint.multiply(BigDecimal.valueOf(2)).subtract(onHand)
                .max(BigDecimal.ZERO).setScale(0, java.math.RoundingMode.CEILING);
            BigDecimal unitCost = nz(it.costPrice());
            BigDecimal estCost = suggested.multiply(unitCost);
            String status = onHand.signum() <= 0 ? "OUT_OF_STOCK"
                : it.stockLevel() == ItemDto.StockLevel.CRITICAL ? "CRITICAL" : "LOW";
            rows.add(new ReorderSuggestionDto.Row(it.code(), it.name(), it.categoryName(), status,
                onHand, it.uom(), it.warning(), it.critical(), suggested, unitCost, estCost));
            totalCost = totalCost.add(estCost);
        }
        // Most urgent first: out-of-stock, then critical, then low; then by name.
        rows.sort(java.util.Comparator
            .comparingInt((ReorderSuggestionDto.Row r) -> switch (r.status()) {
                case "OUT_OF_STOCK" -> 0; case "CRITICAL" -> 1; default -> 2; })
            .thenComparing(r -> r.name() == null ? "" : r.name(), String.CASE_INSENSITIVE_ORDER));
        return new ReorderSuggestionDto(rows.size(), totalCost, rows);
    }

    /** Every stock movement in a period, normalized to a signed qty change per item. */
    @Transactional(readOnly = true)
    public InventoryMovementDto inventoryMovement(LocalDate from, LocalDate to) {
        long siteId = TenantContext.requireSiteId();
        Map<Long, String> itemLabel = new HashMap<>();
        for (ItemDto it : inventorySnapshot()) {
            itemLabel.put(it.id(), it.code() + " — " + it.name());
        }
        List<TransactionLog> logs = transactionLogRepository.findInRange(
            siteId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        List<InventoryMovementDto.Row> rows = new ArrayList<>();
        for (TransactionLog t : logs) {
            String label = itemLabel.getOrDefault(t.getItemId(), "#" + t.getItemId());
            rows.add(new InventoryMovementDto.Row(
                t.getCreationDate(), label, t.getTransactionType(),
                reference(t), signedChange(t), balanceAfter(t), t.getCreatedBy()));
        }
        return new InventoryMovementDto(from, to, rows.size(), rows);
    }

    /**
     * The signed quantity change a journal row represents. Single source of truth
     * for movement direction, shared by the movement report and as-of reconstruction.
     */
    private static BigDecimal signedChange(TransactionLog t) {
        return switch (t.getTransactionType()) {
            case TransactionLog.TYPE_STOCK_IN_GR -> dec(t.getAttribute3());                         // received qty in attr3
            case TransactionLog.TYPE_STOCK_OUT_SALE -> dec(t.getAttribute2());                      // attr2 already negative
            case TransactionLog.TYPE_STOCK_IN_VOID, TransactionLog.TYPE_STOCK_IN_REFUND -> dec(t.getAttribute2()).abs();
            case TransactionLog.TYPE_STOCK_OUT_TRANSFER -> dec(t.getAttribute2()).abs().negate();
            case TransactionLog.TYPE_STOCK_IN_TRANSFER, TransactionLog.TYPE_STOCK_IN_XFER_CANCEL -> dec(t.getAttribute2()).abs();
            default -> dec(t.getAttribute2());   // ADJUST, OPENING, EDIT — signed delta already in attr2
        };
    }

    /** Running balance after the movement, or null when the row type doesn't record one. */
    private static BigDecimal balanceAfter(TransactionLog t) {
        return switch (t.getTransactionType()) {
            case TransactionLog.TYPE_STOCK_IN_GR, TransactionLog.TYPE_STOCK_OUT_TRANSFER,
                 TransactionLog.TYPE_STOCK_IN_TRANSFER, TransactionLog.TYPE_STOCK_IN_XFER_CANCEL -> null;
            default -> dec(t.getAttribute3());
        };
    }

    private static String reference(TransactionLog t) {
        return switch (t.getTransactionType()) {
            case TransactionLog.TYPE_STOCK_IN_GR -> t.getAttribute2() != null ? t.getAttribute2() : t.getAttribute1();
            case TransactionLog.TYPE_STOCK_ADJUST, TransactionLog.TYPE_STOCK_IN_OPENING,
                 TransactionLog.TYPE_STOCK_EDIT -> t.getReason();
            default -> t.getAttribute1();
        };
    }

    /**
     * Shift History report: one row per shift (cash-reconciliation snapshot) with
     * the sales rung during its window nested underneath. Open shifts use {@code now}
     * as the window end. Sales are matched to a shift by cashier ({@code createdBy}).
     */
    @Transactional(readOnly = true)
    public List<ShiftHistoryReportDto> shiftHistory(String status, String search, LocalDate from, LocalDate to) {
        long siteId = TenantContext.requireSiteId();
        String statusU = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        String pattern = (search == null || search.isBlank()) ? null : "%" + search.trim().toLowerCase() + "%";
        LocalDateTime fromTs = from == null ? null : from.atStartOfDay();
        LocalDateTime toTs = to == null ? null : to.atTime(LocalTime.MAX);

        List<Shift> shifts = shiftRepository.searchForReport(siteId, statusU, pattern, fromTs, toTs);

        Map<Long, String> custName = new HashMap<>();
        for (Customer c : customerRepository.findBySiteIdOrderByCustomerNameAsc(siteId)) {
            custName.put(c.getCustomerId(), c.getCustomerName());
        }

        List<ShiftHistoryReportDto> data = new ArrayList<>(shifts.size());
        for (Shift s : shifts) {
            LocalDateTime windowEnd = s.getClosedAt() == null ? LocalDateTime.now() : s.getClosedAt();
            List<Sale> sales = saleRepository.findByCashierInWindow(siteId, s.getCashier(), s.getOpenedAt(), windowEnd);
            List<ShiftHistoryReportDto.SaleRow> saleRows = new ArrayList<>(sales.size());
            for (Sale sa : sales) {
                String customer = sa.getCustomerId() == null ? null : custName.get(sa.getCustomerId());
                saleRows.add(new ShiftHistoryReportDto.SaleRow(
                    sa.getSalesNumber(), sa.getCreationDate(), sa.getModeOfPayment(),
                    customer, nz(sa.getGrandTotal()), sa.getStatus()));
            }
            data.add(new ShiftHistoryReportDto(
                s.getShiftNumber(), s.getCashier(), s.getStatus(),
                s.getOpeningFloat(), s.getCountedCash(), s.getExpectedCash(), s.getCashVariance(),
                s.getCashSalesTotal(), s.getCashRefundsTotal(), s.getCashInTotal(), s.getCashOutTotal(),
                s.getNoncashGcashTotal(), s.getNoncashPaymayaTotal(), s.getNoncashBankTotal(),
                s.getNoncashChequeTotal(), s.getNoncashChargeTotal(),
                s.getSaleCount(), s.getOpenedAt(), s.getClosedAt(), s.getClosedBy(), s.getCloseNote(),
                saleRows));
        }
        return data;
    }

    private static BigDecimal dec(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
