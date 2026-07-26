package com.built4u.pos.stocktransfer;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    Optional<StockTransfer> findBySourceSiteIdAndTransferNumber(Long sourceSiteId, String transferNumber);

    /** Lookup by transfer number across sites — receive/cancel run where the current site may be source OR dest. */
    Optional<StockTransfer> findByTransferNumber(String transferNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockTransfer s WHERE s.transferNumber = :tn")
    Optional<StockTransfer> findByTransferNumberForUpdate(@Param("tn") String transferNumber);

    /** Used by the nextTransferNumber generator (per source site, per year). */
    @Query("SELECT MAX(s.transferNumber) FROM StockTransfer s "
        + "WHERE s.sourceSiteId = :siteId AND s.transferNumber LIKE :prefix")
    String findMaxTransferNumberWithPrefix(@Param("siteId") Long siteId,
                                            @Param("prefix") String prefix);

    /**
     * Filtered, paged list (newest first). {@code direction}: {@code OUTBOUND}
     * = current site is the source; {@code INBOUND} = current site is the dest;
     * null = either (default).
     */
    @Query("""
           SELECT s FROM StockTransfer s
           WHERE ((:direction = 'OUTBOUND' AND s.sourceSiteId = :siteId)
               OR (:direction = 'INBOUND'  AND s.destSiteId   = :siteId)
               OR (:direction IS NULL
                   AND (s.sourceSiteId = :siteId OR s.destSiteId = :siteId)))
             AND (:status IS NULL OR s.status = :status)
             AND (:search IS NULL OR LOWER(s.transferNumber) LIKE :search)
             AND (:from   IS NULL OR s.shippedAt >= :from)
             AND (:to     IS NULL OR s.shippedAt <= :to)
           ORDER BY s.shippedAt DESC, s.id DESC
           """)
    Page<StockTransfer> search(@Param("siteId") Long siteId,
                                @Param("direction") String direction,
                                @Param("status") String status,
                                @Param("search") String search,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                Pageable pageable);
}
