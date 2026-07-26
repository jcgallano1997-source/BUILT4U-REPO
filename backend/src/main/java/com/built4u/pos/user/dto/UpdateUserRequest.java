package com.built4u.pos.user.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Update an existing user's profile + roles + sites + active state.
 * Username cannot be changed (it's the natural identifier).
 * Password changes go through the dedicated /reset-password endpoint.
 */
public record UpdateUserRequest(
    @NotBlank @Size(min = 1, max = 150)
    String fullName,

    @Email @Size(max = 255)
    String email,

    @NotNull
    Boolean active,

    @NotEmpty
    List<@NotBlank String> roleCodes,

    @NotEmpty
    List<@NotBlank String> siteCodes
) {}
