package com.built4u.pos.payable.dto;

import java.util.List;

/** A payable plus its disbursement history (newest first). */
public record PayableDetailDto(
    PayableDto payable,
    List<PayablePaymentDto> payments
) {}
