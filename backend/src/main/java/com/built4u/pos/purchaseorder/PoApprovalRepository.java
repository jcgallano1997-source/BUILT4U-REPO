package com.built4u.pos.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PoApprovalRepository extends JpaRepository<PoApproval, PoApproval.PoApprovalId> {
    Optional<PoApproval> findBySiteIdAndPoNumber(Long siteId, String poNumber);
}
