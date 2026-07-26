package com.built4u.pos.supplier;

import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.supplier.dto.CreateSupplierRequest;
import com.built4u.pos.supplier.dto.UpdateSupplierRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierDto> list(String search, boolean includeInactive) {
        Long siteId = TenantContext.requireSiteId();
        String needle = search == null ? "" : search.toLowerCase().trim();
        return supplierRepository.findBySiteIdOrderBySupplierNameAsc(siteId).stream()
            .filter(s -> includeInactive || Boolean.TRUE.equals(s.getActive()))
            .filter(s -> needle.isEmpty()
                || s.getSupplierName().toLowerCase().contains(needle)
                || s.getSupplierCode().toLowerCase().contains(needle)
                || (s.getContact() != null && s.getContact().toLowerCase().contains(needle)))
            .map(SupplierDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SupplierDto get(Long supplierId) {
        Long siteId = TenantContext.requireSiteId();
        return supplierRepository.findBySiteIdAndSupplierId(siteId, supplierId)
            .map(SupplierDto::from)
            .orElseThrow(() -> new NotFoundException("Supplier " + supplierId + " not found"));
    }

    @Transactional
    public SupplierDto create(CreateSupplierRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String code = req.code().trim();
        if (supplierRepository.existsByCode(siteId, code, null)) {
            throw new ConflictException("Supplier code '" + code + "' is already in use at this site");
        }
        Supplier entity = Supplier.builder()
            .siteId(siteId).supplierCode(code).supplierName(req.name().trim())
            .supplierAddress(blankToNull(req.address())).agentName(blankToNull(req.agentName()))
            .contact(blankToNull(req.contact())).active(true).build();
        return SupplierDto.from(supplierRepository.save(entity));
    }

    @Transactional
    public SupplierDto update(Long supplierId, UpdateSupplierRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Supplier s = supplierRepository.findBySiteIdAndSupplierId(siteId, supplierId)
            .orElseThrow(() -> new NotFoundException("Supplier " + supplierId + " not found"));
        String code = req.code().trim();
        if (supplierRepository.existsByCode(siteId, code, supplierId)) {
            throw new ConflictException("Supplier code '" + code + "' is already in use at this site");
        }
        s.setSupplierCode(code);
        s.setSupplierName(req.name().trim());
        s.setSupplierAddress(blankToNull(req.address()));
        s.setAgentName(blankToNull(req.agentName()));
        s.setContact(blankToNull(req.contact()));
        s.setActive(Boolean.TRUE.equals(req.active()));
        return SupplierDto.from(supplierRepository.save(s));
    }

    @Transactional
    public void softDelete(Long supplierId) {
        Long siteId = TenantContext.requireSiteId();
        Supplier s = supplierRepository.findBySiteIdAndSupplierId(siteId, supplierId)
            .orElseThrow(() -> new NotFoundException("Supplier " + supplierId + " not found"));
        s.setActive(false);
        supplierRepository.save(s);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
