package com.built4u.pos.shift;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A cash drawer movement recorded during an open shift ({@code pos_cash_movement}).
 * {@code direction} is IN (pay-in, added float) or OUT (paid-out, petty cash, bank
 * drop). These adjust the shift's expected cash so a legitimate paid-out doesn't
 * read as a shortage.
 */
@Entity
@Table(name = "pos_cash_movement")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovement {

    public static final String IN = "IN";
    public static final String OUT = "OUT";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_cash_movement_seq")
    @SequenceGenerator(name = "pos_cash_movement_seq", sequenceName = "pos_cash_movement_seq", allocationSize = 1)
    @Column(name = "movement_id")
    private Long movementId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "shift_number", nullable = false, length = 100)
    private String shiftNumber;

    @Column(nullable = false, length = 3)
    private String direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;
}
