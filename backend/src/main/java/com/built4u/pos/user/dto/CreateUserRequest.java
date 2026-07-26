package com.built4u.pos.user.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateUserRequest(
    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, digits, dot, dash, underscore")
    String username,

    @NotBlank @Size(min = 1, max = 150)
    String fullName,

    @Email @Size(max = 255)
    String email,

    @NotBlank @Size(min = 8, max = 72)
    String initialPassword,

    /** When true, user must change the password on first login. Default true. */
    Boolean forceChangeOnFirstLogin,

    @NotEmpty
    List<@NotBlank String> roleCodes,

    @NotEmpty
    List<@NotBlank String> siteCodes
) {}
