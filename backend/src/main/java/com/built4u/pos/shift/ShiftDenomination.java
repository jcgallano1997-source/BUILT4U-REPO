package com.built4u.pos.shift;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** A bill/coin tally line recorded at shift close ({@code pos_shift_denomination}). */
@Entity
@Table(name = "pos_shift_denomination")
@IdClass(ShiftDenominationId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDenomination {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "shift_number", nullable = false, length = 100)
    private String shiftNumber;

    @Id
    @Column(nullable = false)
    private BigDecimal denom;

    @Column(nullable = false)
    private Integer qty;
}
