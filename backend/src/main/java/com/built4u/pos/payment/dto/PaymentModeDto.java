package com.built4u.pos.payment.dto;

import com.built4u.pos.payment.PaymentMode;

import java.math.BigDecimal;

public record PaymentModeDto(
    Long id,
    String code,
    String label,
    String surchargeType,
    BigDecimal surchargeValue,
    boolean isCash,
    boolean allowsPartial,
    boolean customerRequired,
    boolean accountsReceivable,
    int arDueDays,
    int sortOrder,
    boolean active
) {
    public static PaymentModeDto from(PaymentMode m) {
        return new PaymentModeDto(m.getId(), m.getCode(), m.getLabel(), m.getSurchargeType(),
            m.getSurchargeValue(), m.isCash(), m.isAllowsPartial(), m.isCustomerRequired(),
            m.isAccountsReceivable(), m.getArDueDays(), m.getSortOrder(), m.isActive());
    }
}
