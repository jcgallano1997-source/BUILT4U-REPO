package com.built4u.pos.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateLocationRequest(
    @NotBlank @Size(max = 100) String name,
    @PositiveOrZero            BigDecimal capacity,
    @NotNull                   Boolean active
) {}
