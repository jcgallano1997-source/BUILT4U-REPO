package com.built4u.pos.receivable.dto;

import com.built4u.pos.receivable.ReceivablePayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceivablePaymentDto(
    Long id,
    BigDecimal amount,
    String note,
    LocalDateTime paidAt,
    String createdBy
) {
    public static ReceivablePaymentDto from(ReceivablePayment p) {
        return new ReceivablePaymentDto(
            p.getId(), p.getAmount(), p.getNote(), p.getPaidAt(), p.getCreatedBy());
    }
}
