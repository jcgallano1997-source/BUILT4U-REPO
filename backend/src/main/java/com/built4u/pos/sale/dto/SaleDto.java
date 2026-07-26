package com.built4u.pos.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleDto(
    String salesNumber,
    BigDecimal total,
    BigDecimal discountAll,
    BigDecimal totalDiscItem,
    BigDecimal grandTotal,
    BigDecimal payment,
    BigDecimal change,
    String modeOfPayment,
    Long customerId,
    String customerName,
    String reference,
    String status,
    int reprintCount,
    LocalDateTime creationDate,
    String createdBy,
    List<SaleLineDto> lines
) {}
