package com.built4u.pos.reportemail.dto;

/**
 * A user who can be picked as a report recipient. {@code email} is null when the
 * user has no address on file — the UI shows those but won't let them be chosen,
 * since they'd be silently skipped at send time.
 */
public record RecipientUserDto(
    Long userId,
    String username,
    String fullName,
    String email
) {}
