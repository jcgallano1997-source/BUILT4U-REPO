package com.built4u.pos.sale;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One refunded line ({@code pos_return_item}). Rows sharing a {@code returnNumber}
 * form one refund; each points back to the original {@code salesNumber}. Voids do
 * NOT write rows here — they only flip {@code Sale.status} to VOIDED.
 */
@Entity
@Table(name = "pos_return_item")
@IdClass(ReturnItemId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id @Column(name = "return_number", nullable = false, length = 100)
    private String returnNumber;

    @Id @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "sales_number", nullable = false, length = 100)
    private String salesNumber;

    @Column(name = "item_desc", length = 300)
    private String itemDesc;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(length = 50)
    private String uom;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal;

    @Column(length = 300)
    private String reason;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
