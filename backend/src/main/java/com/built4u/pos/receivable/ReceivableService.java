package com.built4u.pos.receivable;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.customer.Customer;
import com.built4u.pos.customer.CustomerRepository;
import com.built4u.pos.receivable.dto.ReceivableDetailDto;
import com.built4u.pos.receivable.dto.ReceivableDto;
import com.built4u.pos.receivable.dto.ReceivablePaymentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository repo;
    private final ReceivablePaymentRepository paymentRepo;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<ReceivableDto> list(String status, Long customerId, String search,
                                    boolean overdue, Pageable pageable) {
        long siteId = TenantContext.requireSiteId();
        LocalDate today = LocalDate.now();
        String pattern = (search == null || search.isBlank())
            ? null : "%" + search.trim().toLowerCase() + "%";
        Page<Receivable> page = repo.search(siteId,
            (status == null || status.isBlank()) ? null : status.trim().toUpperCase(),
            customerId, pattern, overdue, today, pageable);
        Map<Long, String> names = new HashMap<>();
        return page.map(r -> ReceivableDto.from(r,
            names.computeIfAbsent(r.getCustomerId(), cid ->
                customerRepository.findBySiteIdAndCustomerId(siteId, cid)
                    .map(Customer::getCustomerName).orElse("#" + cid)),
            today));
    }

    /** Record a collection: locks the row, reduces the balance, closes when fully paid. */
    @Transactional
    public ReceivableDetailDto recordPayment(Long id, BigDecimal amount, String note) {
        long siteId = TenantContext.requireSiteId();
        Receivable r = repo.findBySiteIdAndIdForUpdate(siteId, id)
            .orElseThrow(() -> new NotFoundException("Receivable " + id + " not found"));
        if (ReceivableStatus.CANCELLED.name().equals(r.getStatus())) {
            throw new BadRequestException("This receivable was cancelled (sale voided).");
        }
        if (ReceivableStatus.PAID.name().equals(r.getStatus()) || r.getBalance().signum() == 0) {
            throw new BadRequestException("This receivable is already fully paid.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero.");
        }
        if (amount.compareTo(r.getBalance()) > 0) {
            throw new BadRequestException(
                "Payment ₱" + amount.toPlainString() + " exceeds the outstanding balance ₱"
                + r.getBalance().toPlainString() + ".");
        }

        r.setAmountPaid(r.getAmountPaid().add(amount));
        r.setBalance(r.getBalance().subtract(amount));
        if (r.getBalance().signum() == 0) {
            r.setStatus(ReceivableStatus.PAID.name());
            r.setClosedAt(LocalDateTime.now());
        } else {
            r.setStatus(ReceivableStatus.PARTIAL.name());
        }
        repo.save(r);

        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();
        paymentRepo.save(ReceivablePayment.builder()
            .siteId(siteId)
            .receivableId(r.getId())
            .amount(amount)
            .note(cleanNote)
            .build());

        return get(id);
    }

    @Transactional(readOnly = true)
    public ReceivableDetailDto get(Long id) {
        long siteId = TenantContext.requireSiteId();
        Receivable r = repo.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Receivable " + id + " not found"));
        String name = customerRepository.findBySiteIdAndCustomerId(siteId, r.getCustomerId())
            .map(Customer::getCustomerName).orElse("#" + r.getCustomerId());
        var payments = paymentRepo
            .findBySiteIdAndReceivableIdOrderByIdDesc(siteId, r.getId())
            .stream().map(ReceivablePaymentDto::from).toList();
        return new ReceivableDetailDto(
            ReceivableDto.from(r, name, LocalDate.now()), payments);
    }
}
