package com.built4u.pos.sale;

import com.built4u.pos.common.tenant.YesNoConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sale header ({@code pos_sale_header}); lines live in {@link SaleItem}. */
@Entity
@Table(name = "pos_sale_header")
@IdClass(SaleId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id @Column(name = "sales_number", nullable = false, length = 100)
    private String salesNumber;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "discount_all", nullable = false)
    @Builder.Default
    private BigDecimal discountAll = BigDecimal.ZERO;

    @Column(name = "total_disc_item", nullable = false)
    @Builder.Default
    private BigDecimal totalDiscItem = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal;

    @Column(nullable = false)
    private BigDecimal payment;

    @Column(name = "change_due", nullable = false)
    private BigDecimal changeDue;

    @Column(name = "mode_of_payment", nullable = false, length = 50)
    private String modeOfPayment;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(length = 100)
    private String voucher;

    @Column(length = 200)
    private String reference;

    @Column(name = "for_delivery", nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean forDelivery = false;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = SaleStatus.COMPLETED.name();

    @Column(name = "reprint_count", nullable = false)
    @Builder.Default
    private int reprintCount = 0;

    @Column(name = "points_redeemed", nullable = false)
    @Builder.Default
    private BigDecimal pointsRedeemed = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
