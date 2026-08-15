package com.built4u.pos.heldsale;

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
 * A parked/held POS cart the cashier can recall later. The {@code cartJson} is
 * opaque to the backend (the register owns its shape); the other columns drive
 * the recall list. Site-scoped; not a financial transaction. Mapped to
 * {@code pos_held_sale}.
 */
@Entity
@Table(name = "pos_held_sale")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeldSale {

    @Id
    @Column(name = "held_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_held_sale_seq")
    @SequenceGenerator(name = "pos_held_sale_seq", sequenceName = "pos_held_sale_seq", allocationSize = 1)
    private Long heldId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(length = 100)
    private String label;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "item_count", nullable = false)
    @Builder.Default
    private Integer itemCount = 0;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Lob
    @Column(name = "cart_json", nullable = false)
    private String cartJson;

    @CreatedDate @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedDate @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;
}
