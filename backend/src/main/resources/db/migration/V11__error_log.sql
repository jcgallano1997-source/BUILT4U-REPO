-- =============================================================================
-- Built4U POS (single-business) — V11: persistent error log.
-- =============================================================================
-- One row per unhandled 5xx, written by the global exception handler in its own
-- REQUIRES_NEW transaction (so it commits even though the request's tx rolls
-- back). Gives the operator in-app debugging without trawling server logs. The
-- short ref is echoed to the client so a user can quote it. Message/stack are
-- redacted of credential-like values before persistence. TIMESTAMP dates.
-- =============================================================================

CREATE TABLE pos_error_log (
  id               NUMBER              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ref              VARCHAR2(8 CHAR)    NOT NULL,
  occurred_at      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
  site_code        VARCHAR2(60 CHAR),
  site_name        VARCHAR2(255 CHAR),
  username         VARCHAR2(255 CHAR),
  http_method      VARCHAR2(10 CHAR),
  request_path     VARCHAR2(1000 CHAR),
  exception_class  VARCHAR2(500 CHAR),
  message          VARCHAR2(2000 CHAR),
  stack_trace      CLOB
);
CREATE INDEX ix_pos_error_log_occurred ON pos_error_log (occurred_at DESC);
