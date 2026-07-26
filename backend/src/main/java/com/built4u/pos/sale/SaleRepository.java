package com.built4u.pos.sale;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, SaleId> {

    Optional<Sale> findBySiteIdAndSalesNumber(Long siteId, String salesNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.siteId = :siteId AND s.salesNumber = :salesNumber")
    Optional<Sale> findBySiteIdAndSalesNumberForUpdate(@Param("siteId") Long siteId,
                                                       @Param("salesNumber") String salesNumber);

    List<Sale> findTop200BySiteIdOrderByCreationDateDescSalesNumberDesc(Long siteId);

    List<Sale> findTop200BySiteIdAndStatusOrderByCreationDateDescSalesNumberDesc(Long siteId, String status);

    @Query("SELECT MAX(s.salesNumber) FROM Sale s WHERE s.siteId = :siteId AND s.salesNumber LIKE :prefix")
    String findMaxSalesNumberWithPrefix(@Param("siteId") Long siteId, @Param("prefix") String prefix);

    /** COMPLETED sales in a half-open datetime window [from, to) — drives the sales report. */
    @Query("""
           SELECT s FROM Sale s
           WHERE s.siteId = :siteId AND s.status = 'COMPLETED'
             AND s.creationDate >= :from AND s.creationDate < :to
           ORDER BY s.creationDate ASC
           """)
    List<Sale> findCompletedInRange(@Param("siteId") Long siteId,
                                    @Param("from") java.time.LocalDateTime from,
                                    @Param("to") java.time.LocalDateTime to);

    /** Sum of grand_total for a cashier's non-VOIDED sales of one mode in a window (shift reconc). */
    @Query("""
           SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s
           WHERE s.siteId = :siteId
             AND s.createdBy = :cashier
             AND s.modeOfPayment = :mode
             AND s.status <> 'VOIDED'
             AND s.creationDate >= :from
             AND s.creationDate <= :to
           """)
    BigDecimal sumGrandTotalByCashierModeInWindow(@Param("siteId") Long siteId,
                                                  @Param("cashier") String cashier,
                                                  @Param("mode") String mode,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    @Query("""
           SELECT COUNT(s) FROM Sale s
           WHERE s.siteId = :siteId
             AND s.createdBy = :cashier
             AND s.status <> 'VOIDED'
             AND s.creationDate >= :from
             AND s.creationDate <= :to
           """)
    long countByCashierInWindow(@Param("siteId") Long siteId,
                                @Param("cashier") String cashier,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
