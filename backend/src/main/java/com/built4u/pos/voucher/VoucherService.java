package com.built4u.pos.voucher;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.voucher.dto.SaveVoucherRequest;
import com.built4u.pos.voucher.dto.VoucherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Admin CRUD for vouchers (behind {@code MOD_VOUCHERS}). Site = current context. */
@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final VoucherRepository repo;

    @Transactional(readOnly = true)
    public Page<VoucherDto> list(String search, boolean includeInactive, Pageable pageable) {
        Long siteId = TenantContext.requireSiteId();
        String pattern = search == null || search.isBlank()
            ? "%" : "%" + search.toLowerCase().trim() + "%";
        return repo.search(siteId, pattern, includeInactive, pageable).map(VoucherDto::from);
    }

    @Transactional(readOnly = true)
    public VoucherDto get(Long id) {
        Long siteId = TenantContext.requireSiteId();
        return repo.findBySiteIdAndId(siteId, id).map(VoucherDto::from)
            .orElseThrow(() -> new NotFoundException("Voucher " + id + " not found"));
    }

    @Transactional
    public VoucherDto create(SaveVoucherRequest r) {
        Long siteId = TenantContext.requireSiteId();
        String code = r.code().trim();
        validate(r);
        repo.findBySiteIdAndCodeIgnoreCase(siteId, code).ifPresent(v -> {
            throw new ConflictException("Voucher code '" + code + "' already exists at this site");
        });
        Voucher v = Voucher.builder()
            .siteId(siteId)
            .code(code)
            .description(blankToNull(r.description()))
            .discountType(r.discountType())
            .discountValue(r.discountValue())
            .maxDiscount("PERCENT".equals(r.discountType()) ? r.maxDiscount() : null)
            .minSpend(r.minSpend())
            .validFrom(r.validFrom())
            .validTo(r.validTo())
            .usageLimit(r.usageLimit())
            .active(r.active())
            .build();
        return VoucherDto.from(repo.save(v));
    }

    @Transactional
    public VoucherDto update(Long id, SaveVoucherRequest r) {
        Long siteId = TenantContext.requireSiteId();
        Voucher v = repo.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Voucher " + id + " not found"));
        String code = r.code().trim();
        validate(r);
        repo.findBySiteIdAndCodeIgnoreCase(siteId, code)
            .filter(other -> !other.getId().equals(id))
            .ifPresent(o -> {
                throw new ConflictException("Voucher code '" + code + "' already exists at this site");
            });
        v.setCode(code);
        v.setDescription(blankToNull(r.description()));
        v.setDiscountType(r.discountType());
        v.setDiscountValue(r.discountValue());
        v.setMaxDiscount("PERCENT".equals(r.discountType()) ? r.maxDiscount() : null);
        v.setMinSpend(r.minSpend());
        v.setValidFrom(r.validFrom());
        v.setValidTo(r.validTo());
        v.setUsageLimit(r.usageLimit());
        v.setActive(r.active());
        return VoucherDto.from(repo.save(v));
    }

    @Transactional
    public void softDelete(Long id) {
        Long siteId = TenantContext.requireSiteId();
        Voucher v = repo.findBySiteIdAndId(siteId, id)
            .orElseThrow(() -> new NotFoundException("Voucher " + id + " not found"));
        v.setActive(false);
        repo.save(v);
    }

    private void validate(SaveVoucherRequest r) {
        if ("PERCENT".equals(r.discountType()) && r.discountValue().compareTo(HUNDRED) > 0) {
            throw new BadRequestException("A PERCENT voucher cannot exceed 100%.");
        }
        if (r.validFrom() != null && r.validTo() != null && r.validFrom().isAfter(r.validTo())) {
            throw new BadRequestException("Valid-from date must be on or before valid-to.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
