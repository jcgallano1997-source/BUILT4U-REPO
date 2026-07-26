package com.built4u.pos.voucher.dto;

import com.built4u.pos.voucher.Voucher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VoucherDto(
    Long id,
    String code,
    String description,
    String discountType,
    BigDecimal discountValue,
    BigDecimal maxDiscount,
    BigDecimal minSpend,
    LocalDate validFrom,
    LocalDate validTo,
    Integer usageLimit,
    int usedCount,
    boolean active,
    String createdBy,
    LocalDateTime lastUpdateDate
) {
    public static VoucherDto from(Voucher v) {
        return new VoucherDto(
            v.getId(), v.getCode(), v.getDescription(), v.getDiscountType(),
            v.getDiscountValue(), v.getMaxDiscount(), v.getMinSpend(),
            v.getValidFrom(), v.getValidTo(), v.getUsageLimit(), v.getUsedCount(),
            v.isActive(), v.getCreatedBy(), v.getLastUpdateDate());
    }
}
