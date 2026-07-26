package com.built4u.pos.common.audit.dto;

import com.built4u.pos.common.audit.AuditLog;

import java.time.LocalDateTime;

public record AuditLogDto(
    Long id,
    Long siteId,
    String username,
    LocalDateTime occurredAt,
    String entityName,
    String entityId,
    String action,
    String module,
    String reference,
    String changes
) {
    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(
            a.getId(), a.getSiteId(), a.getUsername(), a.getOccurredAt(),
            a.getEntityName(), a.getEntityId(), a.getAction(),
            a.getModule(), a.getReference(), a.getChanges());
    }
}
