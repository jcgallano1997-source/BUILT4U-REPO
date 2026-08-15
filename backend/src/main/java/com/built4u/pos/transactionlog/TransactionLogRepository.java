package com.built4u.pos.transactionlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    List<TransactionLog> findBySiteIdAndItemIdOrderByCreationDateDesc(Long siteId, Long itemId);

    /** All stock movements in a half-open window [from, to) — drives the movement report. */
    @Query("""
           SELECT t FROM TransactionLog t
           WHERE t.siteId = :siteId AND t.creationDate >= :from AND t.creationDate < :to
           ORDER BY t.creationDate DESC, t.logId DESC
           """)
    List<TransactionLog> findInRange(@Param("siteId") Long siteId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    /** All stock movements on/after {@code from} — used to reconstruct historical (as-of) balances. */
    @Query("""
           SELECT t FROM TransactionLog t
           WHERE t.siteId = :siteId AND t.creationDate >= :from
           """)
    List<TransactionLog> findFrom(@Param("siteId") Long siteId, @Param("from") LocalDateTime from);
}
