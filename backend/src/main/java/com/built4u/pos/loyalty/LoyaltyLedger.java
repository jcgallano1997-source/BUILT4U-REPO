package com.built4u.pos.loyalty;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One signed points movement ({@code pos_loyalty_ledger}). {@code customer.points}
 * remains the live balance; this is the audit history. {@code points} is signed
 * (EARN/OPENING +, REDEEM/ADJUST ±).
 */
@Entity
@Table(name = "pos_loyalty_ledger")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(nullable = false)
    private BigDecimal points;

    @Column(name = "sales_number", length = 100)
    private String salesNumber;

    @Column(length = 255)
    private String note;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean expired = false;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
