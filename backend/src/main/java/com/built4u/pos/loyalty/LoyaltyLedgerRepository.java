package com.built4u.pos.loyalty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedger, Long> {

    Page<LoyaltyLedger> findBySiteIdAndCustomerIdOrderByIdDesc(Long siteId, Long customerId, Pageable pageable);

    /** EARN entries for a sale — used to claw points back when the sale is voided. */
    List<LoyaltyLedger> findBySiteIdAndSalesNumberAndEntryType(Long siteId, String salesNumber, String entryType);

    /** Sum of all ledger entries for a customer (audit; live balance is customer.points). */
    @Query("SELECT COALESCE(SUM(l.points), 0) FROM LoyaltyLedger l " +
           "WHERE l.siteId = :siteId AND l.customerId = :customerId")
    BigDecimal sumPoints(@Param("siteId") Long siteId, @Param("customerId") Long customerId);
}
