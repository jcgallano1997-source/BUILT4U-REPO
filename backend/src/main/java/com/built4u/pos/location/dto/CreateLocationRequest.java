package com.built4u.pos.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLocationRequest(
    @NotBlank @Size(max = 100) String name,
    @PositiveOrZero            BigDecimal capacity
) {}
