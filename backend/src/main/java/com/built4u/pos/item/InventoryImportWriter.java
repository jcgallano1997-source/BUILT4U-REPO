package com.built4u.pos.item;

import com.built4u.pos.item.InventoryImportService.ParsedRow;
import com.built4u.pos.transactionlog.TransactionLog;
import com.built4u.pos.transactionlog.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence half of the inventory import, split into its own bean so each chunk
 * runs in a fresh transaction ({@link Propagation#REQUIRES_NEW}). That keeps one
 * bad row from poisoning the whole import: when a chunk fails it is retried
 * row-by-row via {@link #saveOne}, so every good row still commits. (A caught
 * persistence error inside a single shared transaction marks it rollback-only,
 * which is why the old all-in-one-transaction importer lost everything.)
 */
@Component
@RequiredArgsConstructor
class InventoryImportWriter {

    private final ItemRepository itemRepository;
    private final TransactionLogRepository txnLogRepository;

    /** Save a whole chunk in one transaction; any error rolls the chunk back and propagates. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int[] saveChunk(Long siteId, List<ParsedRow> rows) {
        int created = 0, updated = 0;
        for (ParsedRow r : rows) {
            if (saveRow(siteId, r)) created++; else updated++;
        }
        return new int[]{created, updated};
    }

    /** Save a single row in its own transaction — the fallback used when a chunk fails. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveOne(Long siteId, ParsedRow r) {
        return saveRow(siteId, r);
    }

    /** @return true if a new item was created, false if an existing one (matched by code) was updated. */
    private boolean saveRow(Long siteId, ParsedRow r) {
        var existing = itemRepository.findBySiteIdAndItemCodeIgnoreCase(siteId, r.code());
        if (existing.isPresent()) {
            Item it = existing.get();
            BigDecimal oldQty = it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity();
            it.setItemName(r.name());
            it.setCatId(r.catId());
            it.setLocId(r.locId());
            it.setUom(r.uom());
            if (r.qty() != null) it.setQuantity(r.qty());
            if (r.sell() != null) it.setSellingPrice(r.sell());
            if (r.cost() != null) it.setCostPrice(r.cost());
            itemRepository.save(it);
            // Journal a quantity change so as-of reports stay accurate.
            if (r.qty() != null && r.qty().compareTo(oldQty) != 0) {
                logStock(siteId, it.getItemId(), r.catId(), TransactionLog.TYPE_STOCK_EDIT,
                    r.qty().subtract(oldQty), r.qty(), "Bulk import");
            }
            return false;
        }
        BigDecimal openingQty = r.qty() == null ? BigDecimal.ZERO : r.qty();
        Item newItem = itemRepository.save(Item.builder()
            .siteId(siteId).catId(r.catId()).locId(r.locId())
            .itemCode(r.code()).itemName(r.name()).uom(r.uom())
            .quantity(openingQty)
            .sellingPrice(r.sell() == null ? BigDecimal.ZERO : r.sell())
            .costPrice(r.cost() == null ? BigDecimal.ZERO : r.cost())
            .active(true).build());
        logStock(siteId, newItem.getItemId(), r.catId(), TransactionLog.TYPE_STOCK_IN_OPENING,
            openingQty, openingQty, "Opening balance (bulk import)");
        return true;
    }

    /** Append a stock-movement journal row (signed delta in attr2, resulting balance in attr3). */
    private void logStock(Long siteId, Long itemId, Long catId, String type,
                          BigDecimal delta, BigDecimal balance, String reason) {
        txnLogRepository.save(TransactionLog.builder()
            .siteId(siteId)
            .itemId(itemId)
            .catId(catId)
            .transactionType(type)
            .attribute2(delta == null ? "0" : delta.toPlainString())
            .attribute3(balance == null ? null : balance.toPlainString())
            .reason(reason)
            .build());
    }
}
