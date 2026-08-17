package com.built4u.pos.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, SaleItemId> {
    List<SaleItem> findBySiteIdAndSalesNumberOrderByItemIdAsc(Long siteId, String salesNumber);

    /** Last COMPLETED-sale date per item (drives the dead-stock report). Rows: [itemId, lastSold]. */
    @Query("""
           SELECT si.itemId, MAX(s.creationDate)
             FROM SaleItem si, Sale s
            WHERE si.siteId = s.siteId AND si.salesNumber = s.salesNumber
              AND s.siteId = :siteId AND s.status = 'COMPLETED'
            GROUP BY si.itemId
           """)
    List<Object[]> lastSoldPerItem(@Param("siteId") Long siteId);
}
