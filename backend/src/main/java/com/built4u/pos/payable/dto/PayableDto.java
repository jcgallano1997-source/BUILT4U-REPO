package com.built4u.pos.payable.dto;

import com.built4u.pos.payable.Payable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PayableDto(
    Long id,
    String source,
    String category,
    String poNumber,
    String grNumber,
    Long supplierId,
    String payeeName,
    String description,
    BigDecimal originalAmount,
    BigDecimal amountPaid,
    BigDecimal balance,
    LocalDate dueDate,
    String status,
    boolean overdue,
    LocalDateTime closedAt,
    LocalDateTime creationDate
) {
    public static PayableDto from(Payable p, LocalDate today) {
        boolean overdue = p.getBalance() != null
            && p.getBalance().signum() > 0
            && p.getDueDate() != null
            && p.getDueDate().isBefore(today)
            && !"PAID".equals(p.getStatus())
            && !"CANCELLED".equals(p.getStatus());
        return new PayableDto(
            p.getId(), p.getSource(), p.getCategory(), p.getPoNumber(), p.getGrNumber(),
            p.getSupplierId(), p.getPayeeName(), p.getDescription(),
            p.getOriginalAmount(), p.getAmountPaid(), p.getBalance(),
            p.getDueDate(), p.getStatus(), overdue,
            p.getClosedAt(), p.getCreationDate());
    }
}
