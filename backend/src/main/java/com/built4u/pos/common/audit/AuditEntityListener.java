package com.built4u.pos.common.audit;

import com.built4u.pos.common.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Universal business audit trail. Registers as a Hibernate post-insert /
 * post-update / post-delete listener and writes one {@code pos_audit_log} row
 * per entity change with the changed fields (before→after), the JWT user, the
 * site, and the optional {@link AuditContext} business reference.
 *
 * <p>Writes via {@link JdbcTemplate} (joins the in-flight transaction, so a
 * rolled-back business tx discards its audit rows too — and it never
 * re-triggers this listener). Resilient: any failure is logged and swallowed so
 * auditing can never break a business write. Sensitive field values are
 * redacted; high-volume internal logs (transaction log, loyalty ledger, auth
 * tokens) and the audit row itself are skipped so this stays a business-CRUD
 * trail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEntityListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Set<String> SENSITIVE =
        Set.of("passwordhash", "tokenhash", "password", "secret");
    /** Entities excluded entirely — noisy append-only logs + the audit row itself. */
    private static final Set<String> SKIP_ENTITIES =
        Set.of("AuditLog", "TransactionLog", "LoyaltyLedger", "RefreshToken", "PasswordResetToken");
    private static final int MAX_VALUE_LEN = 1000;
    private static final String INSERT_SQL =
        "INSERT INTO pos_audit_log (site_id, username, entity_name, entity_id, "
        + "action, module, reference, changes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final EntityManagerFactory entityManagerFactory;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void register() {
        SessionFactoryImplementor sf = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sf.getServiceRegistry().requireService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
        log.info("AuditEntityListener registered (universal audit log active)");
    }

    @Override
    public void onPostInsert(PostInsertEvent e) {
        safe(() -> {
            EntityPersister p = e.getPersister();
            String name = simpleName(p.getEntityName());
            if (SKIP_ENTITIES.contains(name)) return;
            List<Map<String, Object>> changes = new ArrayList<>();
            String[] props = p.getPropertyNames();
            Object[] state = e.getState();
            for (int i = 0; i < props.length; i++) {
                Object v = formatted(props[i], state[i]);
                if (v == SKIP) continue;
                changes.add(Map.of("field", props[i], "old", "", "new", str(v)));
            }
            write(name, entityId(e.getEntity(), e.getId()), "CREATE", changes);
        });
    }

    @Override
    public void onPostUpdate(PostUpdateEvent e) {
        safe(() -> {
            EntityPersister p = e.getPersister();
            String name = simpleName(p.getEntityName());
            if (SKIP_ENTITIES.contains(name)) return;
            String[] props = p.getPropertyNames();
            Object[] old = e.getOldState();
            Object[] cur = e.getState();
            int[] dirty = e.getDirtyProperties();
            List<Integer> idx = new ArrayList<>();
            if (dirty != null) {
                for (int d : dirty) idx.add(d);
            } else if (old != null) {
                for (int i = 0; i < props.length; i++) {
                    if (!Objects.equals(old[i], cur[i])) idx.add(i);
                }
            }
            List<Map<String, Object>> changes = new ArrayList<>();
            for (int i : idx) {
                Object ov = old == null ? null : formatted(props[i], old[i]);
                Object nv = formatted(props[i], cur[i]);
                if (ov == SKIP && nv == SKIP) continue;
                changes.add(Map.of("field", props[i],
                    "old", ov == SKIP ? "" : str(ov),
                    "new", nv == SKIP ? "" : str(nv)));
            }
            if (changes.isEmpty()) return;
            write(name, entityId(e.getEntity(), e.getId()), "UPDATE", changes);
        });
    }

    @Override
    public void onPostDelete(PostDeleteEvent e) {
        safe(() -> {
            EntityPersister p = e.getPersister();
            String name = simpleName(p.getEntityName());
            if (SKIP_ENTITIES.contains(name)) return;
            List<Map<String, Object>> changes = new ArrayList<>();
            String[] props = p.getPropertyNames();
            Object[] state = e.getDeletedState();
            if (state != null) {
                for (int i = 0; i < props.length; i++) {
                    Object v = formatted(props[i], state[i]);
                    if (v == SKIP) continue;
                    changes.add(Map.of("field", props[i], "old", str(v), "new", ""));
                }
            }
            write(name, entityId(e.getEntity(), e.getId()), "DELETE", changes);
        });
    }

    // Fire during flush (inside the business tx) so audit rows are tx-consistent.
    @Override public boolean requiresPostCommitHandling(EntityPersister p) { return false; }

    // ── internals ───────────────────────────────────────────────────────────

    private static final Object SKIP = new Object();

    private void write(String entity, String id, String action, List<Map<String, Object>> changes) {
        String json;
        try {
            json = changes.isEmpty() ? null : objectMapper.writeValueAsString(changes);
        } catch (Exception ex) {
            json = null;
        }
        final String body = json;
        jdbcTemplate.update(INSERT_SQL, ps -> {
            Long site = TenantContext.getSiteId();
            if (site == null) ps.setNull(1, java.sql.Types.NUMERIC);
            else ps.setLong(1, site);
            ps.setString(2, username());
            ps.setString(3, entity);
            ps.setString(4, id == null ? null : trunc(id, 100));
            ps.setString(5, action);
            ps.setString(6, AuditContext.module());
            ps.setString(7, AuditContext.reference());
            ps.setString(8, body);
        });
    }

    private static String username() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            return "SYSTEM";
        }
        return trunc(a.getName(), 50);
    }

    /** Returns a redacted/stringifiable value, or {@link #SKIP} for relations/collections. */
    private Object formatted(String prop, Object value) {
        if (prop != null && SENSITIVE.contains(prop.toLowerCase())) return "***";
        if (value == null) return null;
        if (value instanceof CharSequence || value instanceof Number
            || value instanceof Boolean || value instanceof Character
            || value instanceof Enum<?> || value instanceof Temporal
            || value instanceof java.util.Date) {
            return value;
        }
        return SKIP; // entity association / collection — don't walk the graph
    }

    private static String str(Object v) {
        if (v == null) return "";
        return trunc(String.valueOf(v), MAX_VALUE_LEN);
    }

    private static String trunc(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    /**
     * Readable, queryable id. Single {@code @Id} → "id=5"; {@code @IdClass}
     * composite → "itemId=42;siteId=1" (sorted). Falls back to the raw
     * persister id.
     */
    private static String entityId(Object entity, Object fallback) {
        try {
            if (entity != null) {
                List<String> parts = new ArrayList<>();
                Class<?> c = entity.getClass();
                while (c != null && c != Object.class) {
                    for (Field f : c.getDeclaredFields()) {
                        if (f.isAnnotationPresent(Id.class) || f.isAnnotationPresent(EmbeddedId.class)) {
                            f.setAccessible(true);
                            parts.add(f.getName() + "=" + f.get(entity));
                        }
                    }
                    c = c.getSuperclass();
                }
                if (!parts.isEmpty()) {
                    Collections.sort(parts);
                    return trunc(String.join(";", parts), 100);
                }
            }
        } catch (Exception ignore) {
            // fall through to the persister id
        }
        return fallback == null ? null : trunc(String.valueOf(fallback), 100);
    }

    private static String simpleName(String entityName) {
        if (entityName == null) return "";
        int dot = entityName.lastIndexOf('.');
        return dot < 0 ? entityName : entityName.substring(dot + 1);
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            log.warn("Audit capture failed (swallowed): {}", t.toString());
        }
    }
}
