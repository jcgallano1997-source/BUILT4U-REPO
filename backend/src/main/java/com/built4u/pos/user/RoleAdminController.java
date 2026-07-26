package com.built4u.pos.user;

import com.built4u.pos.user.dto.CreateRoleRequest;
import com.built4u.pos.user.dto.ModuleDto;
import com.built4u.pos.user.dto.RoleDetailDto;
import com.built4u.pos.user.dto.UpdateRoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_ROLES')")
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    @GetMapping
    public ResponseEntity<List<RoleDetailDto>> list() {
        return ResponseEntity.ok(roleAdminService.list());
    }

    @GetMapping("/_meta/modules")
    public ResponseEntity<List<ModuleDto>> modules() {
        return ResponseEntity.ok(roleAdminService.listModules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDetailDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(roleAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<RoleDetailDto> create(@Valid @RequestBody CreateRoleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleAdminService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDetailDto> update(@PathVariable("id") Long id,
                                                @Valid @RequestBody UpdateRoleRequest req) {
        return ResponseEntity.ok(roleAdminService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        roleAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
