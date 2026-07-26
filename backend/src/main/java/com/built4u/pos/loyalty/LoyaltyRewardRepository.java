package com.built4u.pos.loyalty;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoyaltyRewardRepository extends JpaRepository<LoyaltyReward, Long> {

    List<LoyaltyReward> findBySiteIdOrderBySortOrderAscNameAsc(Long siteId);

    List<LoyaltyReward> findBySiteIdAndActiveTrueOrderBySortOrderAscNameAsc(Long siteId);

    Optional<LoyaltyReward> findBySiteIdAndId(Long siteId, Long id);

    Optional<LoyaltyReward> findBySiteIdAndNameIgnoreCase(Long siteId, String name);

    /** Pessimistic-write lock — serialize redemptions of the same reward row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM LoyaltyReward r WHERE r.siteId = :siteId AND r.id = :id")
    Optional<LoyaltyReward> findBySiteIdAndIdForUpdate(@Param("siteId") Long siteId, @Param("id") Long id);
}
