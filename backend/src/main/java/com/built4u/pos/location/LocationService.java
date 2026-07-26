package com.built4u.pos.location;

import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.location.dto.CreateLocationRequest;
import com.built4u.pos.location.dto.UpdateLocationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<LocationDto> list(String search, boolean includeInactive) {
        Long siteId = TenantContext.requireSiteId();
        String needle = search == null ? "" : search.toLowerCase().trim();
        return locationRepository.findBySiteIdOrderByLocationAsc(siteId).stream()
            .filter(l -> includeInactive || Boolean.TRUE.equals(l.getActive()))
            .filter(l -> needle.isEmpty() || l.getLocation().toLowerCase().contains(needle))
            .map(LocationDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto get(Long locId) {
        Long siteId = TenantContext.requireSiteId();
        return locationRepository.findBySiteIdAndLocId(siteId, locId)
            .map(LocationDto::from)
            .orElseThrow(() -> new NotFoundException("Location " + locId + " not found"));
    }

    @Transactional
    public LocationDto create(CreateLocationRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String name = req.name().trim();
        if (locationRepository.existsByName(siteId, name, null)) {
            throw new ConflictException("Location '" + name + "' already exists at this site");
        }
        Location entity = Location.builder()
            .siteId(siteId).location(name).capacity(req.capacity()).active(true).build();
        return LocationDto.from(locationRepository.save(entity));
    }

    @Transactional
    public LocationDto update(Long locId, UpdateLocationRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Location l = locationRepository.findBySiteIdAndLocId(siteId, locId)
            .orElseThrow(() -> new NotFoundException("Location " + locId + " not found"));
        String name = req.name().trim();
        if (locationRepository.existsByName(siteId, name, locId)) {
            throw new ConflictException("Location '" + name + "' already exists at this site");
        }
        l.setLocation(name);
        l.setCapacity(req.capacity());
        l.setActive(Boolean.TRUE.equals(req.active()));
        return LocationDto.from(locationRepository.save(l));
    }

    @Transactional
    public void softDelete(Long locId) {
        Long siteId = TenantContext.requireSiteId();
        Location l = locationRepository.findBySiteIdAndLocId(siteId, locId)
            .orElseThrow(() -> new NotFoundException("Location " + locId + " not found"));
        l.setActive(false);
        locationRepository.save(l);
    }
}
