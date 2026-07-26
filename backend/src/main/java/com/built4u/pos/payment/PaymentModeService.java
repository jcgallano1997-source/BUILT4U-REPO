package com.built4u.pos.payment;

import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.payment.dto.PaymentModeDto;
import com.built4u.pos.payment.dto.SavePaymentModeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentModeService {

    private final PaymentModeRepository repository;

    /** POS-facing: the active modes for the current site. */
    @Transactional(readOnly = true)
    public List<PaymentModeDto> resolveActive() {
        Long siteId = TenantContext.requireSiteId();
        return repository.findBySiteIdAndActiveTrueOrderBySortOrderAscCodeAsc(siteId).stream()
            .map(PaymentModeDto::from).toList();
    }

    /** Admin: full catalog for the current site. */
    @Transactional(readOnly = true)
    public List<PaymentModeDto> list() {
        Long siteId = TenantContext.requireSiteId();
        return repository.findBySiteIdOrderBySortOrderAscCodeAsc(siteId).stream()
            .map(PaymentModeDto::from).toList();
    }

    /** Resolve an active mode by code for the current site (used by checkout). */
    @Transactional(readOnly = true)
    public Optional<PaymentMode> findActiveByCode(Long siteId, String code) {
        return repository.findBySiteIdAndActiveTrueOrderBySortOrderAscCodeAsc(siteId).stream()
            .filter(m -> m.getCode().equalsIgnoreCase(code)).findFirst();
    }

    @Transactional
    public PaymentModeDto create(SavePaymentModeRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String code = req.code().trim();
        if (repository.existsByCode(siteId, code, null)) {
            throw new ConflictException("Payment mode '" + code + "' already exists at this site");
        }
        PaymentMode m = apply(new PaymentMode(), siteId, req, code);
        return PaymentModeDto.from(repository.save(m));
    }

    @Transactional
    public PaymentModeDto update(Long id, SavePaymentModeRequest req) {
        Long siteId = TenantContext.requireSiteId();
        PaymentMode m = repository.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Payment mode " + id + " not found"));
        String code = req.code().trim();
        if (repository.existsByCode(siteId, code, id)) {
            throw new ConflictException("Payment mode '" + code + "' already exists at this site");
        }
        return PaymentModeDto.from(repository.save(apply(m, siteId, req, code)));
    }

    @Transactional
    public void delete(Long id) {
        Long siteId = TenantContext.requireSiteId();
        PaymentMode m = repository.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Payment mode " + id + " not found"));
        repository.delete(m);
    }

    private static PaymentMode apply(PaymentMode m, Long siteId, SavePaymentModeRequest req, String code) {
        m.setSiteId(siteId);
        m.setCode(code);
        m.setLabel(req.label().trim());
        m.setSurchargeType(req.surchargeType());
        m.setSurchargeValue(req.surchargeValue());
        m.setCash(req.isCash());
        m.setAllowsPartial(req.allowsPartial());
        m.setCustomerRequired(req.customerRequired());
        m.setAccountsReceivable(req.accountsReceivable());
        m.setArDueDays(req.arDueDays());
        m.setSortOrder(req.sortOrder());
        m.setActive(req.active());
        return m;
    }
}
