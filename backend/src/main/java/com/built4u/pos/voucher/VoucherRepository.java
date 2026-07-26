package com.built4u.pos.voucher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findBySiteIdAndId(Long siteId, Long id);

    Optional<Voucher> findBySiteIdAndCodeIgnoreCase(Long siteId, String code);

    @Query("""
           SELECT v FROM Voucher v
           WHERE v.siteId = :siteId
             AND (:includeInactive = TRUE OR v.active = TRUE)
             AND (LOWER(v.code) LIKE :pattern OR
                  (v.description IS NOT NULL AND LOWER(v.description) LIKE :pattern))
           """)
    Page<Voucher> search(@Param("siteId") Long siteId,
                         @Param("pattern") String pattern,
                         @Param("includeInactive") boolean includeInactive,
                         Pageable pageable);

    /**
     * Atomic single-use guard: bumps used_count only while under the limit.
     * Returns rows affected — 0 ⇒ the voucher is fully used (reject the redeem).
     */
    @Modifying
    @Query("""
           UPDATE Voucher v SET v.usedCount = v.usedCount + 1
           WHERE v.id = :id
             AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)
           """)
    int tryConsume(@Param("id") Long id);

    /** Release one redemption (full VOID). Never drops below zero. */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 WHERE v.id = :id AND v.usedCount > 0")
    int release(@Param("id") Long id);
}
