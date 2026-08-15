-- =============================================================================
-- Built4U POS (single-business) — V20: mid-shift cash movements + denomination count.
-- =============================================================================
-- Cash in/out recorded during an open shift (pay-ins, paid-outs, petty cash) so
-- expected cash = opening float + cash sales − cash refunds + cash in − cash out.
-- Without this, a paid-out would look like a shortage. Denomination counts at close
-- derive counted cash from the physical bill/coin tally.
-- =============================================================================

CREATE TABLE pos_cash_movement (
  movement_id   NUMBER             NOT NULL,
  site_id       NUMBER             NOT NULL,
  shift_number  VARCHAR2(100 CHAR) NOT NULL,
  direction     VARCHAR2(3 CHAR)   NOT NULL,   -- IN | OUT
  amount        NUMBER(38,2)       NOT NULL,
  reason        VARCHAR2(255 CHAR),
  creation_date TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_cash_movement     PRIMARY KEY (movement_id),
  CONSTRAINT ck_pos_cash_movement_dir CHECK (direction IN ('IN','OUT'))
);
CREATE SEQUENCE pos_cash_movement_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE INDEX ix_pos_cash_movement_shift ON pos_cash_movement (site_id, shift_number);

-- Bill/coin tally recorded at close (face value → quantity). counted cash = Σ(denom × qty).
CREATE TABLE pos_shift_denomination (
  site_id      NUMBER             NOT NULL,
  shift_number VARCHAR2(100 CHAR) NOT NULL,
  denom        NUMBER(38,2)       NOT NULL,
  qty          NUMBER(10)         NOT NULL,
  CONSTRAINT pk_pos_shift_denomination PRIMARY KEY (site_id, shift_number, denom)
);

-- Snapshot the cash-in/out totals onto the shift at close (for the report + closed view).
ALTER TABLE pos_shift ADD (
  cash_in_total  NUMBER(38,2) DEFAULT 0 NOT NULL,
  cash_out_total NUMBER(38,2) DEFAULT 0 NOT NULL
);
