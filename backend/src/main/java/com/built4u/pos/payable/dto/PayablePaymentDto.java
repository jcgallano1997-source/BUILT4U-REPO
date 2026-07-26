package com.built4u.pos.payable.dto;

import com.built4u.pos.payable.PayablePayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayablePaymentDto(
    Long id,
    BigDecimal amount,
    String note,
    LocalDateTime paidAt,
    String createdBy
) {
    public static PayablePaymentDto from(PayablePayment p) {
        return new PayablePaymentDto(
            p.getId(), p.getAmount(), p.getNote(), p.getPaidAt(), p.getCreatedBy());
    }
}
