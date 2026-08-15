package com.built4u.pos.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Small auth helpers shared across admin services. */
public final class AuthUtils {

    private AuthUtils() {}

    /**
     * True when the current request is authenticated as an IT superuser (the
     * wildcard {@code ADMIN} role → {@code ROLE_ADMIN} authority). Used to keep
     * admin-only accounts, roles, and modules invisible to everyone else.
     */
    public static boolean isCurrentUserAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
            .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
    }
}
