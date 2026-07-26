package com.built4u.pos.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Filtered, paged trail (newest first). All filters nullable; {@code q}
     * matches entity-id or business reference (the CLOB {@code changes} is not
     * searched — avoids Oracle CLOB-LIKE pitfalls).
     */
    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:username IS NULL OR LOWER(a.username) LIKE :username)
             AND (:entity   IS NULL OR LOWER(a.entityName) LIKE :entity)
             AND (:action   IS NULL OR a.action = :action)
             AND (:from     IS NULL OR a.occurredAt >= :from)
             AND (:to       IS NULL OR a.occurredAt <= :to)
             AND (:q IS NULL OR LOWER(a.entityId) LIKE :q
                  OR (a.reference IS NOT NULL AND LOWER(a.reference) LIKE :q))
           ORDER BY a.id DESC
           """)
    Page<AuditLog> search(@Param("username") String username,
                          @Param("entity") String entity,
                          @Param("action") String action,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("q") String q,
                          Pageable pageable);
}
