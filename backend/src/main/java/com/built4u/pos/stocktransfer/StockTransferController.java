package com.built4u.pos.stocktransfer;

import com.built4u.pos.stocktransfer.dto.CreateStockTransferRequest;
import com.built4u.pos.stocktransfer.dto.StockTransferDetailDto;
import com.built4u.pos.stocktransfer.dto.StockTransferDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cross-site Stock Transfers (gated by {@code MOD_STOCK_TRANSFER}). */
@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_STOCK_TRANSFER')")
public class StockTransferController {

    private final StockTransferService service;

    @GetMapping
    public ResponseEntity<Page<StockTransferDto>> list(
        @RequestParam(value = "status",    required = false) String status,
        @RequestParam(value = "direction", required = false) String direction,
        @RequestParam(value = "search",    required = false) String search,
        @RequestParam(value = "from",      required = false) String from,
        @RequestParam(value = "to",        required = false) String to,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(status, direction, search, from, to, pageable));
    }

    /** Active sites other than the current one — for the destination dropdown. */
    @GetMapping("/destinations")
    public ResponseEntity<List<StockTransferService.SiteOption>> destinations() {
        return ResponseEntity.ok(service.listAvailableDestinations());
    }

    @GetMapping("/{transferNumber}")
    public ResponseEntity<StockTransferDetailDto> get(@PathVariable("transferNumber") String transferNumber) {
        return ResponseEntity.ok(service.get(transferNumber));
    }

    /** Ship — current site is source; decrements source stock; status IN_TRANSIT. */
    @PostMapping
    public ResponseEntity<StockTransferDetailDto> create(@Valid @RequestBody CreateStockTransferRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    /** Receive at destination — increments destination stock; auto-creates missing items. */
    @PostMapping("/{transferNumber}/receive")
    public ResponseEntity<StockTransferDetailDto> receive(@PathVariable("transferNumber") String transferNumber) {
        return ResponseEntity.ok(service.receive(transferNumber));
    }

    /** Cancel at source — restores source stock; only while IN_TRANSIT. */
    @PostMapping("/{transferNumber}/cancel")
    public ResponseEntity<StockTransferDetailDto> cancel(@PathVariable("transferNumber") String transferNumber) {
        return ResponseEntity.ok(service.cancel(transferNumber));
    }
}
