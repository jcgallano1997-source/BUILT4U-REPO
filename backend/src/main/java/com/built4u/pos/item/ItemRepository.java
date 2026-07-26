package com.built4u.pos.item;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, ItemId> {

    Optional<Item> findBySiteIdAndItemId(Long siteId, Long itemId);

    /**
     * Pessimistic-write lock variant ({@code SELECT ... FOR UPDATE}). Use inside a
     * {@code @Transactional} method when decrementing quantity (POS checkout) to
     * serialize against concurrent cashiers. Lock items in ascending itemId order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.siteId = :siteId AND i.itemId = :itemId")
    Optional<Item> findBySiteIdAndItemIdForUpdate(@Param("siteId") Long siteId,
                                                  @Param("itemId") Long itemId);

    @Query("SELECT i FROM Item i WHERE i.siteId = :siteId AND i.barcodeId = :barcode AND i.active = TRUE")
    Optional<Item> findBySiteIdAndBarcodeIdAndActive(@Param("siteId") Long siteId,
                                                     @Param("barcode") Long barcode);

    @Query("""
           SELECT CASE WHEN COUNT(i) > 0 THEN TRUE ELSE FALSE END
           FROM Item i
           WHERE i.siteId = :siteId
             AND LOWER(i.itemCode) = LOWER(:code)
             AND (:excludeId IS NULL OR i.itemId <> :excludeId)
           """)
    boolean existsByCode(@Param("siteId") Long siteId,
                         @Param("code") String code,
                         @Param("excludeId") Long excludeId);

    @Query("""
           SELECT CASE WHEN COUNT(i) > 0 THEN TRUE ELSE FALSE END
           FROM Item i
           WHERE i.siteId = :siteId
             AND i.barcodeId = :barcode
             AND (:excludeId IS NULL OR i.itemId <> :excludeId)
           """)
    boolean existsByBarcode(@Param("siteId") Long siteId,
                            @Param("barcode") Long barcode,
                            @Param("excludeId") Long excludeId);

    /**
     * Filtered item list ordered by name. The {@code :level} branch mirrors
     * {@code ItemDto.computeStockLevel} (critical over warning). Null params = no
     * filter for that field.
     */
    @Query("""
           SELECT i FROM Item i
           WHERE i.siteId = :siteId
             AND (LOWER(i.itemCode) LIKE :pattern
                  OR LOWER(i.itemName) LIKE :pattern
                  OR (i.itemDesc IS NOT NULL AND LOWER(i.itemDesc) LIKE :pattern))
             AND (:catId IS NULL OR i.catId = :catId)
             AND (:locId IS NULL OR i.locId = :locId)
             AND (:includeInactive = TRUE OR i.active = TRUE)
             AND (:level IS NULL
                  OR (:level = 'CRITICAL'
                      AND i.critical IS NOT NULL AND i.quantity IS NOT NULL
                      AND i.quantity <= i.critical)
                  OR (:level = 'WARNING'
                      AND i.warning IS NOT NULL AND i.quantity IS NOT NULL
                      AND i.quantity <= i.warning
                      AND NOT (i.critical IS NOT NULL AND i.quantity <= i.critical))
                  OR (:level = 'OK'
                      AND NOT (i.critical IS NOT NULL AND i.quantity IS NOT NULL
                               AND i.quantity <= i.critical)
                      AND NOT (i.warning IS NOT NULL AND i.quantity IS NOT NULL
                               AND i.quantity <= i.warning)))
           ORDER BY i.itemName ASC
           """)
    List<Item> search(
        @Param("siteId") Long siteId,
        @Param("pattern") String pattern,
        @Param("catId") Long catId,
        @Param("locId") Long locId,
        @Param("includeInactive") boolean includeInactive,
        @Param("level") String level
    );
}
