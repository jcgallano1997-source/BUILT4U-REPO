package com.built4u.pos.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderItem, PurchaseOrderItemId> {

    /** All line items for one PO. */
    List<PurchaseOrderItem> findBySiteIdAndPoNumberOrderByItemIdAsc(Long siteId, String poNumber);

    /** Distinct PO numbers, optionally filtered by status/supplier. Builds the header list. */
    @Query("""
           SELECT DISTINCT p.poNumber
           FROM PurchaseOrderItem p
           WHERE p.siteId = :siteId
             AND (:status IS NULL OR p.status = :status)
             AND (:supplier IS NULL OR LOWER(p.supplier) LIKE :supplierPattern)
           ORDER BY p.poNumber DESC
           """)
    List<String> findPoNumbers(
        @Param("siteId") Long siteId,
        @Param("status") String status,
        @Param("supplier") String supplier,
        @Param("supplierPattern") String supplierPattern
    );

    @Query("SELECT MAX(p.poNumber) FROM PurchaseOrderItem p WHERE p.siteId = :siteId AND p.poNumber LIKE :prefix")
    String findMaxPoNumberWithPrefix(@Param("siteId") Long siteId, @Param("prefix") String prefix);
}
