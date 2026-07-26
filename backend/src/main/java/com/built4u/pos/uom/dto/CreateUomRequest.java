package com.built4u.pos.uom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUomRequest(@NotBlank @Size(max = 50) String uom) {}
