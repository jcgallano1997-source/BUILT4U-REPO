package com.built4u.pos.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCustomerRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50)            String contact,
    @Size(max = 200)           String address,
    @Email @Size(max = 120)    String email,
    @PositiveOrZero            BigDecimal points
) {}
