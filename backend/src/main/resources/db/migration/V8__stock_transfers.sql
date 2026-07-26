-- =============================================================================
-- Built4U POS (single-business) — V8: cross-site stock transfers.
-- =============================================================================
-- The first module that spans two sites. A transfer ships from a source site
-- (decrementing its stock, status IN_TRANSIT) and is received at the
-- destination (incrementing its stock, auto-creating the item there if its
-- code is new), or cancelled at the source while still IN_TRANSIT (restoring
-- stock). Line rows snapshot code/name/uom/cost at ship time.
--
-- pos_stock_transfer_policy is an admin allow-list of permitted
-- (source_site_id -> dest_site_id) pairs: empty table = OPEN (any site may ship
-- to any other), >= 1 row = ENFORCED (only listed pairs). VARCHAR2(n CHAR);
-- TIMESTAMP dates.
-- =============================================================================


-- ── Transfer header ──────────────────────────────────────────────────────────
CREATE TABLE pos_stock_transfer (
  id                NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source_site_id    NUMBER             NOT NULL,
  dest_site_id      NUMBER             NOT NULL,
  transfer_number   VARCHAR2(100 CHAR) NOT NULL,
  status            VARCHAR2(15 CHAR)  DEFAULT 'IN_TRANSIT' NOT NULL,
  remarks           VARCHAR2(300 CHAR),
  shipped_at        TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  sent_by           VARCHAR2(50 CHAR)  NOT NULL,
  received_at       TIMESTAMP,
  received_by       VARCHAR2(50 CHAR),
  cancelled_at      TIMESTAMP,
  cancelled_by      VARCHAR2(50 CHAR),
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_stock_transfer_status CHECK (status IN ('IN_TRANSIT','RECEIVED','CANCELLED')),
  CONSTRAINT ck_pos_stock_transfer_sites  CHECK (source_site_id <> dest_site_id)
);
CREATE UNIQUE INDEX uq_pos_stock_transfer_num  ON pos_stock_transfer (source_site_id, transfer_number);
CREATE INDEX ix_pos_stock_transfer_src_status  ON pos_stock_transfer (source_site_id, status);
CREATE INDEX ix_pos_stock_transfer_dest_status ON pos_stock_transfer (dest_site_id, status);
CREATE INDEX ix_pos_stock_transfer_shipped     ON pos_stock_transfer (shipped_at DESC);


-- ── Transfer line (snapshot at ship time) ────────────────────────────────────
CREATE TABLE pos_stock_transfer_item (
  id              NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  transfer_id     NUMBER             NOT NULL,
  source_item_id  NUMBER             NOT NULL,
  item_code       VARCHAR2(50 CHAR)  NOT NULL,
  item_name       VARCHAR2(100 CHAR),
  uom             VARCHAR2(50 CHAR),
  quantity        NUMBER(38,2)       NOT NULL,
  unit_cost       NUMBER(38,2)       DEFAULT 0 NOT NULL,
  CONSTRAINT ck_pos_stock_transfer_item_qty  CHECK (quantity > 0),
  CONSTRAINT ck_pos_stock_transfer_item_cost CHECK (unit_cost >= 0)
);
CREATE INDEX ix_pos_stock_transfer_item_xfer ON pos_stock_transfer_item (transfer_id);


-- ── Allow-list policy (empty = OPEN, any row = ENFORCED) ──────────────────────
CREATE TABLE pos_stock_transfer_policy (
  id               NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  source_site_id   NUMBER            NOT NULL,
  dest_site_id     NUMBER            NOT NULL,
  creation_date    TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by       VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_stock_transfer_policy_diff CHECK (source_site_id <> dest_site_id)
);
CREATE UNIQUE INDEX uq_pos_stock_transfer_policy_pair ON pos_stock_transfer_policy (source_site_id, dest_site_id);
CREATE INDEX ix_pos_stock_transfer_policy_src ON pos_stock_transfer_policy (source_site_id);
CREATE INDEX ix_pos_stock_transfer_policy_dst ON pos_stock_transfer_policy (dest_site_id);
