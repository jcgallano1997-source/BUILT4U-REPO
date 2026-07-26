package com.built4u.pos.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserSummaryDto(
    Long id,
    String username,
    String fullName,
    String email,
    boolean active,
    boolean locked,
    boolean mustChangePassword,
    List<String> roleCodes,
    List<String> siteCodes,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {}
