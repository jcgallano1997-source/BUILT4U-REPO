-- =============================================================================
-- Built4U POS (single-business) — V4: shifts + sales (the POS core).
-- =============================================================================
-- Site-scoped. mode_of_payment is a free string in this phase (payment-mode
-- catalog + surcharges/AR/loyalty/vouchers arrive in later phases; those columns
-- exist but stay at their defaults). VARCHAR2(n CHAR); TIMESTAMP audit dates.
-- =============================================================================


-- ── Cashier shifts (open/close reconciliation) ───────────────────────────────
CREATE TABLE pos_shift (
  site_id               NUMBER            NOT NULL,
  shift_number          VARCHAR2(100 CHAR) NOT NULL,
  cashier               VARCHAR2(50 CHAR)  NOT NULL,
  status                VARCHAR2(20 CHAR)  DEFAULT 'OPEN' NOT NULL,
  opening_float         NUMBER(38,2)      NOT NULL,
  opened_at             TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  closed_at             TIMESTAMP,
  closed_by             VARCHAR2(50 CHAR),
  counted_cash          NUMBER(38,2),
  expected_cash         NUMBER(38,2),
  cash_variance         NUMBER(38,2),
  cash_sales_total      NUMBER(38,2)      DEFAULT 0 NOT NULL,
  cash_refunds_total    NUMBER(38,2)      DEFAULT 0 NOT NULL,
  noncash_gcash_total   NUMBER(38,2)      DEFAULT 0 NOT NULL,
  noncash_paymaya_total NUMBER(38,2)      DEFAULT 0 NOT NULL,
  noncash_bank_total    NUMBER(38,2)      DEFAULT 0 NOT NULL,
  noncash_cheque_total  NUMBER(38,2)      DEFAULT 0 NOT NULL,
  noncash_charge_total  NUMBER(38,2)      DEFAULT 0 NOT NULL,
  sale_count            NUMBER(10)        DEFAULT 0 NOT NULL,
  close_note            VARCHAR2(500 CHAR),
  creation_date         TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by            VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_shift        PRIMARY KEY (site_id, shift_number),
  CONSTRAINT ck_pos_shift_status CHECK (status IN ('OPEN','CLOSED'))
);
CREATE INDEX ix_pos_shift_recent  ON pos_shift (site_id, opened_at DESC);
CREATE INDEX ix_pos_shift_cashier ON pos_shift (site_id, cashier, status);
-- At most one OPEN shift per (site, cashier); CLOSED rows fall out via NULL keys.
CREATE UNIQUE INDEX uq_pos_shift_one_open ON pos_shift (
  CASE WHEN status = 'OPEN' THEN site_id ELSE NULL END,
  CASE WHEN status = 'OPEN' THEN cashier ELSE NULL END
);


-- ── Sale header ──────────────────────────────────────────────────────────────
CREATE TABLE pos_sale_header (
  site_id           NUMBER             NOT NULL,
  sales_number      VARCHAR2(100 CHAR) NOT NULL,
  total             NUMBER(38,2)       NOT NULL,
  discount_all      NUMBER(38,2)       DEFAULT 0 NOT NULL,
  total_disc_item   NUMBER(38,2)       DEFAULT 0 NOT NULL,
  grand_total       NUMBER(38,2)       NOT NULL,
  payment           NUMBER(38,2)       NOT NULL,
  change_due        NUMBER(38,2)       NOT NULL,
  mode_of_payment   VARCHAR2(50 CHAR)  NOT NULL,
  customer_id       NUMBER,
  voucher           VARCHAR2(100 CHAR),
  reference         VARCHAR2(200 CHAR),
  for_delivery      VARCHAR2(1 CHAR)   DEFAULT 'N' NOT NULL,
  status            VARCHAR2(20 CHAR)  DEFAULT 'COMPLETED' NOT NULL,
  reprint_count     NUMBER(10)         DEFAULT 0 NOT NULL,
  points_redeemed   NUMBER(38,2)       DEFAULT 0 NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_sale_header        PRIMARY KEY (site_id, sales_number),
  CONSTRAINT ck_pos_sale_header_fordel CHECK (for_delivery IN ('Y','N')),
  CONSTRAINT ck_pos_sale_header_status CHECK (status IN ('COMPLETED','VOIDED','REFUNDED'))
);
CREATE INDEX ix_pos_sale_header_recent   ON pos_sale_header (site_id, creation_date DESC);
CREATE INDEX ix_pos_sale_header_customer ON pos_sale_header (site_id, customer_id);


-- ── Sale line items ──────────────────────────────────────────────────────────
CREATE TABLE pos_sale_item (
  site_id        NUMBER             NOT NULL,
  sales_number   VARCHAR2(100 CHAR) NOT NULL,
  item_id        NUMBER             NOT NULL,
  item_desc      VARCHAR2(300 CHAR),
  quantity       NUMBER(38,2)       NOT NULL,
  uom            VARCHAR2(50 CHAR),
  unit_cost      NUMBER(38,2)       NOT NULL,   -- selling price snapshot
  adjustment     NUMBER(38,2)       DEFAULT 0 NOT NULL,
  sub_total      NUMBER(38,2)       NOT NULL,
  creation_date  TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by     VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_sale_item PRIMARY KEY (site_id, sales_number, item_id)
);
CREATE INDEX ix_pos_sale_item_item ON pos_sale_item (site_id, item_id);


-- ── Return (refund) line items ───────────────────────────────────────────────
CREATE TABLE pos_return_item (
  site_id         NUMBER             NOT NULL,
  return_number   VARCHAR2(100 CHAR) NOT NULL,
  item_id         NUMBER             NOT NULL,
  sales_number    VARCHAR2(100 CHAR) NOT NULL,
  item_desc       VARCHAR2(300 CHAR),
  quantity        NUMBER(38,2)       NOT NULL,
  uom             VARCHAR2(50 CHAR),
  unit_cost       NUMBER(38,2)       NOT NULL,
  sub_total       NUMBER(38,2)       NOT NULL,
  reason          VARCHAR2(300 CHAR),
  creation_date   TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by      VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_return_item PRIMARY KEY (site_id, return_number, item_id)
);
CREATE INDEX ix_pos_return_item_sale   ON pos_return_item (site_id, sales_number);
CREATE INDEX ix_pos_return_item_recent ON pos_return_item (site_id, creation_date DESC);
