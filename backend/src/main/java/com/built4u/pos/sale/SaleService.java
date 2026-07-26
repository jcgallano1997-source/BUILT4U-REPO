package com.built4u.pos.sale;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.customer.Customer;
import com.built4u.pos.customer.CustomerRepository;
import com.built4u.pos.item.Item;
import com.built4u.pos.item.ItemRepository;
import com.built4u.pos.loyalty.LoyaltyConfigService;
import com.built4u.pos.loyalty.LoyaltyLedger;
import com.built4u.pos.loyalty.LoyaltyLedgerRepository;
import com.built4u.pos.loyalty.LoyaltyLedgerService;
import com.built4u.pos.payment.PaymentMode;
import com.built4u.pos.payment.PaymentModeRepository;
import com.built4u.pos.receivable.Receivable;
import com.built4u.pos.receivable.ReceivableRepository;
import com.built4u.pos.receivable.ReceivableStatus;
import com.built4u.pos.sale.dto.*;
import com.built4u.pos.voucher.Voucher;
import com.built4u.pos.voucher.VoucherRedeemService;
import com.built4u.pos.voucher.VoucherRedemption;
import com.built4u.pos.voucher.VoucherRedemptionRepository;
import com.built4u.pos.voucher.VoucherRepository;
import com.built4u.pos.voucher.dto.VoucherEvaluation;
import com.built4u.pos.transactionlog.TransactionLog;
import com.built4u.pos.transactionlog.TransactionLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * POS core: checkout (stock-decrementing sale), void (restore stock), refund
 * (partial returns), and read/list. Everything is scoped by site_id from the JWT.
 *
 * <p>Deferred to later phases: payment-mode surcharges, accounts-receivable
 * (CHARGE credit), loyalty points, vouchers, receipt PDF/email. In this phase all
 * modes require full payment and mode_of_payment is a free string.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ItemRepository itemRepository;
    private final TransactionLogRepository txnLogRepository;
    private final CustomerRepository customerRepository;
    private final PaymentModeRepository paymentModeRepository;
    private final ReceivableRepository receivableRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherRedeemService voucherRedeemService;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final LoyaltyConfigService loyaltyConfigService;
    private final LoyaltyLedgerService loyaltyLedgerService;
    private final LoyaltyLedgerRepository loyaltyLedgerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Checkout ─────────────────────────────────────────────────────────────

    @Transactional
    public SaleDto checkout(CreateSaleRequest req) {
        Long siteId = TenantContext.requireSiteId();

        // Reject duplicate item lines (composite PK would collide) and lock items
        // in ascending itemId order to avoid deadlocks across concurrent checkouts.
        Set<Long> seen = new HashSet<>();
        for (CreateSaleRequest.Line l : req.lines()) {
            if (!seen.add(l.itemId())) {
                throw new BadRequestException("Item " + l.itemId() + " appears more than once in the cart");
            }
        }
        List<CreateSaleRequest.Line> ordered = req.lines().stream()
            .sorted(Comparator.comparing(CreateSaleRequest.Line::itemId)).toList();

        String salesNumber = nextSalesNumber(siteId);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalDiscItem = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        List<TransactionLog> logs = new ArrayList<>();

        for (CreateSaleRequest.Line l : ordered) {
            Item item = itemRepository.findBySiteIdAndItemIdForUpdate(siteId, l.itemId())
                .orElseThrow(() -> new NotFoundException("Item " + l.itemId() + " not found"));
            if (!Boolean.TRUE.equals(item.getActive())) {
                throw new BadRequestException("Item '" + item.getItemCode() + "' is inactive and can't be sold");
            }
            BigDecimal qty = l.quantity();
            BigDecimal adj = l.adjustment() == null ? BigDecimal.ZERO : l.adjustment();
            BigDecimal price = item.getSellingPrice();
            BigDecimal gross = price.multiply(qty);
            BigDecimal sub = gross.subtract(adj);
            if (sub.signum() < 0) {
                throw new BadRequestException("Line discount exceeds the line total for '" + item.getItemCode() + "'");
            }
            BigDecimal newQty = nz(item.getQuantity()).subtract(qty);
            if (newQty.signum() < 0) {
                throw new BadRequestException("Insufficient stock for '" + item.getItemCode()
                    + "' (have " + nz(item.getQuantity()).toPlainString() + ", need " + qty.toPlainString() + ")");
            }
            item.setQuantity(newQty);
            total = total.add(gross);
            totalDiscItem = totalDiscItem.add(adj);

            saleItems.add(SaleItem.builder()
                .siteId(siteId).salesNumber(salesNumber).itemId(item.getItemId())
                .itemDesc(item.getItemName()).quantity(qty).uom(item.getUom())
                .unitCost(price).adjustment(adj).subTotal(sub).build());
            logs.add(TransactionLog.builder()
                .siteId(siteId).itemId(item.getItemId()).catId(item.getCatId())
                .transactionType(TransactionLog.TYPE_STOCK_OUT_SALE)
                .attribute1(salesNumber).attribute2(qty.negate().toPlainString())
                .attribute3(newQty.toPlainString()).build());
        }

        BigDecimal userDiscountAll = req.discountAll() == null ? BigDecimal.ZERO : req.discountAll();
        BigDecimal subtotalForVoucher = total.subtract(totalDiscItem).subtract(userDiscountAll);
        if (subtotalForVoucher.signum() < 0) {
            throw new BadRequestException("Discounts exceed the sale total");
        }

        // Voucher (optional): evaluate against the post-line/post-manual-discount
        // subtotal, then atomically consume. The discount folds into discountAll
        // so the stored totals stay consistent; a redemption row is the audit link.
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        String voucherCode = blankToNull(req.voucherCode());
        if (voucherCode != null) {
            VoucherEvaluation ev = voucherRedeemService.evaluate(siteId, voucherCode, subtotalForVoucher, req.customerId());
            if (!ev.valid()) throw new BadRequestException(ev.message());
            appliedVoucher = voucherRepository.findBySiteIdAndCodeIgnoreCase(siteId, voucherCode)
                .orElseThrow(() -> new BadRequestException("Voucher '" + voucherCode + "' not found."));
            if (voucherRepository.tryConsume(appliedVoucher.getId()) == 0) {
                throw new BadRequestException("This voucher has reached its usage limit.");
            }
            voucherDiscount = ev.discountAmount();
        }

        BigDecimal discountAll = userDiscountAll.add(voucherDiscount);
        BigDecimal grandTotal = total.subtract(totalDiscItem).subtract(discountAll);
        if (grandTotal.signum() < 0) grandTotal = BigDecimal.ZERO;

        // Resolve the customer up front (a credit sale needs it).
        Customer customer = null;
        if (req.customerId() != null) {
            customer = customerRepository.findBySiteIdAndCustomerId(siteId, req.customerId())
                .filter(cc -> Boolean.TRUE.equals(cc.getActive()))
                .orElseThrow(() -> new BadRequestException("Customer " + req.customerId() + " not found or inactive"));
        }
        Long customerId = customer == null ? null : customer.getCustomerId();

        // An accounts-receivable payment mode lets the sale go out on credit: the
        // unpaid remainder becomes a receivable, so payment may be < grand total.
        String modeCode = req.modeOfPayment().trim();
        PaymentMode mode = paymentModeRepository.findBySiteIdAndCode(siteId, modeCode).orElse(null);
        boolean isAr = mode != null && mode.isAccountsReceivable();
        BigDecimal payment = req.payment();

        BigDecimal change;
        BigDecimal arBalance = BigDecimal.ZERO;
        if (isAr) {
            if (customer == null) {
                throw new BadRequestException("A customer is required for a credit (accounts-receivable) sale");
            }
            if (payment.signum() < 0) {
                throw new BadRequestException("Payment can't be negative");
            }
            arBalance = grandTotal.subtract(payment).max(BigDecimal.ZERO);   // unpaid → on account
            change = payment.subtract(grandTotal).max(BigDecimal.ZERO);      // no change on credit
            if (arBalance.signum() > 0 && customer.getCreditLimit() != null
                    && customer.getCreditLimit().signum() > 0) {
                BigDecimal outstanding = nz(receivableRepository.sumOpenBalanceByCustomer(siteId, customerId));
                if (outstanding.add(arBalance).compareTo(customer.getCreditLimit()) > 0) {
                    throw new BadRequestException("Credit limit ₱" + customer.getCreditLimit().toPlainString()
                        + " exceeded — outstanding ₱" + outstanding.toPlainString()
                        + " + this ₱" + arBalance.toPlainString());
                }
            }
        } else {
            if (payment.compareTo(grandTotal) < 0) {
                throw new BadRequestException("Payment " + payment.toPlainString()
                    + " is less than the amount due " + grandTotal.toPlainString());
            }
            change = payment.subtract(grandTotal);
        }

        Sale sale = Sale.builder()
            .siteId(siteId).salesNumber(salesNumber)
            .total(total).discountAll(discountAll).totalDiscItem(totalDiscItem).grandTotal(grandTotal)
            .payment(payment).changeDue(change)
            .modeOfPayment(modeCode).customerId(customerId).reference(blankToNull(req.reference()))
            .status(SaleStatus.COMPLETED.name())
            .build();
        saleRepository.save(sale);
        saleItemRepository.saveAll(saleItems);
        txnLogRepository.saveAll(logs);

        // Credit sale → open a receivable for the unpaid balance.
        if (isAr && arBalance.signum() > 0) {
            LocalDate due = LocalDate.now().plusDays(mode.getArDueDays());
            receivableRepository.save(Receivable.builder()
                .siteId(siteId).salesNumber(salesNumber).customerId(customerId)
                .modeCode(mode.getCode()).originalAmount(arBalance)
                .amountPaid(BigDecimal.ZERO).balance(arBalance).dueDate(due)
                .status(ReceivableStatus.OPEN.name()).build());
            log.info("Sale {} opened a receivable of {} for customer {} (due {})",
                salesNumber, arBalance, customerId, due);
        }

        // Voucher redemption record (the sale↔voucher audit link).
        if (appliedVoucher != null) {
            voucherRedemptionRepository.save(VoucherRedemption.builder()
                .siteId(siteId).voucherId(appliedVoucher.getId()).salesNumber(salesNumber)
                .customerId(customerId).discountAmount(voucherDiscount).build());
            log.info("Sale {} redeemed voucher {} (−{})", salesNumber, appliedVoucher.getCode(), voucherDiscount);
        }

        // Loyalty: earn whole points on the grand total when a customer is attached.
        if (customer != null) {
            BigDecimal earned = grandTotal.multiply(loyaltyConfigService.resolveRate())
                .setScale(0, java.math.RoundingMode.FLOOR);
            if (earned.signum() > 0) {
                customer.setPoints(nz(customer.getPoints()).add(earned));
                customerRepository.save(customer);
                loyaltyLedgerService.post(siteId, customerId, "EARN", earned, salesNumber,
                    "Earned on sale " + salesNumber, null);
                log.info("Sale {} earned {} loyalty points for customer {}", salesNumber, earned, customerId);
            }
        }

        log.info("Sale {} completed at site {}: {} line(s), grand total {}", salesNumber, siteId, saleItems.size(), grandTotal);
        return reload(siteId, salesNumber);
    }

    // ── Void (restore all stock) ─────────────────────────────────────────────

    @Transactional
    public SaleDto voidSale(String salesNumber) {
        Long siteId = TenantContext.requireSiteId();
        Sale sale = saleRepository.findBySiteIdAndSalesNumberForUpdate(siteId, salesNumber)
            .orElseThrow(() -> new NotFoundException("Sale " + salesNumber + " not found"));
        if (!SaleStatus.COMPLETED.name().equals(sale.getStatus())) {
            throw new BadRequestException("Only a completed sale can be voided (this one is " + sale.getStatus() + ")");
        }
        if (!returnItemRepository.findBySiteIdAndSalesNumberOrderByCreationDateAscItemIdAsc(siteId, salesNumber).isEmpty()) {
            throw new BadRequestException("Sale " + salesNumber + " has refunds and can't be voided");
        }

        for (SaleItem si : saleItemRepository.findBySiteIdAndSalesNumberOrderByItemIdAsc(siteId, salesNumber)) {
            restoreStock(siteId, si.getItemId(), si.getQuantity(), salesNumber, TransactionLog.TYPE_STOCK_IN_VOID);
        }
        sale.setStatus(SaleStatus.VOIDED.name());
        saleRepository.save(sale);

        // A credit sale's receivable is written off when the sale is voided.
        receivableRepository.findBySiteIdAndSalesNumber(siteId, salesNumber)
            .filter(r -> !ReceivableStatus.CANCELLED.name().equals(r.getStatus()))
            .ifPresent(r -> {
                r.setStatus(ReceivableStatus.CANCELLED.name());
                r.setClosedAt(LocalDateTime.now());
                receivableRepository.save(r);
                log.info("Sale {} void → receivable {} CANCELLED", salesNumber, r.getId());
            });

        // Release any voucher redeemed on this sale (frees a usage).
        voucherRedemptionRepository.findBySiteIdAndSalesNumberAndReversedFalse(siteId, salesNumber)
            .ifPresent(vr -> {
                vr.setReversed(true);
                vr.setReversedAt(LocalDateTime.now());
                voucherRedemptionRepository.save(vr);
                voucherRepository.release(vr.getVoucherId());
                log.info("Sale {} void → voucher redemption {} reversed", salesNumber, vr.getId());
            });

        // Claw back loyalty points earned on this sale.
        List<LoyaltyLedger> earns =
            loyaltyLedgerRepository.findBySiteIdAndSalesNumberAndEntryType(siteId, salesNumber, "EARN");
        if (!earns.isEmpty() && sale.getCustomerId() != null) {
            BigDecimal earned = earns.stream().map(LoyaltyLedger::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (earned.signum() > 0) {
                Long custId = sale.getCustomerId();
                customerRepository.findBySiteIdAndCustomerId(siteId, custId).ifPresent(c -> {
                    c.setPoints(nz(c.getPoints()).subtract(earned).max(BigDecimal.ZERO));
                    customerRepository.save(c);
                });
                loyaltyLedgerService.post(siteId, custId, "ADJUST", earned.negate(), salesNumber,
                    "Reversed on void of " + salesNumber, null);
                log.info("Sale {} void → clawed back {} loyalty points", salesNumber, earned);
            }
        }

        log.info("Sale {} voided at site {}", salesNumber, siteId);
        return reload(siteId, salesNumber);
    }

    // ── Refund (partial returns) ─────────────────────────────────────────────

    @Transactional
    public ReturnDto refund(String salesNumber, RefundRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Sale sale = saleRepository.findBySiteIdAndSalesNumberForUpdate(siteId, salesNumber)
            .orElseThrow(() -> new NotFoundException("Sale " + salesNumber + " not found"));
        if (SaleStatus.VOIDED.name().equals(sale.getStatus())) {
            throw new BadRequestException("A voided sale can't be refunded");
        }

        Set<Long> seen = new HashSet<>();
        for (RefundRequest.Line l : req.lines()) {
            if (!seen.add(l.itemId())) {
                throw new BadRequestException("Item " + l.itemId() + " appears more than once in the refund");
            }
        }

        Map<Long, SaleItem> soldByItem = saleItemRepository
            .findBySiteIdAndSalesNumberOrderByItemIdAsc(siteId, salesNumber).stream()
            .collect(Collectors.toMap(SaleItem::getItemId, si -> si));

        String returnNumber = nextReturnNumber(siteId);
        String reason = blankToNull(req.reason());
        List<ReturnItem> returns = new ArrayList<>();
        BigDecimal totalRefunded = BigDecimal.ZERO;

        List<RefundRequest.Line> ordered = req.lines().stream()
            .sorted(Comparator.comparing(RefundRequest.Line::itemId)).toList();
        for (RefundRequest.Line l : ordered) {
            SaleItem orig = soldByItem.get(l.itemId());
            if (orig == null) {
                throw new BadRequestException("Item " + l.itemId() + " was not on sale " + salesNumber);
            }
            BigDecimal already = returnItemRepository.sumRefundedQuantity(siteId, salesNumber, l.itemId());
            BigDecimal refundable = orig.getQuantity().subtract(nz(already));
            if (l.quantity().compareTo(refundable) > 0) {
                throw new BadRequestException("Can refund at most " + refundable.toPlainString()
                    + " of item '" + (orig.getItemDesc() == null ? l.itemId() : orig.getItemDesc()) + "'");
            }
            BigDecimal sub = orig.getUnitCost().multiply(l.quantity());
            restoreStock(siteId, l.itemId(), l.quantity(), salesNumber, TransactionLog.TYPE_STOCK_IN_REFUND);
            returns.add(ReturnItem.builder()
                .siteId(siteId).returnNumber(returnNumber).itemId(l.itemId()).salesNumber(salesNumber)
                .itemDesc(orig.getItemDesc()).quantity(l.quantity()).uom(orig.getUom())
                .unitCost(orig.getUnitCost()).subTotal(sub).reason(reason).build());
            totalRefunded = totalRefunded.add(sub);
        }
        returnItemRepository.saveAll(returns);

        // If every sale line is now fully refunded, mark the sale REFUNDED.
        boolean fullyRefunded = soldByItem.values().stream().allMatch(si -> {
            BigDecimal refunded = nz(returnItemRepository.sumRefundedQuantity(siteId, salesNumber, si.getItemId()));
            return refunded.compareTo(si.getQuantity()) >= 0;
        });
        if (fullyRefunded && !SaleStatus.REFUNDED.name().equals(sale.getStatus())) {
            sale.setStatus(SaleStatus.REFUNDED.name());
            saleRepository.save(sale);
        }

        log.info("Refund {} against sale {} at site {}: {} line(s), {} refunded",
            returnNumber, salesNumber, siteId, returns.size(), totalRefunded);

        entityManager.flush();
        entityManager.clear();
        return buildReturnDto(siteId, returnNumber);
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SaleSummaryDto> list(String status) {
        Long siteId = TenantContext.requireSiteId();
        List<Sale> sales = (status == null || status.isBlank())
            ? saleRepository.findTop200BySiteIdOrderByCreationDateDescSalesNumberDesc(siteId)
            : saleRepository.findTop200BySiteIdAndStatusOrderByCreationDateDescSalesNumberDesc(siteId, status.trim().toUpperCase());
        return sales.stream().map(s -> new SaleSummaryDto(
            s.getSalesNumber(), s.getGrandTotal(), s.getModeOfPayment(), customerName(siteId, s.getCustomerId()),
            saleItemRepository.findBySiteIdAndSalesNumberOrderByItemIdAsc(siteId, s.getSalesNumber()).size(),
            s.getStatus(), s.getCreationDate(), s.getCreatedBy())).toList();
    }

    @Transactional(readOnly = true)
    public SaleDto get(String salesNumber) {
        return toDto(TenantContext.requireSiteId(), salesNumber);
    }

    @Transactional(readOnly = true)
    public ReturnDto getReturn(String returnNumber) {
        return buildReturnDto(TenantContext.requireSiteId(), returnNumber);
    }

    // ── internals ────────────────────────────────────────────────────────────

    private void restoreStock(Long siteId, Long itemId, BigDecimal qty, String salesNumber, String txnType) {
        itemRepository.findBySiteIdAndItemIdForUpdate(siteId, itemId).ifPresentOrElse(item -> {
            BigDecimal newQty = nz(item.getQuantity()).add(qty);
            item.setQuantity(newQty);
            txnLogRepository.save(TransactionLog.builder()
                .siteId(siteId).itemId(itemId).catId(item.getCatId())
                .transactionType(txnType).attribute1(salesNumber)
                .attribute2(qty.toPlainString()).attribute3(newQty.toPlainString()).build());
        }, () -> log.warn("Stock restore skipped — item {} no longer exists at site {}", itemId, siteId));
    }

    private SaleDto reload(Long siteId, String salesNumber) {
        entityManager.flush();
        entityManager.clear();
        return toDto(siteId, salesNumber);
    }

    private SaleDto toDto(Long siteId, String salesNumber) {
        Sale s = saleRepository.findBySiteIdAndSalesNumber(siteId, salesNumber)
            .orElseThrow(() -> new NotFoundException("Sale " + salesNumber + " not found"));
        List<SaleLineDto> lines = saleItemRepository.findBySiteIdAndSalesNumberOrderByItemIdAsc(siteId, salesNumber)
            .stream().map(si -> {
                BigDecimal refunded = nz(returnItemRepository.sumRefundedQuantity(siteId, salesNumber, si.getItemId()));
                return new SaleLineDto(si.getItemId(), si.getItemDesc(), si.getUom(), si.getQuantity(),
                    si.getAdjustment(), si.getUnitCost(), si.getSubTotal(), refunded, si.getQuantity().subtract(refunded));
            }).toList();
        return new SaleDto(s.getSalesNumber(), s.getTotal(), s.getDiscountAll(), s.getTotalDiscItem(),
            s.getGrandTotal(), s.getPayment(), s.getChangeDue(), s.getModeOfPayment(),
            s.getCustomerId(), customerName(siteId, s.getCustomerId()), s.getReference(),
            s.getStatus(), s.getReprintCount(), s.getCreationDate(), s.getCreatedBy(), lines);
    }

    private String customerName(Long siteId, Long customerId) {
        if (customerId == null) return null;
        return customerRepository.findBySiteIdAndCustomerId(siteId, customerId)
            .map(Customer::getCustomerName).orElse(null);
    }

    private ReturnDto buildReturnDto(Long siteId, String returnNumber) {
        List<ReturnItem> rows = returnItemRepository.findBySiteIdAndReturnNumberOrderByItemIdAsc(siteId, returnNumber);
        if (rows.isEmpty()) throw new NotFoundException("Return " + returnNumber + " not found");
        ReturnItem head = rows.get(0);
        BigDecimal totalRefunded = rows.stream().map(ReturnItem::getSubTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReturnDto.Line> lines = rows.stream().map(r -> new ReturnDto.Line(
            r.getItemId(), r.getItemDesc(), r.getUom(), r.getQuantity(), r.getUnitCost(), r.getSubTotal())).toList();
        return new ReturnDto(returnNumber, head.getSalesNumber(), totalRefunded, head.getReason(),
            head.getCreationDate(), head.getCreatedBy(), lines);
    }

    private String nextSalesNumber(Long siteId) {
        return nextNumber("S-" + LocalDate.now().getYear() + "-",
            saleRepository::findMaxSalesNumberWithPrefix, siteId);
    }

    private String nextReturnNumber(Long siteId) {
        return nextNumber("R-" + LocalDate.now().getYear() + "-",
            returnItemRepository::findMaxReturnNumberWithPrefix, siteId);
    }

    private interface MaxLookup { String max(Long siteId, String prefix); }

    private String nextNumber(String prefix, MaxLookup lookup, Long siteId) {
        String last = lookup.max(siteId, prefix + "%");
        int next = 1;
        if (last != null) {
            try { next = Integer.parseInt(last.substring(prefix.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return String.format("%s%04d", prefix, next);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
