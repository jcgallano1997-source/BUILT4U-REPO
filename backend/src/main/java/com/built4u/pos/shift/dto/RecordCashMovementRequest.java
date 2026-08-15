package com.built4u.pos.shift.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Record a mid-shift cash drawer movement. {@code direction} is IN or OUT. */
public record RecordCashMovementRequest(
    @NotBlank @Pattern(regexp = "IN|OUT", message = "direction must be IN or OUT") String direction,
    @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal amount,
    @Size(max = 255) String reason
) {}
