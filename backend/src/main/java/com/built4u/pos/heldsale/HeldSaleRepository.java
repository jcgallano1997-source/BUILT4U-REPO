package com.built4u.pos.heldsale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeldSaleRepository extends JpaRepository<HeldSale, Long> {
    List<HeldSale> findBySiteIdOrderByCreationDateDesc(Long siteId);
}
