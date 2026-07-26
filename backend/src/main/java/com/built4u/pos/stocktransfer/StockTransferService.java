package com.built4u.pos.stocktransfer;

import com.built4u.pos.category.Category;
import com.built4u.pos.category.CategoryRepository;
import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.item.Item;
import com.built4u.pos.item.ItemRepository;
import com.built4u.pos.location.Location;
import com.built4u.pos.location.LocationRepository;
import com.built4u.pos.site.Site;
import com.built4u.pos.site.SiteRepository;
import com.built4u.pos.stocktransfer.dto.CreateStockTransferRequest;
import com.built4u.pos.stocktransfer.dto.StockTransferDetailDto;
import com.built4u.pos.stocktransfer.dto.StockTransferDto;
import com.built4u.pos.stocktransfer.dto.StockTransferItemDto;
import com.built4u.pos.stocktransferpolicy.StockTransferPolicyService;
import com.built4u.pos.transactionlog.TransactionLog;
import com.built4u.pos.transactionlog.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cross-site stock transfer. Two-step flow:
 * <ol>
 *   <li><b>Ship</b> — caller at the source site (= {@code TenantContext})
 *       decrements source stock; the transfer goes IN_TRANSIT.</li>
 *   <li><b>Receive</b> — caller at the destination site increments destination
 *       stock; auto-creates the item at the destination if its code doesn't
 *       exist there. Status flips to RECEIVED.</li>
 *   <li><b>Cancel</b> — caller at the source site restores source stock while
 *       still IN_TRANSIT.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository transferItemRepository;
    private final ItemRepository itemRepository;
    private final SiteRepository siteRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final TransactionLogRepository txnLogRepository;
    private final StockTransferPolicyService policyService;

    /** Ship — source-site initiated. Decrements source stock; status IN_TRANSIT. */
    @Transactional
    public StockTransferDetailDto create(CreateStockTransferRequest req) {
        Long sourceSiteId = TenantContext.requireSiteId();
        Long destSiteId = req.destSiteId();
        if (destSiteId.equals(sourceSiteId)) {
            throw new BadRequestException("Destination site must differ from the current site.");
        }
        Site destSite = siteRepository.findById(destSiteId)
            .orElseThrow(() -> new NotFoundException("Destination site " + destSiteId + " not found"));
        // Enforce the admin-configured (source → dest) allow-list (if any).
        policyService.requireAllowed(sourceSiteId, destSiteId);
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new BadRequestException("Transfer must have at least one line");
        }
        Set<Long> seen = new HashSet<>();
        for (var line : req.lines()) {
            if (!seen.add(line.itemId())) {
                throw new BadRequestException("Duplicate item " + line.itemId() + " in transfer lines");
            }
        }

        // Lock source items in ascending itemId order (deadlock-safe — same pattern as SaleService).
        var orderedLines = req.lines().stream()
            .sorted(Comparator.comparingLong(CreateStockTransferRequest.Line::itemId))
            .toList();
        List<Item> lockedItems = new ArrayList<>(orderedLines.size());
        for (var line : orderedLines) {
            Item item = itemRepository.findBySiteIdAndItemIdForUpdate(sourceSiteId, line.itemId())
                .orElseThrow(() -> new NotFoundException("Item " + line.itemId() + " not found at source"));
            BigDecimal have = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            if (have.compareTo(line.quantity()) < 0) {
                throw new BadRequestException(
                    "Insufficient stock for " + item.getItemCode() + ": have "
                    + have.toPlainString() + ", need " + line.quantity().toPlainString());
            }
            lockedItems.add(item);
        }

        String transferNumber = nextTransferNumber(sourceSiteId);

        StockTransfer header = StockTransfer.builder()
            .sourceSiteId(sourceSiteId)
            .destSiteId(destSiteId)
            .transferNumber(transferNumber)
            .status(StockTransferStatus.IN_TRANSIT.name())
            .remarks(blankToNull(req.remarks()))
            .shippedAt(LocalDateTime.now())
            .sentBy(currentUsername())
            .build();
        transferRepository.save(header);

        List<StockTransferItem> savedLines = new ArrayList<>(orderedLines.size());
        for (int i = 0; i < orderedLines.size(); i++) {
            var line = orderedLines.get(i);
            Item item = lockedItems.get(i);
            BigDecimal unitCost = item.getCostPrice() == null ? BigDecimal.ZERO : item.getCostPrice();

            item.setQuantity(item.getQuantity().subtract(line.quantity()));
            itemRepository.save(item);

            StockTransferItem stiRow = StockTransferItem.builder()
                .transferId(header.getId())
                .sourceItemId(item.getItemId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .uom(item.getUom())
                .quantity(line.quantity())
                .unitCost(unitCost)
                .build();
            transferItemRepository.save(stiRow);
            savedLines.add(stiRow);

            txnLogRepository.save(TransactionLog.builder()
                .siteId(sourceSiteId)
                .itemId(item.getItemId())
                .catId(item.getCatId())
                .transactionType(TransactionLog.TYPE_STOCK_OUT_TRANSFER)
                .attribute1(transferNumber)
                .attribute2(line.quantity().toPlainString())
                .attribute3(unitCost.toPlainString())
                .attribute4("dest:" + destSiteId)
                .build());
        }

        log.info("Stock transfer {} shipped from site {} to site {} ({} lines)",
            transferNumber, sourceSiteId, destSiteId, savedLines.size());
        return toDetail(header, savedLines, destSite.getName());
    }

    /**
     * Receive — destination-site initiated. Increments destination stock per
     * line; auto-creates the item at the destination if its code doesn't exist
     * there yet (cloned from the line snapshot: same code/name/uom, cost =
     * source unit cost, qty starts at 0; cat/loc = destination's first active).
     */
    @Transactional
    public StockTransferDetailDto receive(String transferNumber) {
        Long destSiteId = TenantContext.requireSiteId();
        StockTransfer header = transferRepository.findByTransferNumberForUpdate(transferNumber)
            .orElseThrow(() -> new NotFoundException("Transfer " + transferNumber + " not found"));
        if (!destSiteId.equals(header.getDestSiteId())) {
            throw new BadRequestException(
                "You must be logged in to the destination site to receive this transfer.");
        }
        if (!StockTransferStatus.IN_TRANSIT.name().equals(header.getStatus())) {
            throw new BadRequestException(
                "Transfer " + transferNumber + " is " + header.getStatus()
                + " — only IN_TRANSIT transfers can be received.");
        }

        var lines = transferItemRepository.findByTransferIdOrderByIdAsc(header.getId());
        if (lines.isEmpty()) {
            throw new BadRequestException("Transfer has no lines to receive.");
        }

        // Resolve a fallback cat/loc at dest (needed only if we auto-create any items).
        Long fallbackCatId = null;
        Long fallbackLocId = null;
        for (var line : lines) {
            var existing = itemRepository.findBySiteIdAndItemCodeIgnoreCase(destSiteId, line.getItemCode());
            Long destItemId;
            if (existing.isPresent()) {
                destItemId = existing.get().getItemId();
            } else {
                if (fallbackCatId == null) {
                    fallbackCatId = categoryRepository.findBySiteIdOrderByCategoryNameAsc(destSiteId).stream()
                        .filter(c -> Boolean.TRUE.equals(c.getActive()))
                        .map(Category::getCatId).findFirst()
                        .orElseThrow(() -> new BadRequestException(
                            "Cannot auto-create items at destination — no active categories there. "
                            + "Add at least one Category at the destination site first."));
                    fallbackLocId = locationRepository.findBySiteIdOrderByLocationAsc(destSiteId).stream()
                        .filter(l -> Boolean.TRUE.equals(l.getActive()))
                        .map(Location::getLocId).findFirst()
                        .orElseThrow(() -> new BadRequestException(
                            "Cannot auto-create items at destination — no active locations there. "
                            + "Add at least one Location at the destination site first."));
                }
                Item created = Item.builder()
                    .siteId(destSiteId)
                    .catId(fallbackCatId)
                    .locId(fallbackLocId)
                    .itemCode(line.getItemCode())
                    .itemName(line.getItemName() == null ? line.getItemCode() : line.getItemName())
                    .uom(line.getUom() == null ? "" : line.getUom())
                    .quantity(BigDecimal.ZERO)
                    .sellingPrice(line.getUnitCost() == null ? BigDecimal.ZERO : line.getUnitCost())
                    .costPrice(line.getUnitCost() == null ? BigDecimal.ZERO : line.getUnitCost())
                    .active(true)
                    .build();
                // Composite @IdClass ⇒ Spring Data merges; the generated itemId
                // lands on the RETURNED instance, not the local one.
                Item saved = itemRepository.save(created);
                destItemId = saved.getItemId();
            }

            Item destItem = itemRepository.findBySiteIdAndItemIdForUpdate(destSiteId, destItemId)
                .orElseThrow(() -> new BadRequestException(
                    "Destination item " + destItemId + " disappeared mid-receive — retry."));
            BigDecimal newQty = (destItem.getQuantity() == null ? BigDecimal.ZERO : destItem.getQuantity())
                .add(line.getQuantity());
            destItem.setQuantity(newQty);
            itemRepository.save(destItem);

            txnLogRepository.save(TransactionLog.builder()
                .siteId(destSiteId)
                .itemId(destItem.getItemId())
                .catId(destItem.getCatId())
                .transactionType(TransactionLog.TYPE_STOCK_IN_TRANSFER)
                .attribute1(transferNumber)
                .attribute2(line.getQuantity().toPlainString())
                .attribute3((line.getUnitCost() == null ? BigDecimal.ZERO : line.getUnitCost()).toPlainString())
                .attribute4("src:" + header.getSourceSiteId())
                .build());
        }

        header.setStatus(StockTransferStatus.RECEIVED.name());
        header.setReceivedAt(LocalDateTime.now());
        header.setReceivedBy(currentUsername());
        transferRepository.save(header);

        log.info("Stock transfer {} received at site {} ({} lines)",
            transferNumber, destSiteId, lines.size());
        return toDetail(header, lines, null);
    }

    /**
     * Cancel — source-site initiated, only while still IN_TRANSIT. Restores
     * source stock per line and flips the header to CANCELLED.
     */
    @Transactional
    public StockTransferDetailDto cancel(String transferNumber) {
        Long sourceSiteId = TenantContext.requireSiteId();
        StockTransfer header = transferRepository.findByTransferNumberForUpdate(transferNumber)
            .orElseThrow(() -> new NotFoundException("Transfer " + transferNumber + " not found"));
        if (!sourceSiteId.equals(header.getSourceSiteId())) {
            throw new BadRequestException("Only the source site can cancel this transfer.");
        }
        if (!StockTransferStatus.IN_TRANSIT.name().equals(header.getStatus())) {
            throw new BadRequestException(
                "Transfer " + transferNumber + " is " + header.getStatus()
                + " — only IN_TRANSIT transfers can be cancelled.");
        }

        var lines = transferItemRepository.findByTransferIdOrderByIdAsc(header.getId()).stream()
            .sorted(Comparator.comparingLong(StockTransferItem::getSourceItemId))
            .toList();

        for (var line : lines) {
            Item item = itemRepository
                .findBySiteIdAndItemIdForUpdate(sourceSiteId, line.getSourceItemId())
                .orElseThrow(() -> new BadRequestException(
                    "Source item " + line.getSourceItemId() + " disappeared — cannot restore."));
            BigDecimal newQty = (item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity())
                .add(line.getQuantity());
            item.setQuantity(newQty);
            itemRepository.save(item);

            txnLogRepository.save(TransactionLog.builder()
                .siteId(sourceSiteId)
                .itemId(item.getItemId())
                .catId(item.getCatId())
                .transactionType(TransactionLog.TYPE_STOCK_IN_XFER_CANCEL)
                .attribute1(transferNumber)
                .attribute2(line.getQuantity().toPlainString())
                .attribute3((line.getUnitCost() == null ? BigDecimal.ZERO : line.getUnitCost()).toPlainString())
                .attribute4("dest:" + header.getDestSiteId())
                .build());
        }

        header.setStatus(StockTransferStatus.CANCELLED.name());
        header.setCancelledAt(LocalDateTime.now());
        header.setCancelledBy(currentUsername());
        transferRepository.save(header);

        log.info("Stock transfer {} cancelled at site {} ({} lines restored)",
            transferNumber, sourceSiteId, lines.size());
        return toDetail(header, lines, null);
    }

    /** Filtered, paged list (transfers where current site is source AND/OR dest). */
    @Transactional(readOnly = true)
    public Page<StockTransferDto> list(String status, String direction, String search,
                                       String from, String to, Pageable pageable) {
        long siteId = TenantContext.requireSiteId();
        String dir = (direction == null || direction.isBlank()) ? null : direction.trim().toUpperCase();
        if (dir != null && !"OUTBOUND".equals(dir) && !"INBOUND".equals(dir)) dir = null;
        String pattern = (search == null || search.isBlank()) ? null
            : "%" + search.trim().toLowerCase() + "%";
        LocalDateTime fromTs = parseDayStart(from);
        LocalDateTime toTs = parseDayEnd(to);
        String statusU = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();

        return transferRepository.search(siteId, dir, statusU, pattern, fromTs, toTs, pageable)
            .map(t -> StockTransferDto.from(t,
                siteRepository.findById(t.getSourceSiteId()).map(Site::getName).orElse("#" + t.getSourceSiteId()),
                siteRepository.findById(t.getDestSiteId()).map(Site::getName).orElse("#" + t.getDestSiteId()),
                transferItemRepository.findByTransferIdOrderByIdAsc(t.getId()).size()));
    }

    @Transactional(readOnly = true)
    public StockTransferDetailDto get(String transferNumber) {
        long siteId = TenantContext.requireSiteId();
        StockTransfer t = transferRepository.findByTransferNumber(transferNumber)
            .orElseThrow(() -> new NotFoundException("Transfer " + transferNumber + " not found"));
        if (t.getSourceSiteId() != siteId && t.getDestSiteId() != siteId) {
            throw new NotFoundException("Transfer " + transferNumber + " not found");
        }
        var items = transferItemRepository.findByTransferIdOrderByIdAsc(t.getId());
        return toDetail(t, items, null);
    }

    /**
     * Active sites other than the current one (for the destination picker),
     * filtered by the Stock Transfer Policy. If the policy is OPEN, every other
     * active site is returned.
     */
    @Transactional(readOnly = true)
    public List<SiteOption> listAvailableDestinations() {
        Long current = TenantContext.requireSiteId();
        var candidates = siteRepository.findAllByOrderByCodeAsc().stream()
            .filter(s -> !current.equals(s.getId()) && "Y".equalsIgnoreCase(s.getActive()))
            .sorted(Comparator.comparing(Site::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
        if (!policyService.enforced()) {
            return candidates.stream().map(s -> new SiteOption(s.getId(), s.getCode(), s.getName())).toList();
        }
        var allowedIds = new HashSet<>(policyService.filterAllowedDests(current,
            candidates.stream().map(Site::getId).toList()));
        return candidates.stream()
            .filter(s -> allowedIds.contains(s.getId()))
            .map(s -> new SiteOption(s.getId(), s.getCode(), s.getName()))
            .toList();
    }

    /** Picker option for the destination-site dropdown. */
    public record SiteOption(Long id, String code, String name) {}

    // ── helpers ─────────────────────────────────────────────────────────────

    private static LocalDateTime parseDayStart(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atStartOfDay();
    }
    private static LocalDateTime parseDayEnd(String s) {
        LocalDate d = parseDate(s);
        return d == null ? null : d.atTime(java.time.LocalTime.MAX);
    }
    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (java.time.format.DateTimeParseException e) { return null; }
    }

    private String nextTransferNumber(Long sourceSiteId) {
        int year = LocalDate.now().getYear();
        String prefix = "ST-" + year + "-";
        String last = transferRepository.findMaxTransferNumberWithPrefix(sourceSiteId, prefix + "%");
        int next = 1;
        if (last != null) {
            try {
                next = Integer.parseInt(last.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                // keep default 1
            }
        }
        return String.format("%s%04d", prefix, next);
    }

    StockTransferDetailDto toDetail(StockTransfer header, List<StockTransferItem> items, String destSiteName) {
        String sourceName = siteRepository.findById(header.getSourceSiteId())
            .map(Site::getName).orElse("#" + header.getSourceSiteId());
        if (destSiteName == null) {
            destSiteName = siteRepository.findById(header.getDestSiteId())
                .map(Site::getName).orElse("#" + header.getDestSiteId());
        }
        var dto = StockTransferDto.from(header, sourceName, destSiteName, items.size());
        var lines = items.stream().map(StockTransferItemDto::from).toList();
        return new StockTransferDetailDto(dto, lines);
    }

    static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            return "SYSTEM";
        }
        return a.getName();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
