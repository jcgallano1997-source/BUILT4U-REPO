package com.built4u.pos.sale;

import java.io.Serializable;
import java.util.Objects;

/** Composite key for {@link SalePayment}: (site_id, sales_number, seq). */
public class SalePaymentId implements Serializable {

    private Long siteId;
    private String salesNumber;
    private Integer seq;

    public SalePaymentId() {}

    public SalePaymentId(Long siteId, String salesNumber, Integer seq) {
        this.siteId = siteId;
        this.salesNumber = salesNumber;
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalePaymentId that)) return false;
        return Objects.equals(siteId, that.siteId)
            && Objects.equals(salesNumber, that.salesNumber)
            && Objects.equals(seq, that.seq);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, salesNumber, seq);
    }
}
