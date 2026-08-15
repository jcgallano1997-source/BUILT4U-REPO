package com.built4u.pos.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findBySiteIdAndShiftNumberOrderByCreationDateAsc(Long siteId, String shiftNumber);

    @Query("""
           SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m
           WHERE m.siteId = :siteId AND m.shiftNumber = :shiftNumber AND m.direction = :direction
           """)
    BigDecimal sumByShiftAndDirection(@Param("siteId") Long siteId,
                                      @Param("shiftNumber") String shiftNumber,
                                      @Param("direction") String direction);
}
