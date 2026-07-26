package com.built4u.pos.voucher.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update payload for a voucher. Cross-field rules (PERCENT ≤ 100,
 * validFrom ≤ validTo, code unique per site) are enforced in the service.
 */
public record SaveVoucherRequest(
    @NotBlank @Size(max = 40) String code,
    @Size(max = 150) String description,
    @NotBlank @Pattern(regexp = "FIXED|PERCENT") String discountType,
    @NotNull @Positive BigDecimal discountValue,
    @PositiveOrZero BigDecimal maxDiscount,
    @PositiveOrZero BigDecimal minSpend,
    LocalDate validFrom,
    LocalDate validTo,
    @Positive Integer usageLimit,
    boolean active
) {}
