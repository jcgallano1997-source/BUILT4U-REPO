package com.built4u.pos.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, SaleItemId> {
    List<SaleItem> findBySiteIdAndSalesNumberOrderByItemIdAsc(Long siteId, String salesNumber);
}
