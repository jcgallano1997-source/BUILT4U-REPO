package com.built4u.pos.shift;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.sale.ReturnItemRepository;
import com.built4u.pos.sale.SalePaymentRepository;
import com.built4u.pos.sale.SaleRepository;
import com.built4u.pos.shift.dto.CashMovementDto;
import com.built4u.pos.shift.dto.CloseShiftRequest;
import com.built4u.pos.shift.dto.OpenShiftRequest;
import com.built4u.pos.shift.dto.RecordCashMovementRequest;
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
    private final SalePaymentRepository salePaymentRepository;
    private final ReturnItemRepository returnItemRepository;
    private final CashMovementRepository cashMovementRepository;
    private final ShiftDenominationRepository shiftDenominationRepository;

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
        BigDecimal[] io = cashInOut(siteId, shiftNumber);
        BigDecimal cashIn = io[0], cashOut = io[1];

        // Counted cash: derive from the denomination tally when provided (typo-proof),
        // else trust the declared amount. Store the tally either way.
        BigDecimal counted = req.countedCash();
        if (req.denominations() != null && !req.denominations().isEmpty()) {
            counted = BigDecimal.ZERO;
            for (CloseShiftRequest.DenomCount d : req.denominations()) {
                counted = counted.add(d.denom().multiply(BigDecimal.valueOf(d.qty())));
                if (d.qty() > 0) {
                    shiftDenominationRepository.save(ShiftDenomination.builder()
                        .siteId(siteId).shiftNumber(shiftNumber).denom(d.denom()).qty(d.qty()).build());
                }
            }
        }

        BigDecimal expected = shift.getOpeningFloat().add(t.cashSales).subtract(t.cashRefunds).add(cashIn).subtract(cashOut);
        BigDecimal variance = counted.subtract(expected);

        shift.setStatus(ShiftStatus.CLOSED.name());
        shift.setClosedAt(to);
        shift.setClosedBy(actor);
        shift.setCountedCash(counted);
        shift.setExpectedCash(expected);
        shift.setCashVariance(variance);
        shift.setCashSalesTotal(t.cashSales);
        shift.setCashRefundsTotal(t.cashRefunds);
        shift.setCashInTotal(cashIn);
        shift.setCashOutTotal(cashOut);
        shift.setNoncashGcashTotal(t.gcash);
        shift.setNoncashPaymayaTotal(t.paymaya);
        shift.setNoncashBankTotal(t.bankTransfer);
        shift.setNoncashChequeTotal(t.cheque);
        shift.setNoncashChargeTotal(t.charge);
        shift.setSaleCount(t.saleCount);
        shift.setCloseNote(blankToNull(req.closeNote()));
        shiftRepository.save(shift);

        log.info("Shift {} closed by {}: expected={}, counted={}, variance={}, sales={}",
            shiftNumber, actor, expected, counted, variance, t.saleCount);
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

    // ── Cash movements ──────────────────────────────────────────────────────────

    @Transactional
    public ShiftDto recordCashMovement(String shiftNumber, RecordCashMovementRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String actor = currentUsername();
        Shift shift = shiftRepository.findBySiteIdAndShiftNumber(siteId, shiftNumber)
            .orElseThrow(() -> new NotFoundException("Shift " + shiftNumber + " not found"));
        if (!ShiftStatus.OPEN.name().equals(shift.getStatus())) {
            throw new BadRequestException("Shift " + shiftNumber + " is closed");
        }
        if (!shift.getCashier().equals(actor) && !hasManagerAuthority()) {
            throw new AccessDeniedException("Only the shift's cashier or a manager can record cash movements");
        }
        cashMovementRepository.save(CashMovement.builder()
            .siteId(siteId).shiftNumber(shiftNumber)
            .direction(req.direction().trim().toUpperCase())
            .amount(req.amount()).reason(blankToNull(req.reason())).build());
        log.info("Cash {} of {} on shift {} by {} ({})", req.direction(), req.amount(), shiftNumber, actor, req.reason());
        return getShift(shiftNumber);
    }

    @Transactional(readOnly = true)
    public List<CashMovementDto> listCashMovements(String shiftNumber) {
        Long siteId = TenantContext.requireSiteId();
        Shift shift = shiftRepository.findBySiteIdAndShiftNumber(siteId, shiftNumber)
            .orElseThrow(() -> new NotFoundException("Shift " + shiftNumber + " not found"));
        if (!shift.getCashier().equals(currentUsername()) && !hasManagerAuthority()) {
            throw new AccessDeniedException("You can only view your own shift");
        }
        return cashMovementRepository.findBySiteIdAndShiftNumberOrderByCreationDateAsc(siteId, shiftNumber).stream()
            .map(m -> new CashMovementDto(m.getMovementId(), m.getDirection(), m.getAmount(), m.getReason(),
                m.getCreatedBy(), m.getCreationDate()))
            .toList();
    }

    /** [cashIn, cashOut] recorded on a shift. */
    private BigDecimal[] cashInOut(Long siteId, String shiftNumber) {
        return new BigDecimal[]{
            nz(cashMovementRepository.sumByShiftAndDirection(siteId, shiftNumber, CashMovement.IN)),
            nz(cashMovementRepository.sumByShiftAndDirection(siteId, shiftNumber, CashMovement.OUT)),
        };
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private record Totals(BigDecimal cashSales, BigDecimal cashRefunds, BigDecimal gcash, BigDecimal paymaya,
                          BigDecimal bankTransfer, BigDecimal cheque, BigDecimal charge, int saleCount) {}

    private Totals computeTotals(Long siteId, String cashier, LocalDateTime from, LocalDateTime to) {
        // Aggregate applied tender amounts per mode (handles split tender — a single
        // sale can contribute cash AND non-cash). Cash change is already excluded.
        BigDecimal cash = salePaymentRepository.sumAppliedByCashierModeInWindow(siteId, cashier, "CASH", from, to);
        BigDecimal gcash = salePaymentRepository.sumAppliedByCashierModeInWindow(siteId, cashier, "GCASH", from, to);
        BigDecimal paymaya = salePaymentRepository.sumAppliedByCashierModeInWindow(siteId, cashier, "PAYMAYA", from, to);
        BigDecimal bank = salePaymentRepository.sumAppliedByCashierModeInWindow(siteId, cashier, "BANK TRANSFER", from, to);
        BigDecimal cheque = salePaymentRepository.sumAppliedByCashierModeInWindow(siteId, cashier, "CHEQUE", from, to);
        BigDecimal charge = salePaymentRepository.sumAppliedByCashierModeInWindow(siteId, cashier, "CHARGE", from, to);
        BigDecimal refunds = returnItemRepository.sumRefundSubTotalByCashierInWindow(siteId, cashier, from, to);
        long count = saleRepository.countByCashierInWindow(siteId, cashier, from, to);
        return new Totals(nz(cash), nz(refunds), nz(gcash), nz(paymaya), nz(bank), nz(cheque), nz(charge), (int) count);
    }

    private ShiftDto toDto(Shift s) {
        if (ShiftStatus.OPEN.name().equals(s.getStatus())) {
            Totals t = computeTotals(s.getSiteId(), s.getCashier(), s.getOpenedAt(), LocalDateTime.now());
            BigDecimal[] io = cashInOut(s.getSiteId(), s.getShiftNumber());
            BigDecimal expected = s.getOpeningFloat().add(t.cashSales).subtract(t.cashRefunds).add(io[0]).subtract(io[1]);
            return new ShiftDto(s.getShiftNumber(), s.getCashier(), s.getStatus(), s.getOpeningFloat(),
                s.getOpenedAt(), null, null, t.cashSales, t.cashRefunds, io[0], io[1], expected, null, null,
                t.gcash, t.paymaya, t.bankTransfer, t.cheque, t.charge, t.saleCount, null, s.getCreationDate(), s.getCreatedBy());
        }
        return new ShiftDto(s.getShiftNumber(), s.getCashier(), s.getStatus(), s.getOpeningFloat(),
            s.getOpenedAt(), s.getClosedAt(), s.getClosedBy(), s.getCashSalesTotal(), s.getCashRefundsTotal(),
            s.getCashInTotal(), s.getCashOutTotal(), s.getExpectedCash(), s.getCountedCash(), s.getCashVariance(),
            s.getNoncashGcashTotal(), s.getNoncashPaymayaTotal(), s.getNoncashBankTotal(), s.getNoncashChequeTotal(),
            s.getNoncashChargeTotal(), s.getSaleCount(), s.getCloseNote(), s.getCreationDate(), s.getCreatedBy());
    }

    private ShiftSummaryDto toSummary(Shift s) {
        if (ShiftStatus.OPEN.name().equals(s.getStatus())) {
            Totals t = computeTotals(s.getSiteId(), s.getCashier(), s.getOpenedAt(), LocalDateTime.now());
            BigDecimal[] io = cashInOut(s.getSiteId(), s.getShiftNumber());
            BigDecimal expected = s.getOpeningFloat().add(t.cashSales).subtract(t.cashRefunds).add(io[0]).subtract(io[1]);
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
