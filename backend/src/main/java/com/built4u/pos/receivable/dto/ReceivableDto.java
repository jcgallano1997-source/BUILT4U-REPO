package com.built4u.pos.receivable.dto;

import com.built4u.pos.receivable.Receivable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReceivableDto(
    Long id,
    String salesNumber,
    Long customerId,
    String customerName,
    String modeCode,
    BigDecimal originalAmount,
    BigDecimal amountPaid,
    BigDecimal balance,
    LocalDate dueDate,
    String status,
    boolean overdue,
    LocalDateTime closedAt,
    LocalDateTime creationDate
) {
    public static ReceivableDto from(Receivable r, String customerName, LocalDate today) {
        boolean overdue = r.getBalance().signum() > 0
            && r.getDueDate().isBefore(today)
            && !"PAID".equals(r.getStatus())
            && !"CANCELLED".equals(r.getStatus());
        return new ReceivableDto(
            r.getId(), r.getSalesNumber(), r.getCustomerId(), customerName,
            r.getModeCode(), r.getOriginalAmount(), r.getAmountPaid(), r.getBalance(),
            r.getDueDate(), r.getStatus(), overdue, r.getClosedAt(), r.getCreationDate());
    }
}
