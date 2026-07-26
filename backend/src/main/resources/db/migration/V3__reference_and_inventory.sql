-- =============================================================================
-- Built4U POS (single-business) — V3: reference data + inventory.
-- =============================================================================
-- Site-scoped, composite-key tables (site_id + surrogate id from a sequence).
-- Order matters: category + location before inventory (FK targets).
-- VARCHAR2(n CHAR) throughout; audit dates are TIMESTAMP (LocalDateTime entities).
-- =============================================================================


-- ── Categories ───────────────────────────────────────────────────────────────
CREATE TABLE pos_category (
  site_id           NUMBER             NOT NULL,
  cat_id            NUMBER             NOT NULL,
  category_name     VARCHAR2(100 CHAR) NOT NULL,
  active            VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_category        PRIMARY KEY (site_id, cat_id),
  CONSTRAINT uq_pos_category_name   UNIQUE (site_id, category_name),
  CONSTRAINT ck_pos_category_active CHECK (active IN ('Y','N'))
);
CREATE INDEX ix_pos_category_site_active ON pos_category (site_id, active);
CREATE SEQUENCE pos_category_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── Locations ────────────────────────────────────────────────────────────────
CREATE TABLE pos_location (
  site_id           NUMBER             NOT NULL,
  loc_id            NUMBER             NOT NULL,
  location          VARCHAR2(100 CHAR) NOT NULL,
  capacity          NUMBER(38,2),
  active            VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_location        PRIMARY KEY (site_id, loc_id),
  CONSTRAINT uq_pos_location_name   UNIQUE (site_id, location),
  CONSTRAINT ck_pos_location_active CHECK (active IN ('Y','N'))
);
CREATE INDEX ix_pos_location_site_active ON pos_location (site_id, active);
CREATE SEQUENCE pos_location_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── Units of measure (natural string key, no sequence) ───────────────────────
CREATE TABLE pos_uom (
  site_id  NUMBER            NOT NULL,
  uom      VARCHAR2(50 CHAR) NOT NULL,
  active   VARCHAR2(1 CHAR)  DEFAULT 'Y' NOT NULL,
  CONSTRAINT pk_pos_uom        PRIMARY KEY (site_id, uom),
  CONSTRAINT ck_pos_uom_active CHECK (active IN ('Y','N'))
);
CREATE INDEX ix_pos_uom_site_active ON pos_uom (site_id, active);


-- ── Inventory (item master) ──────────────────────────────────────────────────
CREATE TABLE pos_inventory (
  site_id           NUMBER             NOT NULL,
  item_id           NUMBER             NOT NULL,
  barcode_id        NUMBER,
  cat_id            NUMBER             NOT NULL,
  loc_id            NUMBER             NOT NULL,
  unit_cost         NUMBER(38,2)       NOT NULL,   -- SELLING price per unit
  quantity          NUMBER(38,2)       DEFAULT 0   NOT NULL,
  item_code         VARCHAR2(50 CHAR)  NOT NULL,
  item_name         VARCHAR2(100 CHAR) NOT NULL,
  item_desc         VARCHAR2(300 CHAR),
  uom               VARCHAR2(50 CHAR)  NOT NULL,
  warning           NUMBER(38,2),
  critical          NUMBER(38,2),
  sup_price         NUMBER(38,2),                  -- supplier (cost) price
  active            VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_inventory        PRIMARY KEY (site_id, item_id),
  CONSTRAINT uq_pos_inventory_code   UNIQUE (site_id, item_code),
  CONSTRAINT ck_pos_inventory_active CHECK (active IN ('Y','N')),
  CONSTRAINT fk_pos_inventory_cat FOREIGN KEY (site_id, cat_id) REFERENCES pos_category (site_id, cat_id),
  CONSTRAINT fk_pos_inventory_loc FOREIGN KEY (site_id, loc_id) REFERENCES pos_location (site_id, loc_id)
);
CREATE INDEX ix_pos_inventory_site_active ON pos_inventory (site_id, active);
CREATE INDEX ix_pos_inventory_cat         ON pos_inventory (site_id, cat_id);
CREATE INDEX ix_pos_inventory_loc         ON pos_inventory (site_id, loc_id);
CREATE UNIQUE INDEX uq_pos_inventory_barcode ON pos_inventory (
  CASE WHEN barcode_id IS NOT NULL THEN site_id END,
  CASE WHEN barcode_id IS NOT NULL THEN barcode_id END
);
CREATE SEQUENCE pos_inventory_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ── Universal stock-movement log ─────────────────────────────────────────────
CREATE TABLE pos_transaction_log (
  log_id            NUMBER             GENERATED ALWAYS AS IDENTITY,
  site_id           NUMBER             NOT NULL,
  item_id           NUMBER             NOT NULL,
  cat_id            NUMBER,
  attr_id1          NUMBER,
  attr_id2          NUMBER,
  transaction_type  VARCHAR2(200 CHAR) NOT NULL,
  attribute1        VARCHAR2(300 CHAR),
  attribute2        VARCHAR2(300 CHAR),
  attribute3        VARCHAR2(300 CHAR),
  attribute4        VARCHAR2(300 CHAR),
  reason            VARCHAR2(300 CHAR),
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_transaction_log PRIMARY KEY (log_id)
);
CREATE INDEX ix_pos_txn_log_site_item ON pos_transaction_log (site_id, item_id, creation_date);
CREATE INDEX ix_pos_txn_log_type      ON pos_transaction_log (site_id, transaction_type, creation_date);
