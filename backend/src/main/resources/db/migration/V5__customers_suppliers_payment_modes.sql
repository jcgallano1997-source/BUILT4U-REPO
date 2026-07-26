-- =============================================================================
-- Built4U POS (single-business) — V5: customers, suppliers, payment modes.
-- =============================================================================
-- Site-scoped. Payment modes are per-site (no entity/site_id=0 default layer);
-- a default set is seeded for the MAIN site. VARCHAR2(n CHAR); TIMESTAMP dates.
-- =============================================================================


-- ── Customers ────────────────────────────────────────────────────────────────
CREATE TABLE pos_customer (
  site_id           NUMBER             NOT NULL,
  customer_id       NUMBER             NOT NULL,
  customer_name     VARCHAR2(100 CHAR) NOT NULL,
  contact           VARCHAR2(50 CHAR),
  address           VARCHAR2(200 CHAR),
  email             VARCHAR2(120 CHAR),
  points            NUMBER(38,2)       DEFAULT 0,
  active            VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_customer        PRIMARY KEY (site_id, customer_id),
  CONSTRAINT ck_pos_customer_active CHECK (active IN ('Y','N'))
);
CREATE INDEX ix_pos_customer_site_active ON pos_customer (site_id, active);
CREATE INDEX ix_pos_customer_contact     ON pos_customer (site_id, contact);
CREATE SEQUENCE pos_customer_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── Suppliers ────────────────────────────────────────────────────────────────
CREATE TABLE pos_supplier (
  site_id           NUMBER             NOT NULL,
  supplier_id       NUMBER             NOT NULL,
  supplier_code     VARCHAR2(20 CHAR)  NOT NULL,
  supplier_name     VARCHAR2(100 CHAR) NOT NULL,
  supplier_address  VARCHAR2(250 CHAR),
  agent_name        VARCHAR2(100 CHAR),
  contact           VARCHAR2(100 CHAR),
  active            VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_supplier        PRIMARY KEY (site_id, supplier_id),
  CONSTRAINT uq_pos_supplier_code   UNIQUE (site_id, supplier_code),
  CONSTRAINT ck_pos_supplier_active CHECK (active IN ('Y','N'))
);
CREATE INDEX ix_pos_supplier_site_active ON pos_supplier (site_id, active);
CREATE SEQUENCE pos_supplier_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── Payment modes (per-site catalog) ─────────────────────────────────────────
CREATE TABLE pos_payment_mode (
  id                  NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id             NUMBER            NOT NULL,
  code                VARCHAR2(30 CHAR) NOT NULL,
  label               VARCHAR2(60 CHAR) NOT NULL,
  surcharge_type      VARCHAR2(10 CHAR) DEFAULT 'NONE' NOT NULL,
  surcharge_value     NUMBER(38,2)      DEFAULT 0      NOT NULL,
  is_cash             NUMBER(1)         DEFAULT 0      NOT NULL,
  allows_partial      NUMBER(1)         DEFAULT 0      NOT NULL,
  customer_required   NUMBER(1)         DEFAULT 0      NOT NULL,
  accounts_receivable NUMBER(1)         DEFAULT 0      NOT NULL,
  ar_due_days         NUMBER(10)        DEFAULT 30     NOT NULL,
  sort_order          NUMBER(10)        DEFAULT 100    NOT NULL,
  active              NUMBER(1)         DEFAULT 1      NOT NULL,
  creation_date       TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by          VARCHAR2(50 CHAR),
  last_update_date    TIMESTAMP,
  last_update_by      VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_payment_mode_sctype  CHECK (surcharge_type IN ('NONE','PERCENT','FIXED')),
  CONSTRAINT ck_pos_payment_mode_iscash  CHECK (is_cash IN (0,1)),
  CONSTRAINT ck_pos_payment_mode_partial CHECK (allows_partial IN (0,1)),
  CONSTRAINT ck_pos_payment_mode_custreq CHECK (customer_required IN (0,1)),
  CONSTRAINT ck_pos_payment_mode_ar      CHECK (accounts_receivable IN (0,1)),
  CONSTRAINT ck_pos_payment_mode_active  CHECK (active IN (0,1)),
  CONSTRAINT ck_pos_payment_mode_ardue   CHECK (ar_due_days >= 0),
  CONSTRAINT ck_pos_payment_mode_scval   CHECK (surcharge_value >= 0)
);
CREATE UNIQUE INDEX uq_pos_payment_mode_code ON pos_payment_mode (site_id, UPPER(code));
CREATE INDEX ix_pos_payment_mode_site ON pos_payment_mode (site_id, active);

-- Seed a default catalog for the MAIN site.
INSERT INTO pos_payment_mode (site_id, code, label, is_cash, sort_order)
SELECT s.id, 'CASH', 'Cash', 1, 10 FROM pos_site s WHERE s.code = 'MAIN';
INSERT INTO pos_payment_mode (site_id, code, label, is_cash, sort_order)
SELECT s.id, 'GCASH', 'GCash', 0, 20 FROM pos_site s WHERE s.code = 'MAIN';
INSERT INTO pos_payment_mode (site_id, code, label, is_cash, sort_order)
SELECT s.id, 'PAYMAYA', 'Maya', 0, 30 FROM pos_site s WHERE s.code = 'MAIN';
INSERT INTO pos_payment_mode (site_id, code, label, is_cash, sort_order)
SELECT s.id, 'CARD', 'Card', 0, 40 FROM pos_site s WHERE s.code = 'MAIN';
