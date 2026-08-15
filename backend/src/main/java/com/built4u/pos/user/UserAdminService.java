package com.built4u.pos.user;

import com.built4u.pos.auth.AuthUtils;
import com.built4u.pos.auth.RefreshTokenRepository;
import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.site.Site;
import com.built4u.pos.site.SiteRepository;
import com.built4u.pos.user.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin operations on {@link User}: list / create / update / reset-password,
 * plus read-only role + site lists for the user form's pickers.
 *
 * <p>Password handling: never returns the hash. {@code create} accepts an initial
 * password (BCrypt-hashed). {@code update} cannot change the password — that's
 * {@link #resetPassword}, which also revokes all refresh tokens (forced re-login).
 * Username is immutable after creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SiteRepository siteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserSummaryDto> list(String search, boolean includeInactive) {
        String needle = search == null ? "" : search.toLowerCase().trim();
        boolean admin = AuthUtils.isCurrentUserAdmin();
        return userRepository.findAllByOrderByUsernameAsc().stream()
            // IT/admin accounts are invisible to everyone but another admin.
            .filter(u -> admin || u.getRoles().stream().noneMatch(Role::isWildcard))
            .filter(u -> includeInactive || u.isActive())
            .filter(u -> needle.isEmpty()
                || u.getUsername().toLowerCase().contains(needle)
                || u.getFullName().toLowerCase().contains(needle)
                || (u.getEmail() != null && u.getEmail().toLowerCase().contains(needle)))
            .map(UserAdminService::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserDetailDto get(Long id) {
        User u = loadUser(id);
        // Don't let a non-admin open an IT/admin account (even by guessing the id).
        if (!AuthUtils.isCurrentUserAdmin() && u.getRoles().stream().anyMatch(Role::isWildcard)) {
            throw new NotFoundException("User " + id + " not found");
        }
        return toDetail(u);
    }

    @Transactional
    public UserDetailDto create(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.username().trim())) {
            throw new ConflictException("Username '" + req.username() + "' is already taken");
        }
        String email = normalizeEmail(req.email());
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email '" + email + "' is already in use");
        }
        Set<Role> roles = resolveRoles(req.roleCodes());
        Set<Site> sites = resolveSites(req.siteCodes());

        boolean forceChange = !Boolean.FALSE.equals(req.forceChangeOnFirstLogin());
        String actor = currentUsername();
        LocalDateTime now = LocalDateTime.now();

        User u = User.builder()
            .username(req.username().trim())
            .fullName(req.fullName().trim())
            .email(email)
            .passwordHash(passwordEncoder.encode(req.initialPassword()))
            .active("Y")
            .failedAttempts(0)
            .passwordChangedAt(now)
            .mustChangePassword(forceChange ? "Y" : "N")
            .createdAt(now)
            .createdBy(actor)
            .roles(roles)
            .sites(sites)
            .build();
        User saved = userRepository.save(u);
        log.info("Admin {} created user {} roles={} sites={}", actor, saved.getUsername(), req.roleCodes(), req.siteCodes());
        return toDetail(saved);
    }

    @Transactional
    public UserDetailDto update(Long id, UpdateUserRequest req) {
        User u = loadUser(id);
        Set<Role> newRoles = resolveRoles(req.roleCodes());
        boolean willBeActive = Boolean.TRUE.equals(req.active());

        // Last-administrator guard: refuse a change that would leave no active admin.
        boolean keepsAdmin = willBeActive && newRoles.stream().anyMatch(Role::isWildcard);
        if (!keepsAdmin && userRepository.countOtherActiveAdmins(id) == 0) {
            throw new BadRequestException(
                "This change would leave the system with no administrator. "
                + "Assign the ADMIN role to another active user first.");
        }

        u.setFullName(req.fullName().trim());
        String email = normalizeEmail(req.email());
        if (email != null) {
            var byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent() && !byEmail.get().getId().equals(u.getId())) {
                throw new ConflictException("Email '" + email + "' is already in use");
            }
        }
        u.setEmail(email);
        u.setActive(willBeActive ? "Y" : "N");
        u.setRoles(newRoles);
        u.setSites(resolveSites(req.siteCodes()));
        u.setUpdatedAt(LocalDateTime.now());
        u.setUpdatedBy(currentUsername());

        if (!u.isActive()) {
            int revoked = refreshTokenRepository.revokeAllForUser(u, LocalDateTime.now());
            if (revoked > 0) log.info("Deactivated user {} — revoked {} refresh token(s)", u.getUsername(), revoked);
        }

        return toDetail(userRepository.save(u));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        User u = loadUser(id);
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setMustChangePassword(Boolean.FALSE.equals(req.forceChangeOnNextLogin()) ? "N" : "Y");
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        u.setUpdatedAt(LocalDateTime.now());
        u.setUpdatedBy(currentUsername());
        userRepository.save(u);

        int revoked = refreshTokenRepository.revokeAllForUser(u, LocalDateTime.now());
        log.info("Admin {} reset password for user {}; {} refresh token(s) revoked",
            currentUsername(), u.getUsername(), revoked);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        boolean admin = AuthUtils.isCurrentUserAdmin();
        return roleRepository.findAll().stream()
            .filter(r -> admin || !r.isWildcard())   // hide the ADMIN role from non-admins
            .sorted(Comparator.comparing(Role::getCode))
            .map(r -> new RoleDto(r.getId(), r.getCode(), r.getName(), r.getDescription()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SiteRefDto> listSites() {
        return siteRepository.findAllByOrderByCodeAsc().stream()
            .filter(Site::isActive)
            .map(s -> new SiteRefDto(s.getId(), s.getCode(), s.getName()))
            .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User loadUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    private Set<Role> resolveRoles(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        boolean admin = AuthUtils.isCurrentUserAdmin();
        Set<Role> result = new HashSet<>();
        for (String code : codes) {
            Role r = roleRepository.findByCode(code.trim())
                .orElseThrow(() -> new BadRequestException("Unknown role code: " + code));
            // A non-admin must never be able to grant the IT/admin role.
            if (!admin && r.isWildcard()) {
                throw new BadRequestException("You are not allowed to assign the ADMIN role.");
            }
            result.add(r);
        }
        return result;
    }

    private Set<Site> resolveSites(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new BadRequestException("At least one site is required");
        }
        Set<Site> result = new HashSet<>();
        for (String code : codes) {
            Site s = siteRepository.findByCode(code.trim())
                .orElseThrow(() -> new BadRequestException("Unknown site code: " + code));
            result.add(s);
        }
        return result;
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private static String normalizeEmail(String s) {
        return s == null || s.isBlank() ? null : s.trim().toLowerCase();
    }

    private static UserSummaryDto toSummary(User u) {
        return new UserSummaryDto(
            u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
            u.isActive(), u.isLocked(), u.isMustChangePassword(),
            u.getRoles().stream().map(Role::getCode).sorted().toList(),
            u.getSites().stream().map(Site::getCode).sorted().toList(),
            u.getLastLoginAt(), u.getCreatedAt());
    }

    private static UserDetailDto toDetail(User u) {
        return new UserDetailDto(
            u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
            u.isActive(), u.isLocked(), u.isMustChangePassword(),
            u.getFailedAttempts() == null ? 0 : u.getFailedAttempts(),
            u.getLockedUntil(), u.getPasswordChangedAt(), u.getLastLoginAt(),
            u.getCreatedAt(), u.getCreatedBy(), u.getUpdatedAt(), u.getUpdatedBy(),
            u.getRoles().stream().sorted(Comparator.comparing(Role::getCode))
                .map(r -> new RoleDto(r.getId(), r.getCode(), r.getName(), r.getDescription())).toList(),
            u.getSites().stream().sorted(Comparator.comparing(Site::getCode))
                .map(s -> new SiteRefDto(s.getId(), s.getCode(), s.getName())).toList());
    }
}
