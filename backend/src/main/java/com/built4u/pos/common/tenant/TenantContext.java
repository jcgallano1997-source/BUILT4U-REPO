package com.built4u.pos.common.tenant;

/**
 * Holds the active SITE for the current request thread.
 *
 * <p>Single-business model: there is no tenant/entity layer, so the isolation
 * key is {@code site_id}. Populated by
 * {@link com.built4u.pos.auth.JwtAuthenticationFilter} from the JWT after
 * signature verification (context is derived from the security context ONLY,
 * never from request params), and cleared at the end of every request to avoid
 * leaking across thread-pool reuse.
 */
public final class TenantContext {

    private static final ThreadLocal<Long>   SITE_ID   = new ThreadLocal<>();
    private static final ThreadLocal<String> SITE_CODE = new ThreadLocal<>();
    private static final ThreadLocal<String> SITE_NAME = new ThreadLocal<>();

    private TenantContext() {}

    public static void setSiteId(Long siteId)   { SITE_ID.set(siteId); }
    public static Long getSiteId()              { return SITE_ID.get(); }
    public static void setSiteCode(String code) { SITE_CODE.set(code); }
    public static String getSiteCode()          { return SITE_CODE.get(); }
    public static void setSiteName(String name) { SITE_NAME.set(name); }
    public static String getSiteName()          { return SITE_NAME.get(); }

    public static Long requireSiteId() {
        Long id = SITE_ID.get();
        if (id == null) {
            throw new IllegalStateException("No site context — request was not authenticated");
        }
        return id;
    }

    public static void clear() {
        SITE_ID.remove();
        SITE_CODE.remove();
        SITE_NAME.remove();
    }
}
