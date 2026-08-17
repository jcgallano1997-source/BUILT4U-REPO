package com.built4u.pos.item;

import com.built4u.pos.item.dto.AdjustStockRequest;
import com.built4u.pos.item.dto.CreateItemRequest;
import com.built4u.pos.item.dto.UpdateSellingPriceRequest;
import com.built4u.pos.item.dto.UpdateItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    private static final String READ_ANY =
        "hasAnyAuthority('MOD_INVENTORY','MOD_STOCKTAKE','MOD_POS','MOD_SALES'," +
        "'MOD_PURCHASE_ORDERS','MOD_GOODS_RECEIPTS'," +
        "'MOD_INVENTORY_SNAPSHOT','MOD_INVENTORY_VALUATION','MOD_INVENTORY_MOVEMENT')";

    @GetMapping
    @PreAuthorize(READ_ANY)
    public ResponseEntity<List<ItemDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "catId", required = false) Long catId,
        @RequestParam(value = "locId", required = false) Long locId,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive,
        @RequestParam(value = "stockLevel", required = false) String stockLevel
    ) {
        return ResponseEntity.ok(itemService.list(search, catId, locId, includeInactive, stockLevel));
    }

    @GetMapping("/barcode/{code}")
    @PreAuthorize(READ_ANY)
    public ResponseEntity<ItemDto> findByBarcode(@PathVariable("code") String code) {
        return ResponseEntity.ok(itemService.findByBarcode(code));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ANY)
    public ResponseEntity<ItemDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(itemService.get(id));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyAuthority('MOD_INVENTORY_ADJUST','MOD_STOCKTAKE')")
    public ResponseEntity<ItemDto> adjust(@PathVariable("id") Long id, @Valid @RequestBody AdjustStockRequest req) {
        return ResponseEntity.ok(itemService.adjustStock(id, req));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_INVENTORY_CREATE')")
    public ResponseEntity<ItemDto> create(@Valid @RequestBody CreateItemRequest req) {
        return ResponseEntity.ok(itemService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_EDIT')")
    public ResponseEntity<ItemDto> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateItemRequest req) {
        return ResponseEntity.ok(itemService.update(id, req));
    }

    /** Update just the selling price — used by the reprice-on-receive prompt. */
    @PutMapping("/{id}/selling-price")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_EDIT')")
    public ResponseEntity<ItemDto> updateSellingPrice(@PathVariable("id") Long id,
                                                      @Valid @RequestBody UpdateSellingPriceRequest req) {
        return ResponseEntity.ok(itemService.updateSellingPrice(id, req.sellingPrice()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_INVENTORY_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        itemService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
