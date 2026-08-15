package com.built4u.pos.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftDenominationRepository extends JpaRepository<ShiftDenomination, ShiftDenominationId> {

    List<ShiftDenomination> findBySiteIdAndShiftNumberOrderByDenomDesc(Long siteId, String shiftNumber);
}
