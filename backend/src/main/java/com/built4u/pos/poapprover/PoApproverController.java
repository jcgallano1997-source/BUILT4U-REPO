package com.built4u.pos.poapprover;

import com.built4u.pos.poapprover.dto.ApproverDto;
import com.built4u.pos.poapprover.dto.PoApproverDto;
import com.built4u.pos.poapprover.dto.UpdatePoApproverRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Per-user PO approver mapping admin. ADMIN-only via MOD_PO_APPROVERS. */
@RestController
@RequestMapping("/api/po-approvers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MOD_PO_APPROVERS')")
public class PoApproverController {

    private final PoApproverService service;

    /** Every active user with their current mapping (null = auto-approve). */
    @GetMapping
    public ResponseEntity<List<PoApproverDto>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    /** Users who may be picked as an approver (the owner is built-in and always present). */
    @GetMapping("/pool")
    public ResponseEntity<List<ApproverDto>> pool() {
        return ResponseEntity.ok(service.listApprovers());
    }

    /** Add a user to the approver pool. */
    @PostMapping("/pool/{userId}")
    public ResponseEntity<Void> addApprover(@PathVariable("userId") Long userId) {
        service.addApprover(userId);
        return ResponseEntity.noContent().build();
    }

    /** Remove a user from the pool (rejected for the owner, or if still routed to). */
    @DeleteMapping("/pool/{userId}")
    public ResponseEntity<Void> removeApprover(@PathVariable("userId") Long userId) {
        service.removeApprover(userId);
        return ResponseEntity.noContent().build();
    }

    /** Set or clear a user's approver. {@code approverUserId=null} reverts to auto-approve. */
    @PutMapping("/{userId}")
    public ResponseEntity<Void> update(
        @PathVariable("userId") Long userId,
        @Valid @RequestBody UpdatePoApproverRequest req
    ) {
        service.update(userId, req);
        return ResponseEntity.noContent().build();
    }
}
