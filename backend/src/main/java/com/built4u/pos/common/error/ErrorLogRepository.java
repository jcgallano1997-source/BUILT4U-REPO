package com.built4u.pos.common.error;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    /** Newest-first, optional site filter (null = any). Limit via {@link Pageable}. */
    @Query("""
           SELECT e FROM ErrorLog e
            WHERE (:siteCode IS NULL OR e.siteCode = :siteCode)
            ORDER BY e.occurredAt DESC
           """)
    List<ErrorLog> search(@Param("siteCode") String siteCode, Pageable pageable);

    /** Retention purge — drop rows older than the cutoff. */
    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.occurredAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
