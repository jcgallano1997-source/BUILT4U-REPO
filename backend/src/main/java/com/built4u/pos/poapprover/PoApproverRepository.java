package com.built4u.pos.poapprover;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PoApproverRepository extends JpaRepository<PoApprover, Long> {

    /** All users this approver is responsible for. Used by "pending my approval". */
    List<PoApprover> findByApproverUserId(Long approverUserId);
}
