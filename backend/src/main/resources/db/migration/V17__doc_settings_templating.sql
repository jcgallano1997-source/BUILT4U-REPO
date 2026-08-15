-- =============================================================================
-- Built4U POS (single-business) — V17: rich document templating.
-- =============================================================================
-- Extends pos_doc_settings with the FreePOS templating knobs: a per-site logo
-- (shared by report PDFs and receipts), report-PDF page setup (paper/orientation/
-- margins/font scale/zebra + footer toggles), and receipt customization (logo,
-- header note, field visibility toggles, and a physical format: 80mm thermal vs
-- US Letter bond for dot-matrix). Booleans use the Y/N convention (YesNoConverter).
-- =============================================================================

ALTER TABLE pos_doc_settings ADD (
  logo_image            BLOB,
  logo_mime             VARCHAR2(40 CHAR),
  logo_position         VARCHAR2(10 CHAR)  DEFAULT 'LEFT'         NOT NULL,
  show_logo_pdf         VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  paper_size            VARCHAR2(10 CHAR)  DEFAULT 'A4'           NOT NULL,
  orientation           VARCHAR2(10 CHAR)  DEFAULT 'LANDSCAPE'    NOT NULL,
  margin_preset         VARCHAR2(10 CHAR)  DEFAULT 'NORMAL'       NOT NULL,
  font_scale            VARCHAR2(10 CHAR)  DEFAULT 'NORMAL'       NOT NULL,
  zebra_striping        VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  show_page_numbers     VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  show_timestamp        VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  show_printed_by       VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  show_logo_receipt     VARCHAR2(1 CHAR)   DEFAULT 'N'            NOT NULL,
  receipt_header_note   VARCHAR2(255 CHAR),
  receipt_show_cashier  VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  receipt_show_customer VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  receipt_show_voucher  VARCHAR2(1 CHAR)   DEFAULT 'Y'            NOT NULL,
  receipt_format        VARCHAR2(20 CHAR)  DEFAULT 'THERMAL_80MM' NOT NULL
);
