package com.built4u.pos.common.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Persists unhandled errors into {@code pos_error_log} for in-app debugging.
 *
 * <p>{@link #record} is best-effort and MUST never throw — a failure while
 * logging must not mask the original error. It runs in a fresh
 * {@code REQUIRES_NEW} transaction so it commits even though the request's own
 * transaction is being rolled back by the exception.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ErrorLogService {

    private static final int MAX_STACK_CHARS = 20_000;
    /** Masks credential-like key=value / key: value fragments in text. */
    private static final Pattern SECRET = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|token|authorization|api[_-]?key)\\s*[=:]\\s*\\S+");

    private final ErrorLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String ref, String httpMethod, String requestPath, String username,
                       String siteCode, String siteName, Throwable ex) {
        try {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stack = redact(sw.toString());
            if (stack != null && stack.length() > MAX_STACK_CHARS) {
                stack = stack.substring(0, MAX_STACK_CHARS) + "\n… (truncated)";
            }
            repository.save(ErrorLog.builder()
                .ref(ref)
                .siteCode(trim(siteCode, 60))
                .siteName(trim(siteName, 255))
                .username(trim(username, 255))
                .httpMethod(trim(httpMethod, 10))
                .requestPath(trim(requestPath, 1000))
                .exceptionClass(trim(ex.getClass().getName(), 500))
                .message(trim(redact(ex.getMessage()), 2000))
                .stackTrace(stack)
                .build());
        } catch (Exception logFailure) {
            log.error("Failed to persist error log (ref {}): {}", ref, logFailure.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ErrorLog> recent(String siteCode, int limit) {
        int capped = Math.max(1, Math.min(limit, 500));
        String site = (siteCode == null || siteCode.isBlank()) ? null : siteCode.trim();
        return repository.search(site, PageRequest.of(0, capped));
    }

    @Transactional(readOnly = true)
    public ErrorLog get(Long id) {
        return repository.findById(id).orElse(null);
    }

    public static String redact(String s) {
        if (s == null) return null;
        return SECRET.matcher(s).replaceAll("$1=***");
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
