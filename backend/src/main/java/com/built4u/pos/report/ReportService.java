package com.built4u.pos.report;

import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.item.ItemDto;
import com.built4u.pos.item.ItemService;
import com.built4u.pos.report.dto.InventoryValuationDto;
import com.built4u.pos.report.dto.SalesOverviewDto;
import com.built4u.pos.sale.Sale;
import com.built4u.pos.sale.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sales + inventory report queries (the reports that don't map to an existing list endpoint). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
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

            String mode = s.getModeOfPayment() == null ? "—" : s.getModeOfPayment();
            modeCount.computeIfAbsent(mode, k -> new long[1])[0]++;
            modeTotal.merge(mode, nz(s.getGrandTotal()), BigDecimal::add);

            LocalDate day = s.getCreationDate() == null ? from : s.getCreationDate().toLocalDate();
            dayCount.computeIfAbsent(day, k -> new long[1])[0]++;
            dayNet.merge(day, nz(s.getGrandTotal()), BigDecimal::add);
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

    @Transactional(readOnly = true)
    public InventoryValuationDto inventoryValuation() {
        List<ItemDto> items = inventorySnapshot();
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

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
