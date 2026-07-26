package com.built4u.pos.site.dto;

import java.time.LocalDateTime;

public record SiteSummaryDto(
    Long id,
    String code,
    String name,
    String address,
    boolean active,
    long userCount,
    LocalDateTime createdAt
) {}
