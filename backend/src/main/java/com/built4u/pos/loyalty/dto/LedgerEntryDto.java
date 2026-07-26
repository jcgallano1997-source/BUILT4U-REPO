package com.built4u.pos.loyalty.dto;

import com.built4u.pos.loyalty.LoyaltyLedger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LedgerEntryDto(
    Long id,
    String entryType,
    BigDecimal points,
    String salesNumber,
    String note,
    LocalDate expiresAt,
    boolean expired,
    LocalDateTime creationDate,
    String createdBy
) {
    public static LedgerEntryDto from(LoyaltyLedger l) {
        return new LedgerEntryDto(
            l.getId(), l.getEntryType(), l.getPoints(), l.getSalesNumber(),
            l.getNote(), l.getExpiresAt(), l.isExpired(), l.getCreationDate(), l.getCreatedBy());
    }
}
