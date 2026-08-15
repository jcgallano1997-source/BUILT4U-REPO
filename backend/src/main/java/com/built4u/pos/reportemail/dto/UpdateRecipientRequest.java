package com.built4u.pos.reportemail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateRecipientRequest(
    @Size(max = 100) String label,
    @Email @Size(max = 255) String recipientEmail,
    @Size(max = 255) String subject,
    @Size(max = 2000) String body
) {}
