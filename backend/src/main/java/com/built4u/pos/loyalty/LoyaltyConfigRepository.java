package com.built4u.pos.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Per-site loyalty config. Key = site_id. */
@Repository
public interface LoyaltyConfigRepository extends JpaRepository<LoyaltyConfig, Long> {
}
