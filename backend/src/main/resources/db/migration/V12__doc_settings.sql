-- =============================================================================
-- Built4U POS (single-business) — V12: document / branding settings.
-- =============================================================================
-- Per-site branding consumed by the report-PDF letterhead and the sale-receipt
-- PDF: business identity (name/address/contact/TIN), a report footer note, an
-- accent colour, and receipt title/footer text. One row per site; no row ⇒ the
-- service falls through to hard-coded defaults. (Logo images are intentionally
-- out of scope for now — text branding only.) TIMESTAMP audit dates.
-- =============================================================================

CREATE TABLE pos_doc_settings (
  site_id         NUMBER             NOT NULL,
  business_name   VARCHAR2(150 CHAR),
  address_line    VARCHAR2(255 CHAR),
  contact_line    VARCHAR2(150 CHAR),
  tin             VARCHAR2(40 CHAR),
  footer_note     VARCHAR2(255 CHAR),
  accent_color    VARCHAR2(7 CHAR)   DEFAULT '#1D4ED8' NOT NULL,
  receipt_title   VARCHAR2(60 CHAR)  DEFAULT 'SALES RECEIPT' NOT NULL,
  receipt_footer  VARCHAR2(255 CHAR) DEFAULT 'Thank you!' NOT NULL,
  updated_at      TIMESTAMP,
  updated_by      VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_doc_settings PRIMARY KEY (site_id)
);
