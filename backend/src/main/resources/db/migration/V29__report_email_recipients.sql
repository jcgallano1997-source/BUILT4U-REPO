-- =============================================================================
-- Built4U POS — V29: report email recipients (multi-recipient).
--
-- pos_report_email_config held ONE recipient_email per report, so a report could
-- only ever reach one mailbox. This table adds the real recipient list: the
-- users a report is emailed to, resolved to their pos_user.email at send time.
--
-- Picking users rather than free text means a deactivated staff member stops
-- receiving reports on their own, and a changed address only has to be updated
-- in one place. recipient_email stays on the config as an extra "external"
-- address (an accountant, say) for someone with no user account.
--
-- VARCHAR2(n CHAR); TIMESTAMP dates.
-- =============================================================================

CREATE TABLE pos_report_email_recipient (
  site_id           NUMBER             NOT NULL,
  report_code       VARCHAR2(60 CHAR)  NOT NULL,
  user_id           NUMBER(19)         NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_report_email_recipient PRIMARY KEY (site_id, report_code, user_id)
);

CREATE INDEX ix_pos_report_email_recip_user ON pos_report_email_recipient (user_id);
