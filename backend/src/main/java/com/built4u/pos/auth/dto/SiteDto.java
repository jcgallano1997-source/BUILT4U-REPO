package com.built4u.pos.auth.dto;

import com.built4u.pos.site.Site;

public record SiteDto(Long id, String code, String name) {
    public static SiteDto from(Site site) {
        return new SiteDto(site.getId(), site.getCode(), site.getName());
    }
}
