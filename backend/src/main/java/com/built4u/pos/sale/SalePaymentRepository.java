package com.built4u.pos.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalePaymentRepository extends JpaRepository<SalePayment, SalePaymentId> {

    List<SalePayment> findBySiteIdAndSalesNumberOrderBySeqAsc(Long siteId, String salesNumber);

    /** Applied total for one mode, over a cashier's non-VOIDED sales in a window (shift reconciliation). */
    @Query("""
           SELECT COALESCE(SUM(p.amount), 0) FROM SalePayment p, Sale s
           WHERE p.siteId = :siteId AND s.siteId = :siteId AND p.salesNumber = s.salesNumber
             AND s.createdBy = :cashier AND p.mode = :mode AND s.status <> 'VOIDED'
             AND s.creationDate >= :from AND s.creationDate <= :to
           """)
    BigDecimal sumAppliedByCashierModeInWindow(@Param("siteId") Long siteId,
                                               @Param("cashier") String cashier,
                                               @Param("mode") String mode,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    /** All tenders of COMPLETED sales in a half-open window [from, to) — drives the sales-by-mode report. */
    @Query("""
           SELECT p FROM SalePayment p, Sale s
           WHERE p.siteId = :siteId AND s.siteId = :siteId AND p.salesNumber = s.salesNumber
             AND s.status = 'COMPLETED'
             AND s.creationDate >= :from AND s.creationDate < :to
           """)
    List<SalePayment> findForCompletedSalesInRange(@Param("siteId") Long siteId,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}
