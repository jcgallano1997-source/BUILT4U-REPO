package com.built4u.pos.supplier;

import com.built4u.pos.common.tenant.YesNoConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pos_supplier")
@IdClass(SupplierId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id @Column(name = "supplier_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_supplier_seq")
    @SequenceGenerator(name = "pos_supplier_seq", sequenceName = "pos_supplier_seq", allocationSize = 1)
    private Long supplierId;

    @Column(name = "supplier_code", nullable = false, length = 20)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "supplier_address", length = 250)
    private String supplierAddress;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(length = 100)
    private String contact;

    /** When true, a goods receipt from this supplier auto-creates an AP payable. */
    @Column(name = "ap_enabled", nullable = false)
    @Builder.Default
    private boolean apEnabled = false;

    /** Net terms (days) used for the auto-created payable's due date. */
    @Column(name = "payable_days", nullable = false)
    @Builder.Default
    private int payableDays = 30;

    @Column(nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean active = true;

    @CreatedDate @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedDate @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;
}
