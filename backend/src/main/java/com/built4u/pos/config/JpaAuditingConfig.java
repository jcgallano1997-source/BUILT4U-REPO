package com.built4u.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate}
 * populate automatically, and wires {@code @CreatedBy}/{@code @LastModifiedBy} to
 * the current authenticated username.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null || auth.getName().isBlank()
                || "anonymousUser".equals(auth.getName())) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
