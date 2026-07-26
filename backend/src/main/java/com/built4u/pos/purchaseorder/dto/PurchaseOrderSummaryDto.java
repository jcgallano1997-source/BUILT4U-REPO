package com.built4u.pos.purchaseorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lightweight summary for the PO list view (no line items). */
public record PurchaseOrderSummaryDto(
    String poNumber,
    String supplier,
    String deliveryDate,
    String status,
    BigDecimal grandTotal,
    int lineCount,
    LocalDateTime creationDate,
    String createdBy
) {}
