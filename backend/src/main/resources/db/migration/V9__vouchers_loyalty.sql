-- =============================================================================
-- Built4U POS (single-business) — V9: discount vouchers & loyalty points.
-- =============================================================================
-- Vouchers: a per-site catalog of discount codes redeemed at checkout; each
-- redemption is one pos_voucher_redemption row (the sale↔voucher link, reversed
-- on VOID). Loyalty: a per-site earn-rate config, a signed points ledger
-- (EARN on sale / REDEEM on reward / ADJUST on void), a reward catalog, and
-- reward redemptions. The live points balance stays on pos_customer.points.
--
-- Deferred (not in this migration): points expiry lots and points→voucher
-- conversion. VARCHAR2(n CHAR); TIMESTAMP audit dates; DATE calendar bounds.
-- =============================================================================


-- ── Vouchers ─────────────────────────────────────────────────────────────────
CREATE TABLE pos_voucher (
  id               NUMBER            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id          NUMBER            NOT NULL,
  code             VARCHAR2(40 CHAR) NOT NULL,
  description      VARCHAR2(150 CHAR),
  discount_type    VARCHAR2(10 CHAR) NOT NULL,
  discount_value   NUMBER(38,2)      NOT NULL,
  max_discount     NUMBER(38,2),
  min_spend        NUMBER(38,2),
  valid_from       DATE,
  valid_to         DATE,
  usage_limit      NUMBER,
  used_count       NUMBER            DEFAULT 0 NOT NULL,
  active           NUMBER(1)         DEFAULT 1 NOT NULL,
  creation_date    TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  created_by       VARCHAR2(50 CHAR),
  last_update_date TIMESTAMP,
  last_update_by   VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_voucher_type   CHECK (discount_type IN ('FIXED','PERCENT')),
  CONSTRAINT ck_pos_voucher_active CHECK (active IN (0,1)),
  CONSTRAINT ck_pos_voucher_value  CHECK (discount_value > 0)
);
CREATE UNIQUE INDEX uq_pos_voucher_code ON pos_voucher (site_id, UPPER(code));
CREATE INDEX ix_pos_voucher_site_active ON pos_voucher (site_id, active);

CREATE TABLE pos_voucher_redemption (
  id              NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id         NUMBER             NOT NULL,
  voucher_id      NUMBER             NOT NULL,
  sales_number    VARCHAR2(100 CHAR) NOT NULL,
  customer_id     NUMBER,
  discount_amount NUMBER(38,2)       NOT NULL,
  redeemed_at     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  reversed        NUMBER(1)          DEFAULT 0 NOT NULL,
  reversed_at     TIMESTAMP,
  creation_date   TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by      VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_voucher_redemption_rev CHECK (reversed IN (0,1))
);
CREATE INDEX ix_pos_voucher_redemption_sale ON pos_voucher_redemption (site_id, sales_number);


-- ── Loyalty config (per-site earn rate) ──────────────────────────────────────
CREATE TABLE pos_loyalty_config (
  site_id       NUMBER        NOT NULL,
  points_rate   NUMBER(5,2)   DEFAULT 5 NOT NULL,   -- percent of grand total earned
  redeem_value  NUMBER(38,2)  DEFAULT 1 NOT NULL,   -- informational ₱ value of a point
  updated_at    TIMESTAMP,
  updated_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_loyalty_config      PRIMARY KEY (site_id),
  CONSTRAINT ck_pos_loyalty_config_rate CHECK (points_rate >= 0 AND points_rate <= 100),
  CONSTRAINT ck_pos_loyalty_config_rdm  CHECK (redeem_value >= 0)
);


-- ── Loyalty points ledger (signed) ───────────────────────────────────────────
CREATE TABLE pos_loyalty_ledger (
  id             NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id        NUMBER             NOT NULL,
  customer_id    NUMBER             NOT NULL,
  entry_type     VARCHAR2(10 CHAR)  NOT NULL,
  points         NUMBER(38,2)       NOT NULL,
  sales_number   VARCHAR2(100 CHAR),
  note           VARCHAR2(255 CHAR),
  expires_at     DATE,
  expired        NUMBER(1)          DEFAULT 0 NOT NULL,
  creation_date  TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by     VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_loyalty_ledger_type
    CHECK (entry_type IN ('EARN','REDEEM','CONVERT','EXPIRE','ADJUST','OPENING')),
  CONSTRAINT ck_pos_loyalty_ledger_expired CHECK (expired IN (0,1))
);
CREATE INDEX ix_pos_loyalty_ledger_cust ON pos_loyalty_ledger (site_id, customer_id, id);
CREATE INDEX ix_pos_loyalty_ledger_sale ON pos_loyalty_ledger (site_id, sales_number);


-- ── Loyalty reward catalog ───────────────────────────────────────────────────
CREATE TABLE pos_loyalty_reward (
  id               NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id          NUMBER             NOT NULL,
  name             VARCHAR2(100 CHAR) NOT NULL,
  description      VARCHAR2(300 CHAR),
  points_cost      NUMBER(38,2)       NOT NULL,
  reward_type      VARCHAR2(10 CHAR)  NOT NULL,
  item_id          NUMBER,
  sort_order       NUMBER             DEFAULT 100 NOT NULL,
  active           NUMBER(1)          DEFAULT 1   NOT NULL,
  creation_date    TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by       VARCHAR2(50 CHAR),
  last_update_date TIMESTAMP,
  last_update_by   VARCHAR2(50 CHAR),
  CONSTRAINT ck_pos_loyalty_reward_type   CHECK (reward_type IN ('ITEM','FREETEXT')),
  CONSTRAINT ck_pos_loyalty_reward_active CHECK (active IN (0,1)),
  CONSTRAINT ck_pos_loyalty_reward_cost   CHECK (points_cost > 0),
  CONSTRAINT ck_pos_loyalty_reward_item   CHECK (
    (reward_type = 'ITEM'     AND item_id IS NOT NULL)
    OR (reward_type = 'FREETEXT' AND item_id IS NULL))
);
CREATE INDEX ix_pos_loyalty_reward_site ON pos_loyalty_reward (site_id, active);


-- ── Loyalty reward redemptions ───────────────────────────────────────────────
CREATE TABLE pos_loyalty_redemption (
  id             NUMBER             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  site_id        NUMBER             NOT NULL,
  reward_id      NUMBER             NOT NULL,
  customer_id    NUMBER             NOT NULL,
  reward_name    VARCHAR2(100 CHAR) NOT NULL,
  reward_type    VARCHAR2(10 CHAR)  NOT NULL,
  item_id        NUMBER,
  points_spent   NUMBER(38,2)       NOT NULL,
  note           VARCHAR2(300 CHAR),
  redeemed_at    TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  creation_date  TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by     VARCHAR2(50 CHAR)
);
CREATE INDEX ix_pos_loyalty_redemption_cust ON pos_loyalty_redemption (site_id, customer_id, id);
