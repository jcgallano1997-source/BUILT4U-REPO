package com.built4u.pos.location;

import com.built4u.pos.location.dto.CreateLocationRequest;
import com.built4u.pos.location.dto.UpdateLocationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    private static final String READ_ANY =
        "hasAnyAuthority('MOD_LOCATIONS','MOD_INVENTORY','MOD_STOCKTAKE'," +
        "'MOD_INVENTORY_SNAPSHOT','MOD_INVENTORY_VALUATION','MOD_INVENTORY_MOVEMENT')";

    @GetMapping
    @PreAuthorize(READ_ANY)
    public ResponseEntity<List<LocationDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(locationService.list(search, includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ANY)
    public ResponseEntity<LocationDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(locationService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_LOCATIONS')")
    public ResponseEntity<LocationDto> create(@Valid @RequestBody CreateLocationRequest req) {
        return ResponseEntity.ok(locationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_LOCATIONS')")
    public ResponseEntity<LocationDto> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateLocationRequest req) {
        return ResponseEntity.ok(locationService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_LOCATIONS')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        locationService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
