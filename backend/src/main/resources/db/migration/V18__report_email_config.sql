-- =============================================================================
-- Built4U POS (single-business) — V18: per-report email delivery config.
-- =============================================================================
-- Saved recipient / subject / body for emailing a report (one row per report
-- code per site). Blank subject/body fall back to generated defaults; blank
-- recipient falls back to app.mail.default-recipient. Delivery itself stays
-- inert until app.mail.resend-api-key is configured. TIMESTAMP audit dates.
-- =============================================================================

CREATE TABLE pos_report_email_config (
  site_id         NUMBER              NOT NULL,
  report_code     VARCHAR2(60 CHAR)   NOT NULL,
  label           VARCHAR2(100 CHAR),
  recipient_email VARCHAR2(255 CHAR),
  subject         VARCHAR2(255 CHAR),
  body            VARCHAR2(2000 CHAR),
  updated_at      TIMESTAMP,
  updated_by      VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_report_email_config PRIMARY KEY (site_id, report_code)
);
