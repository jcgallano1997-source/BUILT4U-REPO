package com.built4u.pos.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(
    @NotBlank @Size(max = 20)  String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 250)           String address,
    @Size(max = 100)           String agentName,
    @Size(max = 100)           String contact,
    boolean                    apEnabled,
    @PositiveOrZero            Integer payableDays,
    @NotNull                   Boolean active
) {}
