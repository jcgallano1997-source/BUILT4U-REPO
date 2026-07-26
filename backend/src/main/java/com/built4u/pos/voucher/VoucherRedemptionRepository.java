package com.built4u.pos.voucher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {

    /** The active (non-reversed) redemption for a sale, if any. */
    Optional<VoucherRedemption> findBySiteIdAndSalesNumberAndReversedFalse(Long siteId, String salesNumber);

    Optional<VoucherRedemption> findBySiteIdAndSalesNumber(Long siteId, String salesNumber);
}
