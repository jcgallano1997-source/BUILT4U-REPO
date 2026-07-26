package com.built4u.pos.stocktransferpolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockTransferPolicyRepository extends JpaRepository<StockTransferPolicy, Long> {

    boolean existsBySourceSiteIdAndDestSiteId(Long sourceSiteId, Long destSiteId);

    List<StockTransferPolicy> findAllByOrderBySourceSiteIdAscDestSiteIdAsc();

    @Query("SELECT p.destSiteId FROM StockTransferPolicy p WHERE p.sourceSiteId = :sourceSiteId")
    List<Long> findAllowedDestSiteIds(Long sourceSiteId);
}
