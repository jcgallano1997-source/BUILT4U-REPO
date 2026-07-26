package com.built4u.pos.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(@NotBlank @Size(max = 100) String name) {}
