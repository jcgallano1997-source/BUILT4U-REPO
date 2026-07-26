package com.built4u.pos.voucher;

import com.built4u.pos.voucher.dto.VoucherEvaluation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Evaluates a voucher against a cart and computes the discount. Pure / no
 * consumption — both the POS validate endpoint and {@code SaleService.checkout}
 * call {@link #evaluate}; checkout then atomically consumes via
 * {@code VoucherRepository.tryConsume}.
 */
@Service
@RequiredArgsConstructor
public class VoucherRedeemService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final VoucherRepository voucherRepo;

    /** @param customerId reserved for a future per-customer limit (unused in v1). */
    @Transactional(readOnly = true)
    public VoucherEvaluation evaluate(Long siteId, String code, BigDecimal subtotal, Long customerId) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) return VoucherEvaluation.invalid(c, "NOT_FOUND", "Enter a voucher code.");

        Optional<Voucher> found = voucherRepo.findBySiteIdAndCodeIgnoreCase(siteId, c);
        if (found.isEmpty()) {
            return VoucherEvaluation.invalid(c, "NOT_FOUND", "Voucher '" + c + "' not found.");
        }
        Voucher v = found.get();
        if (!v.isActive()) {
            return VoucherEvaluation.invalid(c, "INACTIVE", "This voucher is inactive.");
        }
        LocalDate today = LocalDate.now();
        if (v.getValidFrom() != null && today.isBefore(v.getValidFrom())) {
            return VoucherEvaluation.invalid(c, "NOT_STARTED",
                "This voucher is valid from " + v.getValidFrom() + ".");
        }
        if (v.getValidTo() != null && today.isAfter(v.getValidTo())) {
            return VoucherEvaluation.invalid(c, "EXPIRED",
                "This voucher expired on " + v.getValidTo() + ".");
        }
        if (v.getMinSpend() != null && subtotal.compareTo(v.getMinSpend()) < 0) {
            return VoucherEvaluation.invalid(c, "BELOW_MIN_SPEND",
                "Requires a minimum spend of " + v.getMinSpend().toPlainString() + ".");
        }
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            return VoucherEvaluation.invalid(c, "USAGE_EXHAUSTED",
                "This voucher has reached its usage limit.");
        }
        BigDecimal amount = computeDiscount(v, subtotal);
        return VoucherEvaluation.ok(v.getCode(), v.getDescription(), v.getDiscountType(), amount);
    }

    /** Discount the voucher gives on {@code subtotal}; never exceeds the subtotal. */
    public BigDecimal computeDiscount(Voucher v, BigDecimal subtotal) {
        BigDecimal amount;
        if ("PERCENT".equals(v.getDiscountType())) {
            amount = subtotal.multiply(v.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            if (v.getMaxDiscount() != null && amount.compareTo(v.getMaxDiscount()) > 0) {
                amount = v.getMaxDiscount();
            }
        } else { // FIXED
            amount = v.getDiscountValue();
        }
        if (amount.compareTo(subtotal) > 0) amount = subtotal;   // never negative total
        if (amount.signum() < 0) amount = BigDecimal.ZERO;
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
