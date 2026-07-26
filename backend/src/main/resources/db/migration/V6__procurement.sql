-- =============================================================================
-- Built4U POS (single-business) — V6: procurement (PO + goods receipts).
-- =============================================================================
-- Site-scoped. Purchase orders and goods receipts are stored one row per line
-- (header fields denormalized onto every row, matching the FreePOS lineage);
-- the service layer aggregates a "header" view from the line rows.
--
-- pos_po_approver is a business-wide (NOT site-scoped) creator -> approver
-- routing map keyed by user_id. Absence of a row = "auto-approve on create".
-- pos_po_approval is the approval audit sidecar (approved_at / approved_by /
-- auto_approved) that can't live on the line table; one row once a PO reaches
-- APPROVED. VARCHAR2(n CHAR); TIMESTAMP dates.
-- =============================================================================


-- ── Purchase orders (one row per line) ───────────────────────────────────────
CREATE TABLE pos_purchase_order (
  site_id           NUMBER             NOT NULL,
  po_number         VARCHAR2(100 CHAR) NOT NULL,
  item_id           NUMBER             NOT NULL,
  item_desc         VARCHAR2(300 CHAR),
  quantity          NUMBER(38,2)       NOT NULL,
  uom               VARCHAR2(50 CHAR),
  unit_price        NUMBER(38,2)       NOT NULL,
  adjustment        VARCHAR2(50 CHAR),
  sub_total         NUMBER(38,2),
  grand_total       NUMBER(38,2),
  supplier          VARCHAR2(200 CHAR),
  delivery_date     VARCHAR2(100 CHAR),   -- stringified YYYY-MM-DD by convention
  remarks           VARCHAR2(200 CHAR),
  status            VARCHAR2(20 CHAR)  DEFAULT 'DRAFT' NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_purchase_order        PRIMARY KEY (site_id, po_number, item_id),
  CONSTRAINT ck_pos_purchase_order_status CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_RECEIVED','RECEIVED','CANCELLED'))
);
CREATE INDEX ix_pos_purchase_order_status   ON pos_purchase_order (site_id, status);
CREATE INDEX ix_pos_purchase_order_supplier ON pos_purchase_order (site_id, supplier);


-- ── Goods receipts (one row per received line) ───────────────────────────────
-- po_number is optional: a GR can be booked against a PO (typical) or direct.
CREATE TABLE pos_goods_receipt (
  site_id           NUMBER             NOT NULL,
  gr_number         VARCHAR2(100 CHAR) NOT NULL,
  item_id           NUMBER             NOT NULL,
  item_desc         VARCHAR2(300 CHAR),
  quantity          NUMBER(38,2)       NOT NULL,
  uom               VARCHAR2(50 CHAR),
  reference         VARCHAR2(200 CHAR),
  po_number         VARCHAR2(100 CHAR),
  remarks           VARCHAR2(200 CHAR),
  sup_price         NUMBER(38,2),
  sub_total         NUMBER(38,2),
  supplier          VARCHAR2(100 CHAR),
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_goods_receipt PRIMARY KEY (site_id, gr_number, item_id)
);
CREATE INDEX ix_pos_goods_receipt_po       ON pos_goods_receipt (site_id, po_number);
CREATE INDEX ix_pos_goods_receipt_creation ON pos_goods_receipt (site_id, creation_date);


-- ── PO approver routing (business-wide, keyed by creator user_id) ─────────────
CREATE TABLE pos_po_approver (
  user_id           NUMBER             NOT NULL,
  approver_user_id  NUMBER             NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_po_approver PRIMARY KEY (user_id)
);
CREATE INDEX ix_pos_po_approver_approver ON pos_po_approver (approver_user_id);


-- ── PO approval audit sidecar ────────────────────────────────────────────────
CREATE TABLE pos_po_approval (
  site_id        NUMBER             NOT NULL,
  po_number      VARCHAR2(100 CHAR) NOT NULL,
  approved_at    TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  approved_by    VARCHAR2(50 CHAR)  NOT NULL,
  auto_approved  VARCHAR2(1 CHAR)   DEFAULT 'N' NOT NULL,
  CONSTRAINT pk_pos_po_approval      PRIMARY KEY (site_id, po_number),
  CONSTRAINT ck_pos_po_approval_auto CHECK (auto_approved IN ('Y','N'))
);
