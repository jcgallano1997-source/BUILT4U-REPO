package com.built4u.pos.shift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashMovementDto(
    Long movementId,
    String direction,
    BigDecimal amount,
    String reason,
    String createdBy,
    LocalDateTime creationDate
) {}
