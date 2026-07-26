package com.built4u.pos.poapprover.dto;

/**
 * One row for the admin UI: every active user, plus their approver mapping if
 * any. {@code approverUserId} / {@code approverUsername} are null when the user
 * is on auto-approve (no mapping row exists).
 */
public record PoApproverDto(
    Long userId,
    String username,
    String fullName,
    Long approverUserId,
    String approverUsername,
    String approverFullName
) {}
