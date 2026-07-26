package com.built4u.pos.goodsreceipt;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One received line. Multiple lines share the same gr_number. po_number is
 * optional — GRs can be created against a PO (typical) or standalone (direct).
 * Mapped to {@code pos_goods_receipt}.
 */
@Entity
@Table(name = "pos_goods_receipt")
@IdClass(GoodsReceiptItemId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItem {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "gr_number", nullable = false, length = 100)
    private String grNumber;

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_desc", length = 300)
    private String itemDesc;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(length = 50)
    private String uom;

    @Column(length = 200)
    private String reference;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(length = 200)
    private String remarks;

    @Column(name = "sup_price")
    private BigDecimal supPrice;

    @Column(name = "sub_total")
    private BigDecimal subTotal;

    @Column(length = 100)
    private String supplier;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
