package com.built4u.pos.common.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One recorded entity change ({@code pos_audit_log}). Written by
 * {@link AuditEntityListener} via JDBC (so it never re-triggers the listener);
 * this entity exists for the read/report side.
 */
@Entity
@Table(name = "pos_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id")
    private Long siteId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "occurred_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(nullable = false, length = 10)
    private String action;

    @Column(length = 60)
    private String module;

    @Column(length = 200)
    private String reference;

    @Lob
    @Column
    private String changes;
}
