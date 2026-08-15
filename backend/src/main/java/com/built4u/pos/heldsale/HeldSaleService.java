package com.built4u.pos.heldsale;

import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.heldsale.dto.HeldSaleDto;
import com.built4u.pos.heldsale.dto.HeldSaleSummaryDto;
import com.built4u.pos.heldsale.dto.SaveHeldSaleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Park/recall of in-progress POS carts. Everything is scoped to the caller's
 * active site, so a held sale can be recalled by any cashier at that site.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeldSaleService {

    private final HeldSaleRepository repository;

    @Transactional
    public HeldSaleDto save(SaveHeldSaleRequest req) {
        Long siteId = TenantContext.requireSiteId();
        HeldSale saved = repository.save(HeldSale.builder()
            .siteId(siteId)
            .label(blankToNull(req.label()))
            .customerId(req.customerId())
            .customerName(blankToNull(req.customerName()))
            .itemCount(req.itemCount() == null ? 0 : req.itemCount())
            .totalAmount(req.totalAmount() == null ? BigDecimal.ZERO : req.totalAmount())
            .cartJson(req.cartJson())
            .build());
        log.info("Held sale {} parked at site {} ({} items)", saved.getHeldId(), siteId, saved.getItemCount());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HeldSaleSummaryDto> list() {
        Long siteId = TenantContext.requireSiteId();
        return repository.findBySiteIdOrderByCreationDateDesc(siteId).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public HeldSaleDto get(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(load(id));
        log.info("Held sale {} discarded", id);
    }

    /** Load a held sale, enforcing site scoping (cross-site access is not-found). */
    private HeldSale load(Long id) {
        Long siteId = TenantContext.requireSiteId();
        HeldSale h = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Held sale " + id + " not found"));
        if (!siteId.equals(h.getSiteId())) {
            throw new NotFoundException("Held sale " + id + " not found");
        }
        return h;
    }

    private HeldSaleDto toDto(HeldSale h) {
        return new HeldSaleDto(h.getHeldId(), h.getLabel(), h.getCustomerId(), h.getCustomerName(),
            h.getItemCount(), h.getTotalAmount(), h.getCartJson(), h.getCreatedBy(), h.getCreationDate());
    }

    private HeldSaleSummaryDto toSummary(HeldSale h) {
        return new HeldSaleSummaryDto(h.getHeldId(), h.getLabel(), h.getCustomerName(),
            h.getItemCount(), h.getTotalAmount(), h.getCreatedBy(), h.getCreationDate());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
