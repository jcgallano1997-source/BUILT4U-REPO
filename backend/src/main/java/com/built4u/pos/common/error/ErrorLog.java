package com.built4u.pos.common.error;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A persisted unhandled-error record ({@code pos_error_log}). Written by the
 * global exception handler for in-app, per-site debugging by an admin. Message
 * and stack trace are redacted of credential-like values before persistence.
 */
@Entity
@Table(name = "pos_error_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String ref;

    @Column(name = "occurred_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "site_code", length = 60)
    private String siteCode;

    @Column(name = "site_name", length = 255)
    private String siteName;

    @Column(length = 255)
    private String username;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_path", length = 1000)
    private String requestPath;

    @Column(name = "exception_class", length = 500)
    private String exceptionClass;

    @Column(length = 2000)
    private String message;

    @Lob
    @Column(name = "stack_trace")
    private String stackTrace;
}
