package com.built4u.pos.loyalty;

import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.customer.Customer;
import com.built4u.pos.customer.CustomerRepository;
import com.built4u.pos.loyalty.dto.LedgerEntryDto;
import com.built4u.pos.loyalty.dto.LoyaltyLedgerView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Posts loyalty ledger entries. {@link #post} joins the caller's transaction
 * (e.g. checkout) and only records history — it never mutates
 * {@code customer.points} (the caller owns the live balance).
 */
@Service
@RequiredArgsConstructor
public class LoyaltyLedgerService {

    private final LoyaltyLedgerRepository repo;
    private final CustomerRepository customerRepository;

    /** Paged points history for a customer (newest first) + reconciliation. */
    @Transactional(readOnly = true)
    public LoyaltyLedgerView history(Long customerId, int page, int size) {
        Long siteId = TenantContext.requireSiteId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<LedgerEntryDto> p = repo
            .findBySiteIdAndCustomerIdOrderByIdDesc(siteId, customerId, PageRequest.of(Math.max(page, 0), safeSize))
            .map(LedgerEntryDto::from);
        BigDecimal ledgerSum = repo.sumPoints(siteId, customerId);
        BigDecimal liveBalance = customerRepository.findBySiteIdAndCustomerId(siteId, customerId)
            .map(Customer::getPoints).orElse(BigDecimal.ZERO);
        if (liveBalance == null) liveBalance = BigDecimal.ZERO;
        return new LoyaltyLedgerView(
            p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
            ledgerSum == null ? BigDecimal.ZERO : ledgerSum, liveBalance);
    }

    @Transactional
    public void post(Long siteId, Long customerId, String entryType,
                     BigDecimal points, String salesNumber, String note, LocalDate expiresAt) {
        if (customerId == null || points == null || points.signum() == 0) return;
        repo.save(LoyaltyLedger.builder()
            .siteId(siteId).customerId(customerId).entryType(entryType)
            .points(points).salesNumber(salesNumber).note(note)
            .expiresAt(expiresAt).expired(false).build());
    }
}
