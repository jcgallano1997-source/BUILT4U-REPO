package com.built4u.pos.payable.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Manually-entered expense → becomes a payable with {@code source=EXPENSE}.
 * No PO/GR/supplier_id; the user types the payee (utility name, person, etc.)
 * and a category (free-text with a suggested datalist in the UI).
 */
public record CreateExpenseRequest(
    @Size(max = 60) String category,
    @NotBlank @Size(max = 150) String payeeName,
    @Size(max = 300) String description,
    @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal amount,
    @NotNull LocalDate dueDate
) {}
