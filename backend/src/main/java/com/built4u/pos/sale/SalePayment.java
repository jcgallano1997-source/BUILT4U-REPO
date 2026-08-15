package com.built4u.pos.sale;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One tender applied to a sale ({@code pos_sale_payment}). {@code amount} is the
 * APPLIED value — the sum of a sale's amounts equals its grand total (cash change
 * excluded). {@code tendered} is what was handed over (cash may exceed applied).
 */
@Entity
@Table(name = "pos_sale_payment")
@IdClass(SalePaymentId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePayment {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "sales_number", nullable = false, length = 100)
    private String salesNumber;

    @Id
    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "pay_mode", nullable = false, length = 50)
    private String mode;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal tendered = BigDecimal.ZERO;

    @Column(name = "change_due", nullable = false)
    @Builder.Default
    private BigDecimal changeDue = BigDecimal.ZERO;

    @Column(length = 200)
    private String reference;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;
}
