package com.built4u.pos.goodsreceipt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceiptItem, GoodsReceiptItemId> {

    List<GoodsReceiptItem> findBySiteIdAndGrNumberOrderByItemIdAsc(Long siteId, String grNumber);

    /** All received lines for a given PO (across multiple GRs). */
    List<GoodsReceiptItem> findBySiteIdAndPoNumberOrderByGrNumberAscItemIdAsc(Long siteId, String poNumber);

    /** Every received line for the site, newest GR first — for the standalone Receiving list. */
    @Query("""
           SELECT g FROM GoodsReceiptItem g
           WHERE g.siteId = :siteId
           ORDER BY g.grNumber DESC, g.itemId ASC
           """)
    List<GoodsReceiptItem> findAllForSite(@Param("siteId") Long siteId);

    /** Sum of received quantity per item_id for a PO — used to compute remaining qty. */
    @Query("""
           SELECT g.itemId, SUM(g.quantity)
           FROM GoodsReceiptItem g
           WHERE g.siteId = :siteId AND g.poNumber = :poNumber
           GROUP BY g.itemId
           """)
    List<Object[]> sumReceivedByPoRaw(@Param("siteId") Long siteId, @Param("poNumber") String poNumber);

    default Map<Long, BigDecimal> sumReceivedByPo(Long siteId, String poNumber) {
        return sumReceivedByPoRaw(siteId, poNumber).stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> (BigDecimal) row[1],
                (a, b) -> a
            ));
    }

    @Query("SELECT MAX(g.grNumber) FROM GoodsReceiptItem g WHERE g.siteId = :siteId AND g.grNumber LIKE :prefix")
    String findMaxGrNumberWithPrefix(@Param("siteId") Long siteId, @Param("prefix") String prefix);
}
