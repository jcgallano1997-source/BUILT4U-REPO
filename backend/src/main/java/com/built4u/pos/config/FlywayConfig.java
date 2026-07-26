package com.built4u.pos.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Oracle has no transactional DDL, so a migration that fails partway leaves its
 * already-executed statements committed <em>and</em> a {@code success = 0} row in
 * {@code flyway_schema_history}. That dead marker then blocks every subsequent
 * {@code migrate()} with "Detected failed migration to version N".
 *
 * <p>{@code repair()} clears failed entries and realigns checksums so a corrected,
 * re-runnable migration can apply on the next boot. A genuinely broken migration
 * still throws and still fails startup — this only removes the marker that would
 * otherwise permanently wedge the deploy.
 *
 * <p>Trade-off: repair also re-baselines checksums, so migrations are treated as
 * append-only. Drop this bean if strict checksum enforcement is preferred.
 */
@Configuration
public class FlywayConfig {

    @Bean
    FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
