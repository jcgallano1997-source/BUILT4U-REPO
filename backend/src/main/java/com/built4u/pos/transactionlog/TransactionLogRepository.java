package com.built4u.pos.transactionlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    List<TransactionLog> findBySiteIdAndItemIdOrderByCreationDateDesc(Long siteId, Long itemId);
}
