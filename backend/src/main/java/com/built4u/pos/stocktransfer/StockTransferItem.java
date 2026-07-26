package com.built4u.pos.stocktransfer;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One line of a stock transfer ({@code pos_stock_transfer_item}). Snapshots
 * item_code/name/uom/cost at ship time so later edits at either site don't
 * rewrite the transfer record.
 */
@Entity
@Table(name = "pos_stock_transfer_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @Column(name = "source_item_id", nullable = false)
    private Long sourceItemId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(length = 50)
    private String uom;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;
}
