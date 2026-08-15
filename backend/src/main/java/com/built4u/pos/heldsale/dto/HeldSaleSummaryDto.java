package com.built4u.pos.heldsale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Row in the "held sales" recall list (no cart payload — kept light). */
public record HeldSaleSummaryDto(
    Long heldId,
    String label,
    String customerName,
    Integer itemCount,
    BigDecimal totalAmount,
    String createdBy,
    LocalDateTime creationDate
) {}
