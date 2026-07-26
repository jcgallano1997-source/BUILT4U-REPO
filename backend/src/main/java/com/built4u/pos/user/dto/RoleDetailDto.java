package com.built4u.pos.user.dto;

import java.util.List;

public record RoleDetailDto(
    Long id,
    String code,
    String name,
    String description,
    boolean builtIn,
    boolean wildcard,
    List<String> moduleCodes
) {}
