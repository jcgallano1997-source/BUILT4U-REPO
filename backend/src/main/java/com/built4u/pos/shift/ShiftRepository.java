package com.built4u.pos.shift;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, ShiftId> {

    Optional<Shift> findBySiteIdAndShiftNumber(Long siteId, String shiftNumber);

    /** The caller's currently OPEN shift for this site, if any. */
    Optional<Shift> findBySiteIdAndCashierAndStatus(Long siteId, String cashier, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shift s WHERE s.siteId = :siteId AND s.shiftNumber = :shiftNumber")
    Optional<Shift> findBySiteIdAndShiftNumberForUpdate(@Param("siteId") Long siteId,
                                                        @Param("shiftNumber") String shiftNumber);

    List<Shift> findBySiteIdOrderByOpenedAtDesc(Long siteId);

    List<Shift> findBySiteIdAndCashierOrderByOpenedAtDesc(Long siteId, String cashier);

    @Query("SELECT MAX(s.shiftNumber) FROM Shift s WHERE s.siteId = :siteId AND s.shiftNumber LIKE :prefix")
    String findMaxShiftNumberWithPrefix(@Param("siteId") Long siteId, @Param("prefix") String prefix);

    /** Shift History report: filter by status / cashier-or-number search / opened-at window. */
    @Query("""
           SELECT s FROM Shift s
           WHERE s.siteId = :siteId
             AND (:status IS NULL OR s.status = :status)
             AND (:pattern IS NULL OR LOWER(s.cashier) LIKE :pattern OR LOWER(s.shiftNumber) LIKE :pattern)
             AND (:from IS NULL OR s.openedAt >= :from)
             AND (:to IS NULL OR s.openedAt <= :to)
           ORDER BY s.openedAt DESC
           """)
    List<Shift> searchForReport(@Param("siteId") Long siteId,
                                @Param("status") String status,
                                @Param("pattern") String pattern,
                                @Param("from") java.time.LocalDateTime from,
                                @Param("to") java.time.LocalDateTime to);
}
