package com.built4u.pos.stocktransfer;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * One cross-site stock transfer ({@code pos_stock_transfer}). Two-step flow:
 * source ships (IN_TRANSIT) → destination receives (RECEIVED), or source
 * cancels (CANCELLED, only while still IN_TRANSIT).
 */
@Entity
@Table(name = "pos_stock_transfer")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_site_id", nullable = false)
    private Long sourceSiteId;

    @Column(name = "dest_site_id", nullable = false)
    private Long destSiteId;

    @Column(name = "transfer_number", nullable = false, length = 100)
    private String transferNumber;

    @Column(nullable = false, length = 15)
    @Builder.Default
    private String status = StockTransferStatus.IN_TRANSIT.name();

    @Column(length = 300)
    private String remarks;

    @Column(name = "shipped_at", nullable = false)
    @Builder.Default
    private LocalDateTime shippedAt = LocalDateTime.now();

    @Column(name = "sent_by", nullable = false, length = 50)
    private String sentBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "received_by", length = 50)
    private String receivedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

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
