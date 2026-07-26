package com.built4u.pos.uom;

import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.uom.dto.CreateUomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UomService {

    private final UomRepository uomRepository;

    @Transactional(readOnly = true)
    public List<UomDto> list(boolean includeInactive) {
        Long siteId = TenantContext.requireSiteId();
        List<Uom> rows = includeInactive
            ? uomRepository.findBySiteIdOrderByUomAsc(siteId)
            : uomRepository.findBySiteIdAndActiveTrueOrderByUomAsc(siteId);
        return rows.stream().map(UomDto::from).toList();
    }

    @Transactional
    public UomDto create(CreateUomRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String name = req.uom().trim();
        if (uomRepository.findBySiteIdAndUom(siteId, name).isPresent()) {
            throw new ConflictException("UOM '" + name + "' already exists at this site");
        }
        Uom entity = Uom.builder().siteId(siteId).uom(name).active(true).build();
        return UomDto.from(uomRepository.save(entity));
    }

    /** Update only toggles active — the UOM string is the natural PK and can't be renamed in place. */
    @Transactional
    public UomDto setActive(String uom, boolean active) {
        Long siteId = TenantContext.requireSiteId();
        Uom u = uomRepository.findBySiteIdAndUom(siteId, uom)
            .orElseThrow(() -> new NotFoundException("UOM '" + uom + "' not found"));
        u.setActive(active);
        return UomDto.from(uomRepository.save(u));
    }

    @Transactional
    public void softDelete(String uom) {
        setActive(uom, false);
    }
}
