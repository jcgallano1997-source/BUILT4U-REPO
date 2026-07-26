package com.built4u.pos.payable;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<Payable, Long> {

    Optional<Payable> findBySiteIdAndId(Long siteId, Long id);

    Optional<Payable> findBySiteIdAndGrNumber(Long siteId, String grNumber);

    Optional<Payable> findBySiteIdAndPoNumber(Long siteId, String poNumber);

    /** Lock the row before recording a payment (serialize concurrent disbursements). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payable p WHERE p.siteId = :siteId AND p.id = :id")
    Optional<Payable> findBySiteIdAndIdForUpdate(@Param("siteId") Long siteId,
                                                  @Param("id") Long id);

    /** Σ outstanding balance the site still owes a supplier. */
    @Query("SELECT COALESCE(SUM(p.balance), 0) FROM Payable p " +
           "WHERE p.siteId = :siteId AND p.supplierId = :supplierId " +
           "AND p.status <> 'CANCELLED'")
    BigDecimal sumOpenBalanceBySupplier(@Param("siteId") Long siteId,
                                        @Param("supplierId") Long supplierId);

    /**
     * Filtered, paged list (newest first). All filters nullable; overdue branch
     * matches balance>0 AND due_date<:asOf AND status NOT IN PAID/CANCELLED;
     * {@code search} matches po/gr/payee.
     */
    @Query("""
           SELECT p FROM Payable p
           WHERE p.siteId = :siteId
             AND (:status IS NULL OR p.status = :status)
             AND (:source IS NULL OR p.source = :source)
             AND (:supplierId IS NULL OR p.supplierId = :supplierId)
             AND (:search IS NULL
                  OR LOWER(p.payeeName) LIKE :search
                  OR (p.poNumber IS NOT NULL AND LOWER(p.poNumber) LIKE :search)
                  OR (p.grNumber IS NOT NULL AND LOWER(p.grNumber) LIKE :search))
             AND (:overdue = FALSE OR
                  (p.balance > 0 AND p.dueDate < :asOf
                   AND p.status NOT IN ('PAID','CANCELLED')))
           ORDER BY p.creationDate DESC, p.id DESC
           """)
    Page<Payable> search(@Param("siteId") Long siteId,
                         @Param("status") String status,
                         @Param("source") String source,
                         @Param("supplierId") Long supplierId,
                         @Param("search") String search,
                         @Param("overdue") boolean overdue,
                         @Param("asOf") LocalDate asOf,
                         Pageable pageable);
}
