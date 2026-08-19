package com.built4u.pos.poapprover;

import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.poapprover.dto.ApproverDto;
import com.built4u.pos.poapprover.dto.PoApproverDto;
import com.built4u.pos.poapprover.dto.UpdatePoApproverRequest;
import com.built4u.pos.user.Role;
import com.built4u.pos.user.User;
import com.built4u.pos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Role that is always eligible to approve and can't be removed from the pool. */
    private static final String OWNER_ROLE = "OWNER";

    private final PoApproverRepository repository;
    private final PoApproverPoolRepository poolRepository;
    private final UserRepository userRepository;

    /** The business owner: eligible to approve without a pool row, and never removable. */
    private static boolean isOwner(User u) {
        return u.getRoles().stream().anyMatch(r -> OWNER_ROLE.equalsIgnoreCase(r.getCode()));
    }

    /** The IT/admin account (wildcard role) — an ops login, not a business approver. */
    private static boolean isItAdmin(User u) {
        return u.getRoles().stream().anyMatch(Role::isWildcard);
    }

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
            .filter(u -> !isItAdmin(u))   // the IT account doesn't raise business POs
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

    // ── Approver pool (who may be picked as an approver) ───────────────────

    /**
     * Everyone eligible to approve: the business owner (built-in) plus whoever
     * has been added to the pool. Owner first, then by name.
     */
    @Transactional(readOnly = true)
    public List<ApproverDto> listApprovers() {
        Set<Long> pool = poolRepository.findAll().stream()
            .map(PoApproverPool::getUserId).collect(Collectors.toSet());
        return userRepository.findAllByOrderByUsernameAsc().stream()
            .filter(User::isActive)
            .filter(u -> !isItAdmin(u))
            .filter(u -> isOwner(u) || pool.contains(u.getId()))
            .map(u -> new ApproverDto(u.getId(), u.getUsername(), u.getFullName(), isOwner(u)))
            .sorted(Comparator.comparing(ApproverDto::builtIn).reversed()
                .thenComparing(ApproverDto::fullName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /** Add a user to the approver pool. The owner is already eligible; the IT account can't be. */
    @Transactional
    public void addApprover(Long userId) {
        User u = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (!u.isActive()) throw new BadRequestException("User " + u.getUsername() + " is inactive.");
        if (isItAdmin(u)) {
            throw new BadRequestException(
                "The system administrator is an IT account and cannot be a PO approver.");
        }
        if (isOwner(u) || poolRepository.existsById(userId)) return;   // already eligible
        poolRepository.save(PoApproverPool.builder().userId(userId).build());
        log.info("PO approver pool: added {}", u.getUsername());
    }

    /**
     * Remove a user from the approver pool. The owner is built-in and can't be
     * removed, and neither can anyone still routed to — clearing that silently
     * would drop those POs to auto-approve without telling anyone.
     */
    @Transactional
    public void removeApprover(Long userId) {
        User u = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (isOwner(u)) {
            throw new BadRequestException(
                "The business owner is a built-in approver and cannot be removed.");
        }
        List<String> routed = repository.findByApproverUserId(userId).stream()
            .map(m -> userRepository.findById(m.getUserId()).map(User::getUsername).orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
        if (!routed.isEmpty()) {
            throw new BadRequestException(
                "Still the approver for " + String.join(", ", routed)
                    + ". Route them elsewhere first.");
        }
        poolRepository.deleteById(userId);
        log.info("PO approver pool: removed {}", u.getUsername());
    }

    /**
     * Set or clear a user's approver. {@code approverUserId=null} deletes the
     * row (= revert to auto-approve). A user may not be their own approver, and
     * the approver must be active and in the approver pool.
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
        if (!isOwner(approver) && !poolRepository.existsById(approverId)) {
            throw new BadRequestException(
                approver.getUsername() + " is not a PO approver. Add them as an approver first.");
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
