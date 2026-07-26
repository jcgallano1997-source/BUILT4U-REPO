package com.built4u.pos.customer;

import com.built4u.pos.customer.dto.CreateCustomerRequest;
import com.built4u.pos.customer.dto.UpdateCustomerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    private static final String READ_ANY = "hasAnyAuthority('MOD_CUSTOMERS','MOD_POS','MOD_SALES','MOD_RECEIVABLES')";

    @GetMapping
    @PreAuthorize(READ_ANY)
    public ResponseEntity<List<CustomerDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(customerService.list(search, includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ANY)
    public ResponseEntity<CustomerDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MOD_CUSTOMERS','MOD_POS')")
    public ResponseEntity<CustomerDto> create(@Valid @RequestBody CreateCustomerRequest req) {
        return ResponseEntity.ok(customerService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_CUSTOMERS')")
    public ResponseEntity<CustomerDto> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateCustomerRequest req) {
        return ResponseEntity.ok(customerService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_CUSTOMERS')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        customerService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
