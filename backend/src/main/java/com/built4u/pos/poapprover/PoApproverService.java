package com.built4u.pos.poapprover;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.poapprover.dto.PoApproverDto;
import com.built4u.pos.poapprover.dto.UpdatePoApproverRequest;
import com.built4u.pos.user.User;
import com.built4u.pos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-user PO approver mapping. Consulted at PO creation time and at the
 * Approve button — see {@link com.built4u.pos.purchaseorder.PurchaseOrderService}.
 *
 * <p>Single-business: users are business-global (no entity/tenant layer), so
 * the mapping is business-wide too. Defaults to "auto-approve" when no row
 * exists; ADMIN opts users in one at a time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoApproverService {

    private final PoApproverRepository repository;
    private final UserRepository userRepository;

    /** Admin list — every active user with their mapping (null = auto-approve). */
    @Transactional(readOnly = true)
    public List<PoApproverDto> listAll() {
        List<User> users = userRepository.findAllByOrderByUsernameAsc();
        Map<Long, User> userById = new HashMap<>();
        for (User u : users) userById.put(u.getId(), u);

        Map<Long, PoApprover> mappingByUser = new HashMap<>();
        for (PoApprover row : repository.findAll()) {
            mappingByUser.put(row.getUserId(), row);
        }

        return users.stream()
            .filter(User::isActive)
            .map(u -> {
                PoApprover m = mappingByUser.get(u.getId());
                Long approverId = m == null ? null : m.getApproverUserId();
                User approver = approverId == null ? null : userById.get(approverId);
                return new PoApproverDto(
                    u.getId(), u.getUsername(), u.getFullName(),
                    approver == null ? null : approver.getId(),
                    approver == null ? null : approver.getUsername(),
                    approver == null ? null : approver.getFullName()
                );
            }).toList();
    }

    /**
     * Set or clear a user's approver. {@code approverUserId=null} deletes the
     * row (= revert to auto-approve). A user may not be their own approver, and
     * the approver must be active.
     */
    @Transactional
    public void update(Long userId, UpdatePoApproverRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        Long approverId = req.approverUserId();

        if (approverId == null) {
            repository.deleteById(userId);
            log.info("PO approver mapping for {} cleared (now auto-approves)", user.getUsername());
            return;
        }
        if (approverId.equals(userId)) {
            throw new BadRequestException(
                "A user cannot be their own approver. Leave blank to auto-approve.");
        }
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new NotFoundException("Approver " + approverId + " not found"));
        if (!approver.isActive()) {
            throw new BadRequestException("Approver " + approver.getUsername() + " is inactive.");
        }

        PoApprover row = repository.findById(userId)
            .orElseGet(() -> PoApprover.builder().userId(userId).build());
        row.setApproverUserId(approverId);
        repository.save(row);
        log.info("PO approver mapping: {} → {}", user.getUsername(), approver.getUsername());
    }

    // ── Hot-path helpers (called by PurchaseOrderService) ──────────────────

    /**
     * Resolve the approver USER for the given creator's username. Empty when no
     * row exists or the configured approver is inactive (caller treats either
     * case as "auto-approve").
     */
    @Transactional(readOnly = true)
    public Optional<User> resolveApproverFor(String creatorUsername) {
        return userRepository.findByUsername(creatorUsername)
            .flatMap(u -> repository.findById(u.getId()))
            .flatMap(m -> userRepository.findById(m.getApproverUserId()))
            .filter(User::isActive);
    }

    /** True when {@code candidateUsername} is the designated approver for {@code creatorUsername}. */
    @Transactional(readOnly = true)
    public boolean isApproverFor(String candidateUsername, String creatorUsername) {
        return resolveApproverFor(creatorUsername)
            .map(approver -> approver.getUsername().equalsIgnoreCase(candidateUsername))
            .orElse(false);
    }

    /** All creators that route to the given approver. Used by "pending my approval". */
    @Transactional(readOnly = true)
    public List<String> creatorsRoutingTo(String approverUsername) {
        return userRepository.findByUsername(approverUsername)
            .map(approver -> repository.findByApproverUserId(approver.getId()).stream()
                .map(m -> userRepository.findById(m.getUserId()).map(User::getUsername).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList())
            .orElseGet(List::of);
    }

    /** Convenience: true when the JWT principal holds the ROLE_ADMIN authority. */
    public static boolean currentUserIsAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Convenience: the JWT principal's username (or {@code "SYSTEM"}). */
    public static String currentUsername() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            return "SYSTEM";
        }
        return a.getName();
    }
}
