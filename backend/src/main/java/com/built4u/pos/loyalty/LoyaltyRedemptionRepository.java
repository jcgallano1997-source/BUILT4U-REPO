package com.built4u.pos.loyalty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyRedemptionRepository extends JpaRepository<LoyaltyRedemption, Long> {

    Page<LoyaltyRedemption> findBySiteIdAndCustomerIdOrderByIdDesc(Long siteId, Long customerId, Pageable pageable);
}
