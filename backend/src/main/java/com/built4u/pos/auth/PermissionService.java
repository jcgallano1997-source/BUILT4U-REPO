package com.built4u.pos.auth;

import com.built4u.pos.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Permission helper.
 *
 * <p>Single-business: effective modules = role grants only (no plan
 * entitlement intersection). Used to (a) compute a user's effective module set
 * for {@code UserDto}/login responses, and (b) back server-side guards.
 */
@Service("perm")
public class PermissionService {

    /**
     * Effective modules for a user — the same set the JWT {@code modules} claim
     * resolves to. Wildcard (ADMIN) role = every catalog module; non-wildcard =
     * union of role module codes.
     *
     * <p>Must be called within an open transaction (LAZY {@code moduleCodes}).
     */
    public Set<String> effectiveModules(User user) {
        boolean wildcard = user.getRoles().stream().anyMatch(r -> r.isWildcard());
        if (wildcard) {
            return new TreeSet<>(Modules.ALL);
        }
        Set<String> roleModules = new LinkedHashSet<>();
        user.getRoles().forEach(r -> roleModules.addAll(r.getModuleCodes()));
        return roleModules;
    }

    /** True if the current request's principal holds the given module. */
    public boolean has(String moduleCode) {
        return hasAuthority("MOD_" + moduleCode);
    }

    /** True if the current request's principal holds ANY of the given modules. */
    public boolean any(String... moduleCodes) {
        for (String code : moduleCodes) {
            if (hasAuthority("MOD_" + code)) return true;
        }
        return false;
    }

    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (authority.equals(a.getAuthority())) return true;
        }
        return false;
    }
}
