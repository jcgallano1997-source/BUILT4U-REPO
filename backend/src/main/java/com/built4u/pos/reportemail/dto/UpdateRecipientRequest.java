package com.built4u.pos.reportemail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * {@code userIds} are the users the report is emailed to; {@code recipientEmail}
 * is an optional extra address for someone with no user account. Both may be set
 * — the report goes to all of them.
 */
public record UpdateRecipientRequest(
    @Size(max = 100) String label,
    @Email @Size(max = 255) String recipientEmail,
    @Size(max = 255) String subject,
    @Size(max = 2000) String body,
    List<Long> userIds
) {}
