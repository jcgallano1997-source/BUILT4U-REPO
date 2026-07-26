package com.built4u.pos.goodsreceipt;

import com.built4u.pos.goodsreceipt.dto.CreateGoodsReceiptRequest;
import com.built4u.pos.goodsreceipt.dto.GoodsReceiptDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_GOODS_RECEIPTS','MOD_PURCHASE_ORDERS')")
    public ResponseEntity<List<GoodsReceiptDto>> list(
        @RequestParam(value = "poNumber", required = false) String poNumber,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "from", required = false) String from,
        @RequestParam(value = "to", required = false) String to
    ) {
        List<GoodsReceiptDto> data = (poNumber == null || poNumber.isBlank())
            ? goodsReceiptService.list(search, source, from, to)
            : goodsReceiptService.listForPo(poNumber);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{grNumber}")
    @PreAuthorize("hasAnyAuthority('MOD_GOODS_RECEIPTS','MOD_PURCHASE_ORDERS')")
    public ResponseEntity<GoodsReceiptDto> get(@PathVariable("grNumber") String grNumber) {
        return ResponseEntity.ok(goodsReceiptService.get(grNumber));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_GOODS_RECEIPTS')")
    public ResponseEntity<GoodsReceiptDto> create(@Valid @RequestBody CreateGoodsReceiptRequest req) {
        return ResponseEntity.ok(goodsReceiptService.create(req));
    }
}
