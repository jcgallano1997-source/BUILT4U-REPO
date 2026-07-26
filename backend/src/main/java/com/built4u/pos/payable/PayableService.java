package com.built4u.pos.payable;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.payable.dto.CreateExpenseRequest;
import com.built4u.pos.payable.dto.PayableDetailDto;
import com.built4u.pos.payable.dto.PayableDto;
import com.built4u.pos.payable.dto.PayablePaymentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Accounts Payable. PURCHASE-source payables are auto-created at GR by
 * {@code GoodsReceiptService}; EXPENSE-source manual entries via
 * {@link #createExpense}. Mirror of {@link com.built4u.pos.receivable.ReceivableService}.
 */
@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayablePaymentRepository paymentRepository;

    @Transactional
    public PayableDto createExpense(CreateExpenseRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Payable saved = payableRepository.save(Payable.builder()
            .siteId(siteId)
            .source(PayableSource.EXPENSE.name())
            .category(blankToNull(req.category()))
            .payeeName(req.payeeName().trim())
            .description(blankToNull(req.description()))
            .originalAmount(req.amount())
            .balance(req.amount())
            .dueDate(req.dueDate())
            .status(PayableStatus.OPEN.name())
            .build());
        return PayableDto.from(saved, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Page<PayableDto> list(String status, String source, Long supplierId,
                                 String search, boolean overdue, Pageable pageable) {
        long siteId = TenantContext.requireSiteId();
        LocalDate today = LocalDate.now();
        String pattern = (search == null || search.isBlank())
            ? null : "%" + search.trim().toLowerCase() + "%";
        return payableRepository.search(siteId,
            (status == null || status.isBlank()) ? null : status.trim().toUpperCase(),
            (source == null || source.isBlank()) ? null : source.trim().toUpperCase(),
            supplierId, pattern, overdue, today, pageable)
            .map(p -> PayableDto.from(p, today));
    }

    /** Record a disbursement: locks the row, reduces the balance, closes when fully paid. */
    @Transactional
    public PayableDetailDto recordPayment(Long id, BigDecimal amount, String note) {
        long siteId = TenantContext.requireSiteId();
        Payable p = payableRepository.findBySiteIdAndIdForUpdate(siteId, id)
            .orElseThrow(() -> new NotFoundException("Payable " + id + " not found"));
        if (PayableStatus.CANCELLED.name().equals(p.getStatus())) {
            throw new BadRequestException("This payable was cancelled (linked PO/GR voided).");
        }
        if (PayableStatus.PAID.name().equals(p.getStatus()) || p.getBalance().signum() == 0) {
            throw new BadRequestException("This payable is already fully paid.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero.");
        }
        if (amount.compareTo(p.getBalance()) > 0) {
            throw new BadRequestException(
                "Payment ₱" + amount.toPlainString() + " exceeds the outstanding balance ₱"
                + p.getBalance().toPlainString() + ".");
        }

        p.setAmountPaid(p.getAmountPaid().add(amount));
        p.setBalance(p.getBalance().subtract(amount));
        if (p.getBalance().signum() == 0) {
            p.setStatus(PayableStatus.PAID.name());
            p.setClosedAt(LocalDateTime.now());
        } else {
            p.setStatus(PayableStatus.PARTIAL.name());
        }
        payableRepository.save(p);

        paymentRepository.save(PayablePayment.builder()
            .siteId(siteId)
            .payableId(p.getId())
            .amount(amount)
            .note(blankToNull(note))
            .build());

        return get(id);
    }

    @Transactional(readOnly = true)
    public PayableDetailDto get(Long id) {
        long siteId = TenantContext.requireSiteId();
        Payable p = payableRepository.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Payable " + id + " not found"));
        var payments = paymentRepository
            .findBySiteIdAndPayableIdOrderByIdDesc(siteId, p.getId())
            .stream().map(PayablePaymentDto::from).toList();
        return new PayableDetailDto(PayableDto.from(p, LocalDate.now()), payments);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
