package com.built4u.pos.user.dto;

public record ModuleDto(
    String code,
    String name,
    String description,
    Integer sortOrder
) {}
