package com.built4u.pos.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentModeRepository extends JpaRepository<PaymentMode, Long> {

    Optional<PaymentMode> findBySiteIdAndId(Long siteId, Long id);

    Optional<PaymentMode> findBySiteIdAndCode(Long siteId, String code);

    List<PaymentMode> findBySiteIdOrderBySortOrderAscCodeAsc(Long siteId);

    List<PaymentMode> findBySiteIdAndActiveTrueOrderBySortOrderAscCodeAsc(Long siteId);

    @Query("""
           SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
           FROM PaymentMode m
           WHERE m.siteId = :siteId
             AND UPPER(m.code) = UPPER(:code)
             AND (:excludeId IS NULL OR m.id <> :excludeId)
           """)
    boolean existsByCode(@Param("siteId") Long siteId,
                         @Param("code") String code,
                         @Param("excludeId") Long excludeId);
}
