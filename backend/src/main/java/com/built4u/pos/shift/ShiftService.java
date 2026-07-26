package com.built4u.pos.shift;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.sale.ReturnItemRepository;
import com.built4u.pos.sale.SaleRepository;
import com.built4u.pos.shift.dto.CloseShiftRequest;
import com.built4u.pos.shift.dto.OpenShiftRequest;
import com.built4u.pos.shift.dto.ShiftDto;
import com.built4u.pos.shift.dto.ShiftSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cashier shift lifecycle + end-of-day cash reconciliation.
 *
 * <p>A shift is keyed by (siteId, cashier) where cashier is the JWT username —
 * the same value JPA auditing stamps onto Sale/ReturnItem createdBy. Sales/refunds
 * attribute to a shift by that string match within [openedAt, closedAt].
 *
 * <p>Cash-only reconciliation: {@code expectedCash = openingFloat +
 * Σ(CASH grand_total, status≠VOIDED) − Σ(refund sub_total)}. Other modes are
 * summed for the informational panel but never reconciled. Figures are snapshotted
 * at close so a later void/refund can't rewrite a printed report.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final SaleRepository saleRepository;
    private final ReturnItemRepository returnItemRepository;

    @Transactional
    public ShiftDto openShift(OpenShiftRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String cashier = currentUsername();

        shiftRepository.findBySiteIdAndCashierAndStatus(siteId, cashier, ShiftStatus.OPEN.name())
            .ifPresent(existing -> {
                throw new ConflictException("You already have an open shift (" + existing.getShiftNumber()
                    + "). Close it before opening a new one.");
            });

        if (req.openingFloat().signum() < 0) {
            throw new BadRequestException("Opening float cannot be negative");
        }

        Shift shift = Shift.builder()
            .siteId(siteId)
            .shiftNumber(nextShiftNumber(siteId))
            .cashier(cashier)
            .status(ShiftStatus.OPEN.name())
            .openingFloat(req.openingFloat())
            .openedAt(LocalDateTime.now())
            .build();

        try {
            shiftRepository.save(shift);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("You already have an open shift. Close it before opening a new one.");
        }

        log.info("Shift {} opened by {} (site {}) with float {}", shift.getShiftNumber(), cashier, siteId, req.openingFloat());
        return toDto(shift);
    }

    @Transactional(readOnly = true)
    public ShiftDto getCurrentShift() {
        Long siteId = TenantContext.requireSiteId();
        Shift shift = shiftRepository.findBySiteIdAndCashierAndStatus(siteId, currentUsername(), ShiftStatus.OPEN.name())
            .orElseThrow(() -> new NotFoundException("No open shift"));
        return toDto(shift);
    }

    @Transactional
    public ShiftDto closeShift(String shiftNumber, CloseShiftRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String actor = currentUsername();

        Shift shift = shiftRepository.findBySiteIdAndShiftNumberForUpdate(siteId, shiftNumber)
            .orElseThrow(() -> new NotFoundException("Shift " + shiftNumber + " not found"));

        if (!ShiftStatus.OPEN.name().equals(shift.getStatus())) {
            throw new BadRequestException("Shift " + shiftNumber + " is already closed");
        }
        if (!shift.getCashier().equals(actor) && !hasManagerAuthority()) {
            throw new AccessDeniedException("Only the shift's cashier or an admin/manager can close shift " + shiftNumber);
        }

        LocalDateTime from = shift.getOpenedAt();
        LocalDateTime to = LocalDateTime.now();
        Totals t = computeTotals(siteId, shift.getCashier(), from, to);

        BigDecimal expected = shift.getOpeningFloat().add(t.cashSales).subtract(t.cashRefunds);
        BigDecimal variance = req.countedCash().subtract(expected);

        shift.setStatus(ShiftStatus.CLOSED.name());
        shift.setClosedAt(to);
        shift.setClosedBy(actor);
        shift.setCountedCash(req.countedCash());
        shift.setExpectedCash(expected);
        shift.setCashVariance(variance);
        shift.setCashSalesTotal(t.cashSales);
        shift.setCashRefundsTotal(t.cashRefunds);
        shift.setNoncashGcashTotal(t.gcash);
        shift.setNoncashPaymayaTotal(t.paymaya);
        shift.setNoncashBankTotal(t.bankTransfer);
        shift.setNoncashChequeTotal(t.cheque);
        shift.setNoncashChargeTotal(t.charge);
        shift.setSaleCount(t.saleCount);
        shift.setCloseNote(blankToNull(req.closeNote()));
        shiftRepository.save(shift);

        log.info("Shift {} closed by {}: expected={}, counted={}, variance={}, sales={}",
            shiftNumber, actor, expected, req.countedCash(), variance, t.saleCount);
        return toDto(shift);
    }

    @Transactional(readOnly = true)
    public List<ShiftSummaryDto> listShifts() {
        Long siteId = TenantContext.requireSiteId();
        return shiftRepository.findBySiteIdOrderByOpenedAtDesc(siteId).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftSummaryDto> listMyShifts() {
        Long siteId = TenantContext.requireSiteId();
        return shiftRepository.findBySiteIdAndCashierOrderByOpenedAtDesc(siteId, currentUsername())
            .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ShiftDto getShift(String shiftNumber) {
        Long siteId = TenantContext.requireSiteId();
        Shift shift = shiftRepository.findBySiteIdAndShiftNumber(siteId, shiftNumber)
            .orElseThrow(() -> new NotFoundException("Shift " + shiftNumber + " not found"));
        if (!shift.getCashier().equals(currentUsername()) && !hasManagerAuthority()) {
            throw new AccessDeniedException("You can only view your own shifts");
        }
        return toDto(shift);
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private record Totals(BigDecimal cashSales, BigDecimal cashRefunds, BigDecimal gcash, BigDecimal paymaya,
                          BigDecimal bankTransfer, BigDecimal cheque, BigDecimal charge, int saleCount) {}

    private Totals computeTotals(Long siteId, String cashier, LocalDateTime from, LocalDateTime to) {
        BigDecimal cash = saleRepository.sumGrandTotalByCashierModeInWindow(siteId, cashier, "CASH", from, to);
        BigDecimal gcash = saleRepository.sumGrandTotalByCashierModeInWindow(siteId, cashier, "GCASH", from, to);
        BigDecimal paymaya = saleRepository.sumGrandTotalByCashierModeInWindow(siteId, cashier, "PAYMAYA", from, to);
        BigDecimal bank = saleRepository.sumGrandTotalByCashierModeInWindow(siteId, cashier, "BANK TRANSFER", from, to);
        BigDecimal cheque = saleRepository.sumGrandTotalByCashierModeInWindow(siteId, cashier, "CHEQUE", from, to);
        BigDecimal charge = saleRepository.sumGrandTotalByCashierModeInWindow(siteId, cashier, "CHARGE", from, to);
        BigDecimal refunds = returnItemRepository.sumRefundSubTotalByCashierInWindow(siteId, cashier, from, to);
        long count = saleRepository.countByCashierInWindow(siteId, cashier, from, to);
        return new Totals(nz(cash), nz(refunds), nz(gcash), nz(paymaya), nz(bank), nz(cheque), nz(charge), (int) count);
    }

    private ShiftDto toDto(Shift s) {
        if (ShiftStatus.OPEN.name().equals(s.getStatus())) {
            Totals t = computeTotals(s.getSiteId(), s.getCashier(), s.getOpenedAt(), LocalDateTime.now());
            BigDecimal expected = s.getOpeningFloat().add(t.cashSales).subtract(t.cashRefunds);
            return new ShiftDto(s.getShiftNumber(), s.getCashier(), s.getStatus(), s.getOpeningFloat(),
                s.getOpenedAt(), null, null, t.cashSales, t.cashRefunds, expected, null, null,
                t.gcash, t.paymaya, t.bankTransfer, t.cheque, t.charge, t.saleCount, null, s.getCreationDate(), s.getCreatedBy());
        }
        return new ShiftDto(s.getShiftNumber(), s.getCashier(), s.getStatus(), s.getOpeningFloat(),
            s.getOpenedAt(), s.getClosedAt(), s.getClosedBy(), s.getCashSalesTotal(), s.getCashRefundsTotal(),
            s.getExpectedCash(), s.getCountedCash(), s.getCashVariance(), s.getNoncashGcashTotal(),
            s.getNoncashPaymayaTotal(), s.getNoncashBankTotal(), s.getNoncashChequeTotal(), s.getNoncashChargeTotal(),
            s.getSaleCount(), s.getCloseNote(), s.getCreationDate(), s.getCreatedBy());
    }

    private ShiftSummaryDto toSummary(Shift s) {
        if (ShiftStatus.OPEN.name().equals(s.getStatus())) {
            Totals t = computeTotals(s.getSiteId(), s.getCashier(), s.getOpenedAt(), LocalDateTime.now());
            BigDecimal expected = s.getOpeningFloat().add(t.cashSales).subtract(t.cashRefunds);
            return new ShiftSummaryDto(s.getShiftNumber(), s.getCashier(), s.getStatus(), s.getOpeningFloat(),
                expected, null, null, s.getOpenedAt(), null, t.saleCount);
        }
        return new ShiftSummaryDto(s.getShiftNumber(), s.getCashier(), s.getStatus(), s.getOpeningFloat(),
            s.getExpectedCash(), s.getCountedCash(), s.getCashVariance(), s.getOpenedAt(), s.getClosedAt(), s.getSaleCount());
    }

    private String nextShiftNumber(Long siteId) {
        int year = LocalDate.now().getYear();
        String prefix = "SH-" + year + "-";
        String last = shiftRepository.findMaxShiftNumberWithPrefix(siteId, prefix + "%");
        int next = 1;
        if (last != null) {
            try { next = Integer.parseInt(last.substring(prefix.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return String.format("%s%04d", prefix, next);
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("No authenticated user");
        return auth.getName();
    }

    private static boolean hasManagerAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a.getAuthority();
            if ("ROLE_ADMIN".equals(r) || "ROLE_MANAGER".equals(r)) return true;
        }
        return false;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
