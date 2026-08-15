package com.built4u.pos.heldsale.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Payload to park an in-progress cart. {@code cartJson} is the register's own
 *  serialized cart; the rest is metadata for the recall list. */
public record SaveHeldSaleRequest(
    String label,
    Long customerId,
    String customerName,
    Integer itemCount,
    BigDecimal totalAmount,
    @NotBlank(message = "cartJson is required") String cartJson
) {}
