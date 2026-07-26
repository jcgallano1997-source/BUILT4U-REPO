package com.built4u.pos.auth;

import com.built4u.pos.auth.dto.*;
import com.built4u.pos.auth.exception.AuthException;
import com.built4u.pos.site.Site;
import com.built4u.pos.user.User;
import com.built4u.pos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private static final Pattern PASSWORD_RULE =
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionService permissionService;

    @Value("${app.jwt.refresh-ttl-ms:604800000}")
    private long refreshTtlMs;

    @Value("${app.security.password-max-age-days:90}")
    private long passwordMaxAgeDays;

    /** Public: list active sites the given username can sign into. Used by login UX. */
    @Transactional(readOnly = true)
    public List<SiteDto> sitesFor(String username) {
        return userRepository.findByUsername(username)
            .filter(User::isActive)
            .map(u -> u.getSites().stream()
                .filter(Site::isActive)
                .sorted(Comparator.comparing(Site::getCode))
                .map(SiteDto::from)
                .toList())
            .orElse(List.of());
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
            .orElseThrow(() -> {
                log.info("Login failed — no user '{}'", req.username());
                return new AuthException("Invalid username or password");
            });

        if (!user.isActive()) {
            log.info("Login blocked — user '{}' inactive", user.getUsername());
            throw new AuthException("Account is inactive");
        }

        if (user.isLocked()) {
            log.info("Login blocked — user '{}' locked until {}", user.getUsername(), user.getLockedUntil());
            throw new AuthException("Account is locked. Try again later.");
        }

        // Resolve the site from the user's own granted sites (never a global
        // lookup) — safer and avoids picking a site the user can't access.
        Site site = user.getSites().stream()
            .filter(Site::isActive)
            .filter(s -> s.getCode().equals(req.siteCode()))
            .findFirst()
            .orElseThrow(() -> {
                log.info("User '{}' has no active access to site code '{}'", user.getUsername(), req.siteCode());
                return new AuthException("Invalid site or no access");
            });

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new AuthException("Invalid username or password");
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return issueTokens(user, site);
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest req) {
        String hash = jwtService.hashRefreshToken(req.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new AuthException("Refresh token expired or revoked");
        }

        Site site = stored.getSite();
        if (site == null || !site.isActive()) {
            throw new AuthException("Site no longer available");
        }

        // Rotate: revoke the presented token, issue a fresh pair.
        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser(), site);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            if (rt.getRevokedAt() == null) {
                rt.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(rt);
            }
        });
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthException("User not found"));
        return UserDto.from(user, isPasswordExpired(user), permissionService.effectiveModules(user));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest req) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthException("User not found"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }

        if (!PASSWORD_RULE.matcher(req.newPassword()).matches()) {
            throw new AuthException("New password must be at least 8 characters with at least one letter and one digit");
        }

        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new AuthException("New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword("N");
        user.setUpdatedBy(username);
        userRepository.save(user);

        refreshTokenRepository.revokeAllForUser(user, LocalDateTime.now());
        log.info("Password changed for user '{}'", username);
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            user.setFailedAttempts(0);
            log.info("User '{}' locked after {} failed attempts", user.getUsername(), MAX_FAILED_ATTEMPTS);
        }
        userRepository.save(user);
    }

    private LoginResponse issueTokens(User user, Site site) {
        String accessToken = jwtService.generateAccessToken(user, site);
        String refreshRaw = jwtService.generateRefreshTokenRaw();

        RefreshToken rt = RefreshToken.builder()
            .user(user)
            .site(site)
            .tokenHash(jwtService.hashRefreshToken(refreshRaw))
            .expiresAt(LocalDateTime.now().plus(refreshTtlMs, ChronoUnit.MILLIS))
            .createdAt(LocalDateTime.now())
            .build();
        refreshTokenRepository.save(rt);

        return new LoginResponse(accessToken, refreshRaw,
            UserDto.from(user, isPasswordExpired(user), permissionService.effectiveModules(user)),
            SiteDto.from(site));
    }

    /** Pure runtime check — does NOT mutate the DB. Toggling config back stops forcing rotation. */
    private boolean isPasswordExpired(User user) {
        if (passwordMaxAgeDays <= 0) return false;
        LocalDateTime changed = user.getPasswordChangedAt();
        if (changed == null) return false;
        return changed.plusDays(passwordMaxAgeDays).isBefore(LocalDateTime.now());
    }
}
