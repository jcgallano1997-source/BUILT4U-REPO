package com.built4u.pos.item;

import com.built4u.pos.category.Category;
import com.built4u.pos.common.tenant.YesNoConverter;
import com.built4u.pos.location.Location;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inventory item, mapped to {@code pos_inventory}.
 *
 * <p>Column naming quirks (kept from the FreePOS lineage):
 *   {@code unit_cost} = SELLING price (exposed as {@code sellingPrice});
 *   {@code sup_price} = supplier/cost price (exposed as {@code costPrice}).
 * UOM is a denormalized string; {@link com.built4u.pos.uom.Uom} is just a lookup.
 */
@Entity
@Table(name = "pos_inventory")
@IdClass(ItemId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "item_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_inventory_seq")
    @SequenceGenerator(name = "pos_inventory_seq", sequenceName = "pos_inventory_seq", allocationSize = 1)
    private Long itemId;

    @Column(name = "barcode_id")
    private Long barcodeId;

    @Column(name = "cat_id", nullable = false)
    private Long catId;

    @Column(name = "loc_id", nullable = false)
    private Long locId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_desc", length = 300)
    private String itemDesc;

    /** Base unit — the item is stocked and sold in this unit. */
    @Column(nullable = false, length = 50)
    private String uom;

    /** Optional purchase (buying) unit label, e.g. BOX. Null = buy in the base unit. */
    @Column(name = "purchase_uom", length = 50)
    private String purchaseUom;

    /** Base units per purchase unit (e.g. 12 pcs per box). Null/≤0 = no conversion. */
    @Column(name = "pack_size")
    private BigDecimal packSize;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /** Selling price per unit. Maps to the {@code unit_cost} column. */
    @Column(name = "unit_cost", nullable = false)
    private BigDecimal sellingPrice;

    /** Latest supplier cost price. Maps to the {@code sup_price} column. */
    @Column(name = "sup_price")
    private BigDecimal costPrice;

    @Column
    private BigDecimal warning;

    @Column
    private BigDecimal critical;

    @Column(nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean active = true;

    /** Read-only join to Category (cat_id insert/update flows through {@link #catId}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "site_id", referencedColumnName = "site_id", insertable = false, updatable = false),
        @JoinColumn(name = "cat_id",  referencedColumnName = "cat_id",  insertable = false, updatable = false)
    })
    private Category category;

    /** Read-only join to Location (loc_id insert/update flows through {@link #locId}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "site_id", referencedColumnName = "site_id", insertable = false, updatable = false),
        @JoinColumn(name = "loc_id",  referencedColumnName = "loc_id",  insertable = false, updatable = false)
    })
    private Location location;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy
    @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;
}
