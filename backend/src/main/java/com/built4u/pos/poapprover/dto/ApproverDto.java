package com.built4u.pos.poapprover.dto;

/**
 * A user who may be picked as a PO approver. {@code builtIn} marks the business
 * owner, who is always eligible and cannot be removed from the pool.
 */
public record ApproverDto(
    Long userId,
    String username,
    String fullName,
    boolean builtIn
) {}
