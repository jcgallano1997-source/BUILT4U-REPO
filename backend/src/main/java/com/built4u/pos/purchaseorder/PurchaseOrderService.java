package com.built4u.pos.purchaseorder;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.goodsreceipt.GoodsReceiptRepository;
import com.built4u.pos.item.Item;
import com.built4u.pos.item.ItemRepository;
import com.built4u.pos.poapprover.PoApproverService;
import com.built4u.pos.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderDto;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderLineDto;
import com.built4u.pos.purchaseorder.dto.PurchaseOrderSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Purchase orders — draft → approved → (partially) received → done, plus
 * per-creator approver routing. Stored one row per line (header fields
 * denormalized); the "header" DTO is aggregated from the line rows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final GoodsReceiptRepository grRepository;
    private final ItemRepository itemRepository;
    private final PoApprovalRepository poApprovalRepository;
    private final PoApproverService poApproverService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderSummaryDto> list(String status, String supplier, String from, String to) {
        Long siteId = TenantContext.requireSiteId();
        String supplierPattern = (supplier == null || supplier.isBlank())
            ? null
            : "%" + supplier.toLowerCase() + "%";
        List<String> poNumbers = poRepository.findPoNumbers(siteId, status, supplier, supplierPattern);
        LocalDate fromD = parseDate(from);
        LocalDate toD = parseDate(to);

        List<PurchaseOrderSummaryDto> result = new ArrayList<>(poNumbers.size());
        for (String poNumber : poNumbers) {
            List<PurchaseOrderItem> lines = poRepository.findBySiteIdAndPoNumberOrderByItemIdAsc(siteId, poNumber);
            if (lines.isEmpty()) continue;
            PurchaseOrderItem first = lines.get(0);
            LocalDate d = first.getCreationDate() == null ? null : first.getCreationDate().toLocalDate();
            if (fromD != null && (d == null || d.isBefore(fromD))) continue;
            if (toD != null && (d == null || d.isAfter(toD))) continue;
            BigDecimal grand = lines.stream()
                .map(l -> l.getSubTotal() == null ? BigDecimal.ZERO : l.getSubTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new PurchaseOrderSummaryDto(
                poNumber,
                first.getSupplier(),
                first.getDeliveryDate(),
                first.getStatus(),
                grand,
                lines.size(),
                first.getCreationDate(),
                first.getCreatedBy()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto get(String poNumber) {
        Long siteId = TenantContext.requireSiteId();
        List<PurchaseOrderItem> lines = poRepository.findBySiteIdAndPoNumberOrderByItemIdAsc(siteId, poNumber);
        if (lines.isEmpty()) throw new NotFoundException("Purchase order " + poNumber + " not found");

        Map<Long, BigDecimal> received = grRepository.sumReceivedByPo(siteId, poNumber);
        Map<Long, Item> items = loadItemsByIds(siteId, lines.stream().map(PurchaseOrderItem::getItemId).toList());

        PurchaseOrderItem first = lines.get(0);
        BigDecimal grand = BigDecimal.ZERO;
        List<PurchaseOrderLineDto> lineDtos = new ArrayList<>(lines.size());
        for (PurchaseOrderItem line : lines) {
            BigDecimal sub = line.getSubTotal() == null ? BigDecimal.ZERO : line.getSubTotal();
            grand = grand.add(sub);
            BigDecimal recv = received.getOrDefault(line.getItemId(), BigDecimal.ZERO);
            BigDecimal remaining = line.getQuantity().subtract(recv);
            Item item = items.get(line.getItemId());
            lineDtos.add(new PurchaseOrderLineDto(
                line.getItemId(),
                item == null ? null : item.getItemCode(),
                item == null ? null : item.getItemName(),
                line.getItemDesc(),
                line.getUom(),
                line.getQuantity(),
                recv,
                remaining,
                line.getUnitPrice(),
                sub
            ));
        }

        var approval = poApprovalRepository.findBySiteIdAndPoNumber(siteId, poNumber);

        // UX hint for the detail page (Approve button visibility). True only when
        // the PO is DRAFT AND the current user can flip it to APPROVED (designated
        // approver or ADMIN). setStatus() enforces the same check authoritatively.
        boolean canApprove = false;
        if (PurchaseOrderStatus.DRAFT.name().equals(first.getStatus())) {
            String me = PoApproverService.currentUsername();
            boolean isAdmin = PoApproverService.currentUserIsAdmin();
            canApprove = isAdmin || poApproverService.isApproverFor(me, first.getCreatedBy());
        }

        return new PurchaseOrderDto(
            poNumber,
            first.getSupplier(),
            first.getDeliveryDate(),
            first.getRemarks(),
            first.getStatus(),
            grand,
            first.getCreationDate(),
            first.getCreatedBy(),
            approval.map(PoApproval::getApprovedAt).orElse(null),
            approval.map(PoApproval::getApprovedBy).orElse(null),
            approval.map(PoApproval::isAutoApproved).orElse(false),
            canApprove,
            lineDtos
        );
    }

    /**
     * DRAFT POs whose creator routes to the current user (or all of them, for
     * ADMIN). Backs the "Pending my approval" filter on the PO list page.
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderSummaryDto> listPendingMyApproval() {
        String me = PoApproverService.currentUsername();
        List<PurchaseOrderSummaryDto> drafts = list(PurchaseOrderStatus.DRAFT.name(), null, null, null);
        if (PoApproverService.currentUserIsAdmin()) return drafts;
        Set<String> mine = new HashSet<>(poApproverService.creatorsRoutingTo(me));
        return drafts.stream()
            .filter(po -> po.createdBy() != null && mine.contains(po.createdBy()))
            .toList();
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

    @Transactional
    public PurchaseOrderDto create(CreatePurchaseOrderRequest req) {
        Long siteId = TenantContext.requireSiteId();
        if (req.lines().isEmpty()) throw new BadRequestException("PO must have at least one line");

        // Reject duplicate item_ids in the same PO (the composite PK would catch
        // it, but this error is clearer).
        Set<Long> seen = new HashSet<>();
        for (var l : req.lines()) {
            if (!seen.add(l.itemId())) {
                throw new BadRequestException("Duplicate item " + l.itemId() + " in PO lines");
            }
        }

        Map<Long, Item> items = loadItemsByIds(siteId,
            req.lines().stream().map(CreatePurchaseOrderRequest.Line::itemId).toList());
        for (var l : req.lines()) {
            if (!items.containsKey(l.itemId())) {
                throw new NotFoundException("Item " + l.itemId() + " not found");
            }
        }

        String poNumber = nextPoNumber(siteId);
        BigDecimal grand = req.lines().stream()
            .map(l -> l.unitPrice().multiply(l.quantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Resolve approver. If the creator has no mapping (or the mapped approver
        // is inactive), auto-approve. Otherwise the PO starts DRAFT and the
        // designated approver clicks Approve later.
        String creator = PoApproverService.currentUsername();
        boolean autoApprove = poApproverService.resolveApproverFor(creator).isEmpty();
        String initialStatus = (autoApprove ? PurchaseOrderStatus.APPROVED : PurchaseOrderStatus.DRAFT).name();

        for (var l : req.lines()) {
            Item item = items.get(l.itemId());
            BigDecimal sub = l.unitPrice().multiply(l.quantity());
            PurchaseOrderItem row = PurchaseOrderItem.builder()
                .siteId(siteId)
                .poNumber(poNumber)
                .itemId(l.itemId())
                .itemDesc(item.getItemDesc())
                .uom(item.getUom())
                .quantity(l.quantity())
                .unitPrice(l.unitPrice())
                .subTotal(sub)
                .grandTotal(grand)
                .supplier(req.supplier().trim())
                .deliveryDate(blankToNull(req.deliveryDate()))
                .remarks(blankToNull(req.remarks()))
                .status(initialStatus)
                .build();
            poRepository.save(row);
        }

        // Stamp the approval sidecar if auto-approved.
        if (autoApprove) {
            poApprovalRepository.save(PoApproval.builder()
                .siteId(siteId)
                .poNumber(poNumber)
                .approvedAt(LocalDateTime.now())
                .approvedBy(creator)
                .autoApproved("Y")
                .build());
        }
        log.info("PO {} created with {} lines, grand_total={}, status={}{}",
            poNumber, req.lines().size(), grand, initialStatus,
            autoApprove ? " (auto-approved)" : "");
        return get(poNumber);
    }

    @Transactional
    public PurchaseOrderDto setStatus(String poNumber, PurchaseOrderStatus targetStatus) {
        Long siteId = TenantContext.requireSiteId();
        List<PurchaseOrderItem> lines = poRepository.findBySiteIdAndPoNumberOrderByItemIdAsc(siteId, poNumber);
        if (lines.isEmpty()) throw new NotFoundException("Purchase order " + poNumber + " not found");

        PurchaseOrderStatus current = PurchaseOrderStatus.valueOf(lines.get(0).getStatus());
        if (!isAllowedTransition(current, targetStatus)) {
            throw new BadRequestException("Cannot transition PO from " + current + " to " + targetStatus);
        }

        // Approve is gated to the designated approver (or ADMIN). Other status
        // transitions keep the MOD-only gate enforced at the controller layer.
        String currentUser = PoApproverService.currentUsername();
        if (targetStatus == PurchaseOrderStatus.APPROVED) {
            String creator = lines.get(0).getCreatedBy();
            boolean isAdmin = PoApproverService.currentUserIsAdmin();
            boolean isDesignated = poApproverService.isApproverFor(currentUser, creator);
            if (!isAdmin && !isDesignated) {
                throw new BadRequestException(
                    "Only the designated approver for " + creator + " (or an ADMIN) may approve this PO.");
            }
        }

        for (PurchaseOrderItem line : lines) {
            line.setStatus(targetStatus.name());
            poRepository.save(line);
        }

        // Stamp the approval sidecar on transition to APPROVED.
        if (targetStatus == PurchaseOrderStatus.APPROVED) {
            poApprovalRepository.save(PoApproval.builder()
                .siteId(siteId)
                .poNumber(poNumber)
                .approvedAt(LocalDateTime.now())
                .approvedBy(currentUser)
                .autoApproved("N")
                .build());
        }
        return get(poNumber);
    }

    /** Called by GoodsReceiptService after a GR is recorded — recomputes status from remaining qty. */
    @Transactional
    public void refreshStatusAfterReceiving(String poNumber) {
        Long siteId = TenantContext.requireSiteId();
        List<PurchaseOrderItem> lines = poRepository.findBySiteIdAndPoNumberOrderByItemIdAsc(siteId, poNumber);
        if (lines.isEmpty()) return;
        Map<Long, BigDecimal> received = grRepository.sumReceivedByPo(siteId, poNumber);

        boolean allFullyReceived = true;
        boolean anyReceived = false;
        for (PurchaseOrderItem line : lines) {
            BigDecimal recv = received.getOrDefault(line.getItemId(), BigDecimal.ZERO);
            if (recv.compareTo(BigDecimal.ZERO) > 0) anyReceived = true;
            if (recv.compareTo(line.getQuantity()) < 0) allFullyReceived = false;
        }

        String newStatus;
        if (allFullyReceived) newStatus = PurchaseOrderStatus.RECEIVED.name();
        else if (anyReceived) newStatus = PurchaseOrderStatus.PARTIALLY_RECEIVED.name();
        else newStatus = PurchaseOrderStatus.APPROVED.name();

        for (PurchaseOrderItem line : lines) {
            if (!newStatus.equals(line.getStatus())) {
                line.setStatus(newStatus);
                poRepository.save(line);
            }
        }
    }

    /** Next PO number in the format PO-YYYY-NNNN, sequential per year. */
    private String nextPoNumber(Long siteId) {
        int year = LocalDate.now().getYear();
        String prefix = "PO-" + year + "-";
        String last = poRepository.findMaxPoNumberWithPrefix(siteId, prefix + "%");
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

    private boolean isAllowedTransition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        return switch (to) {
            case APPROVED -> from == PurchaseOrderStatus.DRAFT;
            case CANCELLED -> from.canCancel();
            default -> false;  // RECEIVED / PARTIALLY_RECEIVED transitions happen via receiving
        };
    }

    private Map<Long, Item> loadItemsByIds(Long siteId, List<Long> itemIds) {
        Map<Long, Item> map = new HashMap<>();
        for (Long id : itemIds) {
            itemRepository.findBySiteIdAndItemId(siteId, id).ifPresent(it -> map.put(id, it));
        }
        return map;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
