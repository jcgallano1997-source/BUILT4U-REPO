package com.built4u.pos.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Update an existing site. Site code is immutable (it's referenced in JWTs,
 * refresh-token bindings, and all site_id-keyed data). To "rename" a code,
 * deactivate the old + create a new one and reassign users.
 */
public record UpdateSiteRequest(
    @NotBlank @Size(min = 1, max = 100)
    String name,

    @Size(max = 500)
    String address,

    @NotNull
    Boolean active
) {}
