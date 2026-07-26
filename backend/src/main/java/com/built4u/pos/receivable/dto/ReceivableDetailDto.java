package com.built4u.pos.receivable.dto;

import java.util.List;

/** A receivable plus its collection history (newest first). */
public record ReceivableDetailDto(
    ReceivableDto receivable,
    List<ReceivablePaymentDto> payments
) {}
