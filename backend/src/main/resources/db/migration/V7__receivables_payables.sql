-- =============================================================================
-- Built4U POS (single-business) — V7: accounts receivable & payable.
-- =============================================================================
-- AR: a credit sale (checkout on an accounts-receivable payment mode) creates a
-- pos_receivable for the unpaid balance; collections are appended to
-- pos_receivable_payment. Per-customer credit limits gate new credit.
--
-- AP: a goods receipt from an AP-enabled supplier auto-creates a PURCHASE
-- payable; expenses are entered manually (EXPENSE); disbursements append to
-- pos_payable_payment. Supplier AP config lives on pos_supplier (ap_enabled +
-- payable_days). VARCHAR2(n CHAR); TIMESTAMP audit dates; DATE due dates.
-- =============================================================================


-- ── Customer credit limit (0 = no limit) ─────────────────────────────────────
ALTER TABLE pos_customer ADD (
  credit_limit NUMBER(38,2) DEFAULT 0 NOT NULL
);
ALTER TABLE pos_customer ADD CONSTRAINT ck_pos_customer_credit CHECK (credit_limit >= 0);


-- ── Supplier AP config ───────────────────────────────────────────────────────
ALTER TABLE pos_supplier ADD (
  ap_enabled   NUMBER(1)  DEFAULT 0  NOT NULL,
  payable_days NUMBER(10) DEFAULT 30 NOT NULL
);
ALTER TABLE pos_supplier ADD CONSTRAINT ck_pos_supplier_apen  CHECK (ap_enabled IN (0,1));
ALTER TABLE pos_supplier ADD CONSTRAINT ck_pos_supplier_apday CHECK (payable_days >= 0);


-- ── Receivables ──────────────────────────────────────────────────────────────
CREATE TABLE pos_receivable (
  id                NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id           NUMBER            NOT NULL,
  sales_number      VARCHAR2(100 CHAR) NOT NULL,
  customer_id       NUMBER            NOT NULL,
  mode_code         VARCHAR2(30 CHAR) NOT NULL,
  original_amount   NUMBER(38,2)      NOT NULL,
  amount_paid       NUMBER(38,2)      DEFAULT 0 NOT NULL,
  balance           NUMBER(38,2)      NOT NULL,
  due_date          DATE              NOT NULL,
  status            VARCHAR2(10 CHAR) DEFAULT 'OPEN' NOT NULL,
  closed_at         TIMESTAMP,
  creation_date     TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_receivable_status CHECK (status IN ('OPEN','PARTIAL','PAID','CANCELLED'))
);
CREATE INDEX ix_pos_receivable_site_status ON pos_receivable (site_id, status);
CREATE INDEX ix_pos_receivable_customer    ON pos_receivable (site_id, customer_id);
CREATE INDEX ix_pos_receivable_sales       ON pos_receivable (site_id, sales_number);

CREATE TABLE pos_receivable_payment (
  id             NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id        NUMBER            NOT NULL,
  receivable_id  NUMBER            NOT NULL,
  amount         NUMBER(38,2)      NOT NULL,
  note           VARCHAR2(300 CHAR),
  paid_at        TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  creation_date  TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by     VARCHAR2(50 CHAR)
);
CREATE INDEX ix_pos_receivable_payment ON pos_receivable_payment (site_id, receivable_id);


-- ── Payables ─────────────────────────────────────────────────────────────────
CREATE TABLE pos_payable (
  id                NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id           NUMBER            NOT NULL,
  source            VARCHAR2(10 CHAR) DEFAULT 'PURCHASE' NOT NULL,
  category          VARCHAR2(60 CHAR),
  po_number         VARCHAR2(100 CHAR),
  gr_number         VARCHAR2(100 CHAR),
  supplier_id       NUMBER,
  payee_name        VARCHAR2(150 CHAR) NOT NULL,
  description       VARCHAR2(300 CHAR),
  original_amount   NUMBER(38,2)      NOT NULL,
  amount_paid       NUMBER(38,2)      DEFAULT 0 NOT NULL,
  balance           NUMBER(38,2)      NOT NULL,
  due_date          DATE              NOT NULL,
  status            VARCHAR2(10 CHAR) DEFAULT 'OPEN' NOT NULL,
  closed_at         TIMESTAMP,
  creation_date     TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_payable_source CHECK (source IN ('PURCHASE','EXPENSE')),
  CONSTRAINT ck_pos_payable_status CHECK (status IN ('OPEN','PARTIAL','PAID','CANCELLED'))
);
CREATE INDEX ix_pos_payable_site_status ON pos_payable (site_id, status);
CREATE INDEX ix_pos_payable_supplier    ON pos_payable (site_id, supplier_id);
CREATE INDEX ix_pos_payable_gr          ON pos_payable (site_id, gr_number);

CREATE TABLE pos_payable_payment (
  id             NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id        NUMBER            NOT NULL,
  payable_id     NUMBER            NOT NULL,
  amount         NUMBER(38,2)      NOT NULL,
  note           VARCHAR2(300 CHAR),
  paid_at        TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  creation_date  TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by     VARCHAR2(50 CHAR)
);
CREATE INDEX ix_pos_payable_payment ON pos_payable_payment (site_id, payable_id);
