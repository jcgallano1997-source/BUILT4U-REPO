package com.built4u.pos.customer;

import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.customer.dto.CreateCustomerRequest;
import com.built4u.pos.customer.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerDto> list(String search, boolean includeInactive) {
        Long siteId = TenantContext.requireSiteId();
        String needle = search == null ? "" : search.toLowerCase().trim();
        return customerRepository.findBySiteIdOrderByCustomerNameAsc(siteId).stream()
            .filter(c -> includeInactive || Boolean.TRUE.equals(c.getActive()))
            .filter(c -> needle.isEmpty()
                || c.getCustomerName().toLowerCase().contains(needle)
                || (c.getContact() != null && c.getContact().toLowerCase().contains(needle))
                || (c.getEmail() != null && c.getEmail().toLowerCase().contains(needle)))
            .map(CustomerDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerDto get(Long customerId) {
        Long siteId = TenantContext.requireSiteId();
        return customerRepository.findBySiteIdAndCustomerId(siteId, customerId)
            .map(CustomerDto::from)
            .orElseThrow(() -> new NotFoundException("Customer " + customerId + " not found"));
    }

    @Transactional
    public CustomerDto create(CreateCustomerRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Customer entity = Customer.builder()
            .siteId(siteId)
            .customerName(req.name().trim())
            .contact(blankToNull(req.contact()))
            .address(blankToNull(req.address()))
            .email(blankToNull(req.email()))
            .points(req.points() == null ? BigDecimal.ZERO : req.points())
            .active(true)
            .build();
        return CustomerDto.from(customerRepository.save(entity));
    }

    @Transactional
    public CustomerDto update(Long customerId, UpdateCustomerRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Customer c = customerRepository.findBySiteIdAndCustomerId(siteId, customerId)
            .orElseThrow(() -> new NotFoundException("Customer " + customerId + " not found"));
        c.setCustomerName(req.name().trim());
        c.setContact(blankToNull(req.contact()));
        c.setAddress(blankToNull(req.address()));
        c.setEmail(blankToNull(req.email()));
        c.setPoints(req.points() == null ? BigDecimal.ZERO : req.points());
        c.setActive(Boolean.TRUE.equals(req.active()));
        return CustomerDto.from(customerRepository.save(c));
    }

    @Transactional
    public void softDelete(Long customerId) {
        Long siteId = TenantContext.requireSiteId();
        Customer c = customerRepository.findBySiteIdAndCustomerId(siteId, customerId)
            .orElseThrow(() -> new NotFoundException("Customer " + customerId + " not found"));
        c.setActive(false);
        customerRepository.save(c);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
