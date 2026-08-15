-- =============================================================================
-- Built4U POS — V15: held (parked) sales for POS.
-- =============================================================================
-- Lets a cashier SAVE an in-progress cart and RECALL it later from the register.
-- The cart itself is stored as JSON (opaque to the backend — the POS owns its
-- shape); the header columns are just for the recall list. Site-scoped: any
-- cashier at the same site can recall a held sale (useful across shift handovers).
-- Ephemeral scratch data (not a financial transaction) — no stock/shift impact.
-- All objects live in the BUILT4U schema. FREEPOS is never touched.
-- =============================================================================

CREATE TABLE pos_held_sale (
  held_id           NUMBER             NOT NULL,
  site_id           NUMBER             NOT NULL,
  label             VARCHAR2(100 CHAR),
  customer_id       NUMBER,
  customer_name     VARCHAR2(100 CHAR),
  item_count        NUMBER             DEFAULT 0 NOT NULL,
  total_amount      NUMBER(38,2)       DEFAULT 0 NOT NULL,
  cart_json         CLOB               NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  last_update_date  TIMESTAMP,
  last_update_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_held_sale PRIMARY KEY (held_id)
);

CREATE INDEX ix_pos_held_sale_site ON pos_held_sale (site_id, held_id);
CREATE SEQUENCE pos_held_sale_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
