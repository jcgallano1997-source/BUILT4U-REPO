package com.built4u.pos.auth.dto;

import com.built4u.pos.user.User;

import java.util.Collection;
import java.util.List;

public record UserDto(
    Long id,
    String username,
    String fullName,
    String email,
    List<String> roles,
    List<String> modules,
    boolean mustChangePassword,
    boolean passwordExpired
) {
    public static UserDto from(User user) {
        return from(user, false, List.of());
    }

    public static UserDto from(User user, boolean passwordExpired) {
        return from(user, passwordExpired, List.of());
    }

    public static UserDto from(User user, boolean passwordExpired, Collection<String> modules) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRoles().stream().map(r -> r.getCode()).toList(),
            List.copyOf(modules),
            user.isMustChangePassword() || passwordExpired,
            passwordExpired
        );
    }
}
