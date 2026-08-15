package com.built4u.pos.transactionlog;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Generic stock-movement audit row. {@code transactionType} discriminates
 * between STOCK_IN_GR, STOCK_OUT_SALE, STOCK_ADJUST, etc.
 */
@Entity
@Table(name = "pos_transaction_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    public static final String TYPE_STOCK_IN_OPENING  = "STOCK_IN_OPENING";
    public static final String TYPE_STOCK_EDIT        = "STOCK_EDIT";
    public static final String TYPE_STOCK_IN_GR       = "STOCK_IN_GR";
    public static final String TYPE_STOCK_OUT_SALE    = "STOCK_OUT_SALE";
    public static final String TYPE_STOCK_IN_VOID     = "STOCK_IN_VOID";
    public static final String TYPE_STOCK_IN_REFUND   = "STOCK_IN_REFUND";
    public static final String TYPE_STOCK_ADJUST      = "STOCK_ADJUST";
    public static final String TYPE_STOCK_OUT_TRANSFER = "STOCK_OUT_TRANSFER";
    public static final String TYPE_STOCK_IN_TRANSFER  = "STOCK_IN_TRANSFER";
    public static final String TYPE_STOCK_IN_XFER_CANCEL = "STOCK_IN_XFER_CANCEL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "cat_id")
    private Long catId;

    @Column(name = "attr_id1")
    private Long attrId1;

    @Column(name = "attr_id2")
    private Long attrId2;

    @Column(name = "transaction_type", nullable = false, length = 200)
    private String transactionType;

    @Column(length = 300)
    private String attribute1;

    @Column(length = 300)
    private String attribute2;

    @Column(length = 300)
    private String attribute3;

    @Column(length = 300)
    private String attribute4;

    @Column(length = 300)
    private String reason;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
