package com.built4u.pos.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummaryDto(
    String salesNumber,
    BigDecimal grandTotal,
    String modeOfPayment,
    String customerName,
    int lineCount,
    String status,
    LocalDateTime creationDate,
    String createdBy
) {}
