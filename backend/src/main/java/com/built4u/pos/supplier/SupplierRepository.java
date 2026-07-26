package com.built4u.pos.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, SupplierId> {

    Optional<Supplier> findBySiteIdAndSupplierId(Long siteId, Long supplierId);

    Optional<Supplier> findBySiteIdAndSupplierName(Long siteId, String supplierName);

    List<Supplier> findBySiteIdOrderBySupplierNameAsc(Long siteId);

    @Query("""
           SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
           FROM Supplier s
           WHERE s.siteId = :siteId
             AND LOWER(s.supplierCode) = LOWER(:code)
             AND (:excludeId IS NULL OR s.supplierId <> :excludeId)
           """)
    boolean existsByCode(@Param("siteId") Long siteId,
                         @Param("code") String code,
                         @Param("excludeId") Long excludeId);
}
