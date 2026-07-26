package com.built4u.pos.receivable;

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
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    Optional<Receivable> findBySiteIdAndId(Long siteId, Long id);

    Optional<Receivable> findBySiteIdAndSalesNumber(Long siteId, String salesNumber);

    /** Lock the row before recording a payment (serialize concurrent collections). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Receivable r WHERE r.siteId = :siteId AND r.id = :id")
    Optional<Receivable> findBySiteIdAndIdForUpdate(@Param("siteId") Long siteId,
                                                    @Param("id") Long id);

    /** Σ balance the customer still owes (drives credit-limit checks). */
    @Query("SELECT COALESCE(SUM(r.balance), 0) FROM Receivable r " +
           "WHERE r.siteId = :siteId AND r.customerId = :customerId " +
           "AND r.status <> 'CANCELLED'")
    BigDecimal sumOpenBalanceByCustomer(@Param("siteId") Long siteId,
                                        @Param("customerId") Long customerId);

    /**
     * Filtered, paged list. {@code overdue=true} → only past-due unpaid rows;
     * null status/customer/search = no filter. {@code asOf} is today (for the
     * overdue test); harmless when {@code overdue} is false.
     */
    @Query("""
           SELECT r FROM Receivable r
           WHERE r.siteId = :siteId
             AND (:status IS NULL OR r.status = :status)
             AND (:customerId IS NULL OR r.customerId = :customerId)
             AND (:search IS NULL OR LOWER(r.salesNumber) LIKE :search)
             AND (:overdue = FALSE OR
                  (r.balance > 0 AND r.dueDate < :asOf
                   AND r.status NOT IN ('PAID','CANCELLED')))
           ORDER BY r.creationDate DESC, r.id DESC
           """)
    Page<Receivable> search(@Param("siteId") Long siteId,
                            @Param("status") String status,
                            @Param("customerId") Long customerId,
                            @Param("search") String search,
                            @Param("overdue") boolean overdue,
                            @Param("asOf") LocalDate asOf,
                            Pageable pageable);
}
