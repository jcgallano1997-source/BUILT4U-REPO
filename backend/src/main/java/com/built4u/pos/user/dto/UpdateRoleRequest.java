package com.built4u.pos.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Code is immutable after creation, so it is not part of the update request. */
public record UpdateRoleRequest(
    @NotBlank @Size(max = 80) String name,
    @Size(max = 255) String description,
    @NotEmpty(message = "Select at least one module") List<@NotBlank String> moduleCodes
) {}
