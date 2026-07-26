package com.built4u.pos.item;

import com.built4u.pos.category.CategoryRepository;
import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.item.dto.AdjustStockRequest;
import com.built4u.pos.item.dto.CreateItemRequest;
import com.built4u.pos.item.dto.UpdateItemRequest;
import com.built4u.pos.location.LocationRepository;
import com.built4u.pos.transactionlog.TransactionLog;
import com.built4u.pos.transactionlog.TransactionLogRepository;
import com.built4u.pos.uom.UomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final UomRepository uomRepository;
    private final TransactionLogRepository txnLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /** Flush pending writes + detach so the follow-up read loads the @ManyToOne
     *  category/location joins fresh (a just-persisted entity has them null). */
    private ItemDto reloadDto(Long itemId) {
        entityManager.flush();
        entityManager.clear();
        return get(itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> list(String search, Long catId, Long locId, boolean includeInactive, String stockLevel) {
        Long siteId = TenantContext.requireSiteId();
        String pattern = search == null || search.isBlank() ? "%" : "%" + search.toLowerCase().trim() + "%";
        String level = normalizeStockLevel(stockLevel);
        return itemRepository.search(siteId, pattern, catId, locId, includeInactive, level).stream()
            .map(ItemDto::from).toList();
    }

    private static String normalizeStockLevel(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().toUpperCase();
        return switch (v) {
            case "OK", "WARNING", "CRITICAL" -> v;
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public ItemDto get(Long itemId) {
        Long siteId = TenantContext.requireSiteId();
        return itemRepository.findBySiteIdAndItemId(siteId, itemId)
            .map(ItemDto::from)
            .orElseThrow(() -> new NotFoundException("Item " + itemId + " not found"));
    }

    @Transactional
    public ItemDto create(CreateItemRequest req) {
        Long siteId = TenantContext.requireSiteId();
        validateRefs(siteId, req.catId(), req.locId(), req.uom());
        validateThresholds(req.warning(), req.critical());

        String code = req.code().trim();
        if (itemRepository.existsByCode(siteId, code, null)) {
            throw new ConflictException("Item code '" + code + "' is already in use at this site");
        }
        if (req.barcodeId() != null && itemRepository.existsByBarcode(siteId, req.barcodeId(), null)) {
            throw new ConflictException("Barcode '" + req.barcodeId() + "' is already in use at this site");
        }

        Item entity = Item.builder()
            .siteId(siteId)
            .catId(req.catId())
            .locId(req.locId())
            .itemCode(code)
            .itemName(req.name().trim())
            .itemDesc(blankToNull(req.description()))
            .uom(req.uom().trim())
            .quantity(req.quantity())
            .sellingPrice(req.sellingPrice())
            .costPrice(req.costPrice())
            .warning(req.warning())
            .critical(req.critical())
            .barcodeId(req.barcodeId())
            .active(true)
            .build();
        Item saved = itemRepository.save(entity);
        return reloadDto(saved.getItemId());   // re-read so @ManyToOne joins populate for the DTO
    }

    @Transactional
    public ItemDto update(Long itemId, UpdateItemRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Item i = itemRepository.findBySiteIdAndItemId(siteId, itemId)
            .orElseThrow(() -> new NotFoundException("Item " + itemId + " not found"));

        validateRefs(siteId, req.catId(), req.locId(), req.uom());
        validateThresholds(req.warning(), req.critical());

        String code = req.code().trim();
        if (itemRepository.existsByCode(siteId, code, itemId)) {
            throw new ConflictException("Item code '" + code + "' is already in use at this site");
        }
        if (req.barcodeId() != null && itemRepository.existsByBarcode(siteId, req.barcodeId(), itemId)) {
            throw new ConflictException("Barcode '" + req.barcodeId() + "' is already in use at this site");
        }

        i.setCatId(req.catId());
        i.setLocId(req.locId());
        i.setItemCode(code);
        i.setItemName(req.name().trim());
        i.setItemDesc(blankToNull(req.description()));
        i.setUom(req.uom().trim());
        i.setQuantity(req.quantity());
        i.setSellingPrice(req.sellingPrice());
        i.setCostPrice(req.costPrice());
        i.setWarning(req.warning());
        i.setCritical(req.critical());
        i.setBarcodeId(req.barcodeId());
        i.setActive(Boolean.TRUE.equals(req.active()));

        itemRepository.save(i);
        return reloadDto(itemId);
    }

    @Transactional
    public void softDelete(Long itemId) {
        Long siteId = TenantContext.requireSiteId();
        Item i = itemRepository.findBySiteIdAndItemId(siteId, itemId)
            .orElseThrow(() -> new NotFoundException("Item " + itemId + " not found"));
        i.setActive(false);
        itemRepository.save(i);
    }

    @Transactional(readOnly = true)
    public ItemDto findByBarcode(String barcode) {
        Long siteId = TenantContext.requireSiteId();
        Long parsed;
        try {
            parsed = Long.parseLong(barcode.trim());
        } catch (NumberFormatException e) {
            throw new NotFoundException("Item with barcode '" + barcode + "' not found");
        }
        return itemRepository.findBySiteIdAndBarcodeIdAndActive(siteId, parsed)
            .map(ItemDto::from)
            .orElseThrow(() -> new NotFoundException("Item with barcode '" + barcode + "' not found"));
    }

    @Transactional
    public ItemDto adjustStock(Long itemId, AdjustStockRequest req) {
        Long siteId = TenantContext.requireSiteId();
        BigDecimal delta = req.delta();
        if (delta.signum() == 0) {
            throw new BadRequestException("Adjustment delta must be non-zero");
        }
        Item item = itemRepository.findBySiteIdAndItemIdForUpdate(siteId, itemId)
            .orElseThrow(() -> new NotFoundException("Item " + itemId + " not found"));
        BigDecimal current = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal newQty = current.add(delta);
        if (newQty.signum() < 0) {
            throw new BadRequestException(
                "Adjustment would result in negative stock (have " + current.toPlainString() +
                ", delta " + delta.toPlainString() + ")");
        }
        item.setQuantity(newQty);
        itemRepository.save(item);

        TransactionLog tx = TransactionLog.builder()
            .siteId(siteId)
            .itemId(itemId)
            .catId(item.getCatId())
            .transactionType(TransactionLog.TYPE_STOCK_ADJUST)
            .attribute1(req.reason().trim())
            .attribute2(delta.toPlainString())
            .attribute3(newQty.toPlainString())
            .reason(req.reason().trim())
            .build();
        txnLogRepository.save(tx);

        return get(itemId);
    }

    private void validateRefs(Long siteId, Long catId, Long locId, String uom) {
        categoryRepository.findBySiteIdAndCatId(siteId, catId)
            .filter(c -> Boolean.TRUE.equals(c.getActive()))
            .orElseThrow(() -> new NotFoundException("Category " + catId + " not found or inactive"));
        locationRepository.findBySiteIdAndLocId(siteId, locId)
            .filter(l -> Boolean.TRUE.equals(l.getActive()))
            .orElseThrow(() -> new NotFoundException("Location " + locId + " not found or inactive"));
        uomRepository.findBySiteIdAndUom(siteId, uom.trim())
            .filter(u -> Boolean.TRUE.equals(u.getActive()))
            .orElseThrow(() -> new NotFoundException("UOM '" + uom + "' not found or inactive"));
    }

    private void validateThresholds(BigDecimal warning, BigDecimal critical) {
        if (warning != null && critical != null && critical.compareTo(warning) > 0) {
            throw new BadRequestException("Critical threshold must be <= warning threshold");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
