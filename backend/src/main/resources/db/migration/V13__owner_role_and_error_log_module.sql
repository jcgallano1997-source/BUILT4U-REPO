-- =============================================================================
-- Built4U POS — V13: business OWNER account + admin/IT split.
-- =============================================================================
-- Restructures the two default accounts:
--   admin  = System Administrator (IT / break-glass superuser; wildcard, keeps
--            EVERYTHING incl. the error log). Unchanged here.
--   owner  = Business Owner (master account; supervises ALL sites; has every
--            module EXCEPT the error log, which stays IT-only). New here.
--
-- Also splits the error log into its own ERROR_LOG module (previously it shared
-- AUDIT_LOG), so the OWNER role can have the audit log without the error log.
--
-- PO approvals: owner is the business approver going forward. Per-staff routing
-- (creator -> owner) is configured in the PO Approvers admin screen as staff are
-- added; admin stays a break-glass approver and is not wired into routing here.
-- All objects live in the BUILT4U schema. FREEPOS is never touched.
-- =============================================================================


-- =============================================================================
-- SECTION 1 — New ERROR_LOG module (admin/IT-only; see ErrorLogController)
-- =============================================================================
INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('ERROR_LOG', 'Error Log', 'Server error diagnostics - IT/admin only', 281);


-- =============================================================================
-- SECTION 2 — OWNER role (business master: every module except ERROR_LOG)
-- =============================================================================
INSERT INTO pos_role (code, name, description, built_in, wildcard)
VALUES ('OWNER', 'Business Owner', 'Master account - all sites, all modules except the error log', 'Y', 'N');

-- Grant OWNER every catalog module EXCEPT the IT-only error log.
INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code = 'OWNER'
   AND m.code <> 'ERROR_LOG';


-- =============================================================================
-- SECTION 3 — owner user (placeholder hash; DataSeeder rehashes at boot,
--             must_change_password forces a reset on first login)
-- =============================================================================
INSERT INTO pos_user (username, password_hash, full_name, email, must_change_password, created_by)
VALUES ('owner', 'PLACEHOLDER_REPLACED_BY_DATA_SEEDER', 'Business Owner', NULL, 'Y', 'SYSTEM');

INSERT INTO pos_user_role (user_id, role_id)
SELECT u.id, r.id FROM pos_user u, pos_role r
 WHERE u.username = 'owner' AND r.code = 'OWNER';

-- Link owner to EVERY existing site (supervises all sites). New sites created
-- later are auto-linked to owner by SiteAdminService.create().
INSERT INTO pos_user_site (user_id, site_id)
SELECT u.id, s.id FROM pos_user u, pos_site s
 WHERE u.username = 'owner';
