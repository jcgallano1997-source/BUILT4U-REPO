package com.built4u.pos.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Single-business login: no tenant/business handle — just username + site + password. */
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String siteCode
) {}
