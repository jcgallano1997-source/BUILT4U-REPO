package com.built4u.pos.stocktransferpolicy;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * One row = one permitted (source_site_id → dest_site_id) shipping pair
 * ({@code pos_stock_transfer_policy}). When the table is empty the policy is
 * OPEN (any site may ship to any other); once at least one row exists it is
 * ENFORCED — only listed pairs are permitted at ship time.
 */
@Entity
@Table(name = "pos_stock_transfer_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class StockTransferPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_site_id", nullable = false)
    private Long sourceSiteId;

    @Column(name = "dest_site_id", nullable = false)
    private Long destSiteId;

    @CreatedDate
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;
}
