package com.built4u.pos.stocktransferpolicy.dto;

import jakarta.validation.constraints.NotNull;

public record AddPolicyRequest(
    @NotNull Long sourceSiteId,
    @NotNull Long destSiteId
) {}
