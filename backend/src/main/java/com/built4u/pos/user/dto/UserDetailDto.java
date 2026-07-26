package com.built4u.pos.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Full user record (no password hash). Used by GET /api/admin/users/{id}. */
public record UserDetailDto(
    Long id,
    String username,
    String fullName,
    String email,
    boolean active,
    boolean locked,
    boolean mustChangePassword,
    int failedAttempts,
    LocalDateTime lockedUntil,
    LocalDateTime passwordChangedAt,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy,
    List<RoleDto> roles,
    List<SiteRefDto> sites
) {}
