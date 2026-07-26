package com.built4u.pos.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, ReturnItemId> {

    List<ReturnItem> findBySiteIdAndSalesNumberOrderByCreationDateAscItemIdAsc(Long siteId, String salesNumber);

    List<ReturnItem> findBySiteIdAndReturnNumberOrderByItemIdAsc(Long siteId, String returnNumber);

    /** Sum of refunded quantity for a given sales line (0 if never refunded). */
    @Query("""
           SELECT COALESCE(SUM(r.quantity), 0) FROM ReturnItem r
           WHERE r.siteId = :siteId AND r.salesNumber = :salesNumber AND r.itemId = :itemId
           """)
    BigDecimal sumRefundedQuantity(@Param("siteId") Long siteId,
                                   @Param("salesNumber") String salesNumber,
                                   @Param("itemId") Long itemId);

    @Query("SELECT MAX(r.returnNumber) FROM ReturnItem r WHERE r.siteId = :siteId AND r.returnNumber LIKE :prefix")
    String findMaxReturnNumberWithPrefix(@Param("siteId") Long siteId, @Param("prefix") String prefix);

    /** Sum of refund sub_total credited by a cashier in a window (subtracted from expected cash). */
    @Query("""
           SELECT COALESCE(SUM(r.subTotal), 0) FROM ReturnItem r
           WHERE r.siteId = :siteId
             AND r.createdBy = :cashier
             AND r.creationDate >= :from
             AND r.creationDate <= :to
           """)
    BigDecimal sumRefundSubTotalByCashierInWindow(@Param("siteId") Long siteId,
                                                  @Param("cashier") String cashier,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);
}
