package com.built4u.pos.poapprover.dto;

/**
 * PUT body for the admin UI. {@code approverUserId = null} clears the mapping
 * (= reverts that user to auto-approve).
 */
public record UpdatePoApproverRequest(Long approverUserId) {}
