package com.built4u.pos.auth.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    UserDto user,
    SiteDto site
) {}
