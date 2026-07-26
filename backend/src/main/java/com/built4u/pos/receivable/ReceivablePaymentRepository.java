package com.built4u.pos.receivable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceivablePaymentRepository extends JpaRepository<ReceivablePayment, Long> {

    List<ReceivablePayment> findBySiteIdAndReceivableIdOrderByIdDesc(Long siteId, Long receivableId);
}
