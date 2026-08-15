package com.built4u.pos.sale;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One line of a sale ({@code pos_sale_item}). {@code unitCost} = selling price snapshot. */
@Entity
@Table(name = "pos_sale_item")
@IdClass(SaleItemId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItem {

    @Id @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id @Column(name = "sales_number", nullable = false, length = 100)
    private String salesNumber;

    @Id @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_desc", length = 300)
    private String itemDesc;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(length = 50)
    private String uom;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    /** Catalog selling price at sale time (snapshot). Equals {@code unitCost} when no price override. */
    @Column(name = "list_price")
    private BigDecimal listPrice;

    /** Reason for a price override or line discount (nullable when neither). */
    @Column(name = "override_reason", length = 255)
    private String overrideReason;

    /** Username that authorized the override/discount — the cashier if self-authorized, else the approving manager. */
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    /** Moving-average cost of goods per unit, snapshot at sale time (for stable margin reporting). */
    @Column(name = "unit_cogs")
    private BigDecimal unitCogs;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal adjustment = BigDecimal.ZERO;

    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
