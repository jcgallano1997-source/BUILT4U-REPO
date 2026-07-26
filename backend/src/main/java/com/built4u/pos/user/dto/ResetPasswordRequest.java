package com.built4u.pos.user.dto;

import jakarta.validation.constraints.*;

/**
 * Admin-set password reset. Sets a new password and (by default) forces the user
 * to change it on next login. Also revokes all existing refresh tokens.
 */
public record ResetPasswordRequest(
    @NotBlank @Size(min = 8, max = 72)
    String newPassword,

    /** Force user to change on next login. Default true. */
    Boolean forceChangeOnNextLogin
) {}
