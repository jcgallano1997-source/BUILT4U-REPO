package com.built4u.pos.customer;

import com.built4u.pos.common.tenant.YesNoConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_customer")
@IdClass(CustomerId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id @Column(name = "customer_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_customer_seq")
    @SequenceGenerator(name = "pos_customer_seq", sequenceName = "pos_customer_seq", allocationSize = 1)
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(length = 50)
    private String contact;

    @Column(length = 200)
    private String address;

    @Column(length = 120)
    private String email;

    @Column
    @Builder.Default
    private BigDecimal points = BigDecimal.ZERO;

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
