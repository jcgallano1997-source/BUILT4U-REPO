package com.built4u.pos.common.audit;

import com.built4u.pos.common.audit.dto.AuditLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repo;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> list(String username, String entity, String action,
                                  String from, String to, String q, Pageable pageable) {
        LocalDate fromD = parseDate(from);
        LocalDate toD = parseDate(to);
        return repo.search(
            like(username),
            like(entity),
            blankUpper(action),
            fromD == null ? null : fromD.atStartOfDay(),
            toD == null ? null : toD.atTime(LocalTime.MAX),
            like(q),
            pageable
        ).map(AuditLogDto::from);
    }

    private static String like(String s) {
        if (s == null || s.isBlank()) return null;
        return "%" + s.trim().toLowerCase() + "%";
    }

    private static String blankUpper(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase();
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
