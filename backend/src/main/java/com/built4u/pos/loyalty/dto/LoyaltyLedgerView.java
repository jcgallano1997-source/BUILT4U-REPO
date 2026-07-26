package com.built4u.pos.loyalty.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A page of a customer's points-ledger history plus a reconciliation summary.
 * {@code liveBalance} is the authoritative cached balance ({@code customer.points});
 * {@code ledgerSum} is Σ of every ledger entry.
 */
public record LoyaltyLedgerView(
    List<LedgerEntryDto> content,
    int number,
    int size,
    long totalElements,
    int totalPages,
    BigDecimal ledgerSum,
    BigDecimal liveBalance
) {}
