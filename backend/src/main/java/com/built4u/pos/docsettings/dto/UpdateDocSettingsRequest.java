package com.built4u.pos.docsettings.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDocSettingsRequest(
    @Size(max = 150) String businessName,
    @Size(max = 255) String addressLine,
    @Size(max = 150) String contactLine,
    @Size(max = 40)  String tin,
    @Size(max = 255) String footerNote,
    @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "accentColor must be a #RRGGBB hex colour") String accentColor,
    @Size(max = 60)  String receiptTitle,
    @Size(max = 255) String receiptFooter
) {}
