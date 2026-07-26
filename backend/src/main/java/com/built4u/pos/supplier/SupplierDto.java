package com.built4u.pos.supplier;

import java.time.LocalDateTime;

public record SupplierDto(
    Long id,
    String code,
    String name,
    String address,
    String agentName,
    String contact,
    boolean active,
    LocalDateTime createdAt,
    String createdBy
) {
    public static SupplierDto from(Supplier s) {
        return new SupplierDto(s.getSupplierId(), s.getSupplierCode(), s.getSupplierName(), s.getSupplierAddress(),
            s.getAgentName(), s.getContact(), Boolean.TRUE.equals(s.getActive()), s.getCreationDate(), s.getCreatedBy());
    }
}
