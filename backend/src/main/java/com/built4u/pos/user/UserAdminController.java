package com.built4u.pos.user;

import com.built4u.pos.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_USERS')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<List<UserSummaryDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(userAdminService.list(search, includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<UserDetailDto> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDetailDto> update(@PathVariable("id") Long id,
                                                @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userAdminService.update(id, req));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable("id") Long id,
                                              @Valid @RequestBody ResetPasswordRequest req) {
        userAdminService.resetPassword(id, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/_meta/roles")
    public ResponseEntity<List<RoleDto>> roles() {
        return ResponseEntity.ok(userAdminService.listRoles());
    }

    @GetMapping("/_meta/sites")
    public ResponseEntity<List<SiteRefDto>> sites() {
        return ResponseEntity.ok(userAdminService.listSites());
    }
}
