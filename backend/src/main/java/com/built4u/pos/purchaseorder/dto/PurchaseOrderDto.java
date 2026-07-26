package com.built4u.pos.purchaseorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** "Header" view of a PO. Computed by aggregating its line-item rows. */
public record PurchaseOrderDto(
    String poNumber,
    String supplier,
    String deliveryDate,
    String remarks,
    String status,
    BigDecimal grandTotal,
    LocalDateTime creationDate,
    String createdBy,
    LocalDateTime approvedAt,
    String approvedBy,
    boolean autoApproved,
    /** Does the current request user have the right to approve this PO? */
    boolean canCurrentUserApprove,
    List<PurchaseOrderLineDto> lines
) {}
