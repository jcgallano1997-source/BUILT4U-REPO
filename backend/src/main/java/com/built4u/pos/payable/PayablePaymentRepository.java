package com.built4u.pos.payable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayablePaymentRepository extends JpaRepository<PayablePayment, Long> {

    List<PayablePayment> findBySiteIdAndPayableIdOrderByIdDesc(Long siteId, Long payableId);
}
