package com.built4u.pos.heldsale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Full held sale, including the serialized cart, used when recalling one. */
public record HeldSaleDto(
    Long heldId,
    String label,
    Long customerId,
    String customerName,
    Integer itemCount,
    BigDecimal totalAmount,
    String cartJson,
    String createdBy,
    LocalDateTime creationDate
) {}
