package com.built4u.pos.goodsreceipt;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.goodsreceipt.dto.CreateGoodsReceiptRequest;
import com.built4u.pos.goodsreceipt.dto.GoodsReceiptDto;
import com.built4u.pos.item.Item;
import com.built4u.pos.item.ItemRepository;
import com.built4u.pos.purchaseorder.PurchaseOrderItem;
import com.built4u.pos.purchaseorder.PurchaseOrderRepository;
import com.built4u.pos.purchaseorder.PurchaseOrderService;
import com.built4u.pos.purchaseorder.PurchaseOrderStatus;
import com.built4u.pos.transactionlog.TransactionLog;
import com.built4u.pos.transactionlog.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Records a Goods Receipt and updates inventory transactionally:
 *   1. Validate the GR against the PO (if any)
 *   2. Insert one row per received line into pos_goods_receipt
 *   3. Increment item.quantity for each line; refresh item.cost_price
 *   4. Write one STOCK_IN_GR row per line into pos_transaction_log
 *   5. Recompute PO status (APPROVED / PARTIALLY_RECEIVED / RECEIVED)
 *
 * The whole operation is one transaction — partial failures roll everything back.
 * (AP-payable auto-creation lives in a later phase and is intentionally absent.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptService {

    private final GoodsReceiptRepository grRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderService poService;
    private final ItemRepository itemRepository;
    private final TransactionLogRepository txnLogRepository;

    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> listForPo(String poNumber) {
        Long siteId = TenantContext.requireSiteId();
        List<GoodsReceiptItem> rows = grRepository.findBySiteIdAndPoNumberOrderByGrNumberAscItemIdAsc(siteId, poNumber);
        return groupByGrNumber(siteId, rows);
    }

    /**
     * Filtered goods-receipt list. {@code search} matches GR# / PO# / supplier /
     * reference (case-insensitive contains); {@code source} = "PO" (PO-linked
     * only) | "DIRECT" (no PO) | blank (all); {@code from}/{@code to} bound the
     * receipt date (inclusive, {@code yyyy-MM-dd}). Filtering is in-memory after
     * grouping — fine for this module's volume.
     */
    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> list(String search, String source, String from, String to) {
        Long siteId = TenantContext.requireSiteId();
        List<GoodsReceiptDto> all = groupByGrNumber(siteId, grRepository.findAllForSite(siteId));

        String q = search == null ? null : search.trim().toLowerCase();
        if (q != null && q.isEmpty()) q = null;
        String src = source == null ? null : source.trim().toUpperCase();
        LocalDate fromD = parseDate(from);
        LocalDate toD = parseDate(to);

        final String query = q;
        return all.stream().filter(g -> {
            if ("PO".equals(src) && g.poNumber() == null) return false;
            if ("DIRECT".equals(src) && g.poNumber() != null) return false;
            LocalDate d = g.creationDate() == null ? null : g.creationDate().toLocalDate();
            if (fromD != null && (d == null || d.isBefore(fromD))) return false;
            if (toD != null && (d == null || d.isAfter(toD))) return false;
            if (query != null) {
                String hay = (nz(g.grNumber()) + " " + nz(g.poNumber()) + " "
                    + nz(g.supplier()) + " " + nz(g.reference())).toLowerCase();
                if (!hay.contains(query)) return false;
            }
            return true;
        }).toList();
    }

    @Transactional(readOnly = true)
    public GoodsReceiptDto get(String grNumber) {
        Long siteId = TenantContext.requireSiteId();
        List<GoodsReceiptItem> rows = grRepository.findBySiteIdAndGrNumberOrderByItemIdAsc(siteId, grNumber);
        if (rows.isEmpty()) throw new NotFoundException("GR " + grNumber + " not found");
        return groupByGrNumber(siteId, rows).get(0);
    }

    @Transactional
    public GoodsReceiptDto create(CreateGoodsReceiptRequest req) {
        Long siteId = TenantContext.requireSiteId();
        if (req.lines().isEmpty()) throw new BadRequestException("GR must have at least one line");

        // Resolve PO if specified — must exist, must be APPROVED or PARTIALLY_RECEIVED.
        String poNumber = blankToNull(req.poNumber());
        Map<Long, PurchaseOrderItem> poLineByItemId = Collections.emptyMap();
        String supplier;
        if (poNumber != null) {
            List<PurchaseOrderItem> poLines = poRepository.findBySiteIdAndPoNumberOrderByItemIdAsc(siteId, poNumber);
            if (poLines.isEmpty()) throw new NotFoundException("PO " + poNumber + " not found");
            PurchaseOrderStatus poStatus = PurchaseOrderStatus.valueOf(poLines.get(0).getStatus());
            if (!poStatus.canReceive()) {
                throw new BadRequestException("Cannot receive against PO in status " + poStatus);
            }
            poLineByItemId = new HashMap<>();
            for (PurchaseOrderItem line : poLines) {
                poLineByItemId.put(line.getItemId(), line);
            }
            supplier = poLines.get(0).getSupplier();

            // Validate received qty per line does not exceed remaining qty.
            Map<Long, BigDecimal> alreadyReceived = grRepository.sumReceivedByPo(siteId, poNumber);
            for (var line : req.lines()) {
                PurchaseOrderItem poLine = poLineByItemId.get(line.itemId());
                if (poLine == null) {
                    throw new BadRequestException("Item " + line.itemId() + " is not on PO " + poNumber);
                }
                BigDecimal already = alreadyReceived.getOrDefault(line.itemId(), BigDecimal.ZERO);
                BigDecimal remaining = poLine.getQuantity().subtract(already);
                if (line.quantity().compareTo(remaining) > 0) {
                    throw new BadRequestException(
                        "Receiving qty " + line.quantity() + " exceeds remaining " + remaining +
                        " for item " + line.itemId());
                }
            }
        } else {
            // No PO → direct receipt; the supplier is whatever the user typed.
            supplier = blankToNull(req.supplier());
        }

        // Reject duplicate item_ids in this single GR.
        Set<Long> seenItems = new HashSet<>();
        for (var line : req.lines()) {
            if (!seenItems.add(line.itemId())) {
                throw new BadRequestException("Duplicate item " + line.itemId() + " in GR lines");
            }
        }

        // Load all items up front (we'll mutate them).
        Map<Long, Item> items = new HashMap<>();
        for (var line : req.lines()) {
            Item it = itemRepository.findBySiteIdAndItemId(siteId, line.itemId())
                .orElseThrow(() -> new NotFoundException("Item " + line.itemId() + " not found"));
            items.put(line.itemId(), it);
        }

        String grNumber = nextGrNumber(siteId);
        BigDecimal grand = BigDecimal.ZERO;

        for (var line : req.lines()) {
            Item item = items.get(line.itemId());
            BigDecimal unitCost;
            if (line.unitCost() != null) {
                unitCost = line.unitCost();
            } else if (poNumber != null && poLineByItemId.containsKey(line.itemId())) {
                unitCost = poLineByItemId.get(line.itemId()).getUnitPrice();  // fall back to PO price
            } else {
                unitCost = item.getCostPrice() == null ? BigDecimal.ZERO : item.getCostPrice();
            }
            BigDecimal sub = unitCost.multiply(line.quantity());
            grand = grand.add(sub);

            // 1. Insert the GR row.
            GoodsReceiptItem grRow = GoodsReceiptItem.builder()
                .siteId(siteId)
                .grNumber(grNumber)
                .itemId(line.itemId())
                .itemDesc(item.getItemDesc())
                .quantity(line.quantity())
                .uom(item.getUom())
                .reference(blankToNull(req.reference()))
                .poNumber(poNumber)
                .remarks(blankToNull(req.remarks()))
                .supPrice(unitCost)
                .subTotal(sub)
                .supplier(supplier)
                .build();
            grRepository.save(grRow);

            // 2. Bump item stock + refresh cost price.
            BigDecimal newQty = (item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity())
                .add(line.quantity());
            item.setQuantity(newQty);
            item.setCostPrice(unitCost);
            itemRepository.save(item);

            // 3. Audit row.
            TransactionLog tx = TransactionLog.builder()
                .siteId(siteId)
                .itemId(line.itemId())
                .catId(item.getCatId())
                .transactionType(TransactionLog.TYPE_STOCK_IN_GR)
                .attribute1(poNumber)
                .attribute2(grNumber)
                .attribute3(line.quantity().toPlainString())
                .attribute4(unitCost.toPlainString())
                .reason(blankToNull(req.remarks()))
                .build();
            txnLogRepository.save(tx);
        }

        // 4. Recompute PO status if applicable.
        if (poNumber != null) {
            poService.refreshStatusAfterReceiving(poNumber);
        }

        log.info("GR {} created with {} lines, grand_total={}", grNumber, req.lines().size(), grand);
        return get(grNumber);
    }

    private String nextGrNumber(Long siteId) {
        int year = LocalDate.now().getYear();
        String prefix = "GR-" + year + "-";
        String last = grRepository.findMaxGrNumberWithPrefix(siteId, prefix + "%");
        int next = 1;
        if (last != null) {
            try {
                next = Integer.parseInt(last.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return String.format("%s%04d", prefix, next);
    }

    private List<GoodsReceiptDto> groupByGrNumber(Long siteId, List<GoodsReceiptItem> rows) {
        if (rows.isEmpty()) return List.of();
        Map<String, List<GoodsReceiptItem>> byGr = new LinkedHashMap<>();
        for (GoodsReceiptItem r : rows) byGr.computeIfAbsent(r.getGrNumber(), k -> new ArrayList<>()).add(r);

        // Cache item lookups (efficient enough for one GR).
        Map<Long, Item> items = new HashMap<>();
        rows.forEach(r -> items.computeIfAbsent(r.getItemId(),
            id -> itemRepository.findBySiteIdAndItemId(siteId, id).orElse(null)));

        List<GoodsReceiptDto> out = new ArrayList<>(byGr.size());
        for (var entry : byGr.entrySet()) {
            List<GoodsReceiptItem> grRows = entry.getValue();
            GoodsReceiptItem first = grRows.get(0);
            BigDecimal grand = grRows.stream()
                .map(g -> g.getSubTotal() == null ? BigDecimal.ZERO : g.getSubTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<GoodsReceiptDto.Line> lines = grRows.stream()
                .map(g -> {
                    Item it = items.get(g.getItemId());
                    return new GoodsReceiptDto.Line(
                        g.getItemId(),
                        it == null ? null : it.getItemCode(),
                        it == null ? null : it.getItemName(),
                        g.getUom(),
                        g.getQuantity(),
                        g.getSupPrice(),
                        g.getSubTotal()
                    );
                })
                .toList();
            out.add(new GoodsReceiptDto(
                entry.getKey(),
                first.getPoNumber(),
                first.getSupplier(),
                first.getReference(),
                first.getRemarks(),
                grand,
                first.getCreationDate(),
                first.getCreatedBy(),
                lines
            ));
        }
        return out;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Lenient ISO date parse; blank/invalid → null (no filter). */
    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
