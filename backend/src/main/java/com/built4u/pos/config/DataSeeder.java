package com.built4u.pos.config;

import com.built4u.pos.user.User;
import com.built4u.pos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bootstrap password-hash rewriter for the SQL-seeded {@code admin} user.
 *
 * <p>The V2 seed inserts {@code admin} with a sentinel password_hash that can't
 * validate against any bcrypt input. On the first boot per fresh schema this
 * runner rewrites it to a real bcrypt hash of
 * {@code app.security.seed-admin-password} so the account becomes usable.
 * {@code must_change_password='Y'} forces a reset on first login.
 *
 * <p>Idempotent: once rewritten, subsequent boots skip it. Fail-closed under the
 * {@code prod} profile — refuses to seed with the built-in default password.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_PLACEHOLDER = "PLACEHOLDER_REPLACED_BY_DATA_SEEDER";
    private static final String WEAK_DEFAULT_PASSWORD = "admin123";

    @Value("${app.security.seed-admin-password:admin123}")
    private String seedAdminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        List<User> toSeed = userRepository.findAll().stream()
            .filter(u -> SEED_PLACEHOLDER.equals(u.getPasswordHash()))
            .toList();

        if (toSeed.isEmpty()) {
            return;
        }

        if (environment.acceptsProfiles(Profiles.of("prod"))
                && WEAK_DEFAULT_PASSWORD.equals(seedAdminPassword)) {
            throw new IllegalStateException(
                "Refusing to seed bootstrap account(s) "
                + toSeed.stream().map(User::getUsername).toList()
                + " with the built-in default password under the 'prod' profile. "
                + "Set APP_SECURITY_SEED_ADMIN_PASSWORD to a strong value and redeploy.");
        }

        toSeed.forEach(this::rewriteHash);
    }

    private void rewriteHash(User user) {
        user.setPasswordHash(passwordEncoder.encode(seedAdminPassword));
        userRepository.save(user);
        log.warn("Seeded password rewritten for user '{}'. Change it immediately (force-change is set).",
                 user.getUsername());
    }
}
