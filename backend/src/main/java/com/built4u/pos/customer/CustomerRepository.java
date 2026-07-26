package com.built4u.pos.customer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, CustomerId> {

    Optional<Customer> findBySiteIdAndCustomerId(Long siteId, Long customerId);

    /** Pessimistic-write lock — serialize concurrent points updates (loyalty redeem). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.siteId = :siteId AND c.customerId = :customerId")
    Optional<Customer> findBySiteIdAndCustomerIdForUpdate(@Param("siteId") Long siteId,
                                                          @Param("customerId") Long customerId);

    List<Customer> findBySiteIdOrderByCustomerNameAsc(Long siteId);
}
