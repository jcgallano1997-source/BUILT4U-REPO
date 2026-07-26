package com.built4u.pos.stocktransfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, Long> {

    List<StockTransferItem> findByTransferIdOrderByIdAsc(Long transferId);
}
