package com.built4u.pos.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerDto(
    Long id,
    String name,
    String contact,
    String address,
    String email,
    BigDecimal points,
    BigDecimal creditLimit,
    boolean active,
    LocalDateTime createdAt,
    String createdBy
) {
    public static CustomerDto from(Customer c) {
        return new CustomerDto(c.getCustomerId(), c.getCustomerName(), c.getContact(), c.getAddress(),
            c.getEmail(), c.getPoints(), c.getCreditLimit(), Boolean.TRUE.equals(c.getActive()),
            c.getCreationDate(), c.getCreatedBy());
    }
}
