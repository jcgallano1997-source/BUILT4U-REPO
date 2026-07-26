package com.built4u.pos.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSiteRequest(
    @NotBlank @Size(min = 2, max = 20)
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Site code must be uppercase letters, digits, dash, or underscore only")
    String code,

    @NotBlank @Size(min = 1, max = 100)
    String name,

    @Size(max = 500)
    String address
) {}
