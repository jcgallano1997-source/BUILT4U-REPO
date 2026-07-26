-- =============================================================================
-- Built4U POS (single-business) — V10: business audit log.
-- =============================================================================
-- One row per entity change, written by a Hibernate post-insert/update/delete
-- listener via JDBC (so it joins the business transaction and rolls back with
-- it, and never re-triggers itself). site_id/username/module/reference give the
-- WHO/WHERE/WHY; changes is a JSON array of {field, old, new}. High-volume
-- internal logs (transaction log, loyalty ledger, tokens) are excluded by the
-- listener, so this stays a business-CRUD trail. TIMESTAMP audit dates.
-- =============================================================================

CREATE TABLE pos_audit_log (
  id            NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id       NUMBER,
  username      VARCHAR2(50 CHAR)  NOT NULL,
  occurred_at   TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  entity_name   VARCHAR2(120 CHAR) NOT NULL,
  entity_id     VARCHAR2(100 CHAR),
  action        VARCHAR2(10 CHAR)  NOT NULL,
  module        VARCHAR2(60 CHAR),
  reference     VARCHAR2(200 CHAR),
  changes       CLOB,
  CONSTRAINT ck_pos_audit_log_action CHECK (action IN ('CREATE','UPDATE','DELETE'))
);
CREATE INDEX ix_pos_audit_log_occurred ON pos_audit_log (occurred_at DESC);
CREATE INDEX ix_pos_audit_log_entity   ON pos_audit_log (entity_name);
CREATE INDEX ix_pos_audit_log_user     ON pos_audit_log (username);
