package com.built4u.pos.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoleRequest(
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Z0-9_]+$",
        message = "Code must be UPPERCASE letters, digits, or underscore")
    String code,
    @NotBlank @Size(max = 80) String name,
    @Size(max = 255) String description,
    @NotEmpty(message = "Select at least one module") List<@NotBlank String> moduleCodes
) {}
