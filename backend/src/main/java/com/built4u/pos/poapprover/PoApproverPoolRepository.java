package com.built4u.pos.poapprover;

import org.springframework.data.jpa.repository.JpaRepository;

/** Users eligible to be picked as a PO approver (owner aside — see the entity). */
public interface PoApproverPoolRepository extends JpaRepository<PoApproverPool, Long> {
}
