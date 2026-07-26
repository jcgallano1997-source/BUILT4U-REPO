package com.built4u.pos.purchaseorder;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One line item of a Purchase Order. Multiple rows share the same po_number.
 * Header-level fields (supplier, status, delivery_date, totals) are denormalized
 * onto every row, matching the FreePOS lineage. The service layer keeps them in
 * sync. Mapped to {@code pos_purchase_order}.
 */
@Entity
@Table(name = "pos_purchase_order")
@IdClass(PurchaseOrderItemId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "po_number", nullable = false, length = 100)
    private String poNumber;

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_desc", length = 300)
    private String itemDesc;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(length = 50)
    private String uom;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(length = 50)
    private String adjustment;

    @Column(name = "sub_total")
    private BigDecimal subTotal;

    @Column(name = "grand_total")
    private BigDecimal grandTotal;

    @Column(length = 200)
    private String supplier;

    /** Stringified date — matches FreePOS. Convention: yyyy-MM-dd. */
    @Column(name = "delivery_date", length = 100)
    private String deliveryDate;

    @Column(length = 200)
    private String remarks;

    @Column(nullable = false, length = 20)
    private String status;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
