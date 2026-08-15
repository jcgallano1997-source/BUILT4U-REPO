package com.built4u.pos.shift;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/** Composite key for {@link ShiftDenomination}: (site_id, shift_number, denom). */
public class ShiftDenominationId implements Serializable {

    private Long siteId;
    private String shiftNumber;
    private BigDecimal denom;

    public ShiftDenominationId() {}

    public ShiftDenominationId(Long siteId, String shiftNumber, BigDecimal denom) {
        this.siteId = siteId;
        this.shiftNumber = shiftNumber;
        this.denom = denom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShiftDenominationId that)) return false;
        return Objects.equals(siteId, that.siteId)
            && Objects.equals(shiftNumber, that.shiftNumber)
            && Objects.equals(denom, that.denom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, shiftNumber, denom);
    }
}
