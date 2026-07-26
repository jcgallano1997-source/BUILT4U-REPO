package com.built4u.pos.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, CustomerId> {

    Optional<Customer> findBySiteIdAndCustomerId(Long siteId, Long customerId);

    List<Customer> findBySiteIdOrderByCustomerNameAsc(Long siteId);
}
