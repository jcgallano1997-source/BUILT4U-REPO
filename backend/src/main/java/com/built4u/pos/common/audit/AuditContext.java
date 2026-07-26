package com.built4u.pos.common.audit;

/**
 * Optional per-thread business context for audit rows. A service may set a
 * module + human reference (e.g. "Stock Transfer", "Ship ST-2026-0001") around
 * a unit of work so {@link AuditEntityListener} can stamp WHY a change
 * happened, not just WHAT changed. Always cleared in a finally block.
 */
public final class AuditContext {

    private record Ctx(String module, String reference) {}

    private static final ThreadLocal<Ctx> TL = new ThreadLocal<>();

    private AuditContext() {}

    public static void set(String module, String reference) {
        TL.set(new Ctx(trim(module, 60), trim(reference, 200)));
    }

    public static String module() {
        Ctx c = TL.get();
        return c == null ? null : c.module();
    }

    public static String reference() {
        Ctx c = TL.get();
        return c == null ? null : c.reference();
    }

    public static void clear() {
        TL.remove();
    }

    private static String trim(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
