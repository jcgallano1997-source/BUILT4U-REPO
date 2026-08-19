package com.built4u.pos.poapprover;

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
