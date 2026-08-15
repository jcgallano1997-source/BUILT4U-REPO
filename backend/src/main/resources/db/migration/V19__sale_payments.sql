-- =============================================================================
-- Built4U POS (single-business) — V19: split / multiple tender.
-- =============================================================================
-- One row per tender applied to a sale. The APPLIED amount is what counts toward
-- that mode's total; the sum of a sale's applied amounts always equals the sale's
-- grand_total, so "sales by payment mode" and shift cash reconciliation aggregate
-- cleanly from here (cash change is excluded — tendered minus change = applied).
-- Every sale gets at least one row (legacy single-mode sales included), so reports
-- can read tenders uniformly.
-- =============================================================================

-- NOTE: the tender-method column is named pay_mode — MODE is an Oracle reserved word.
CREATE TABLE pos_sale_payment (
  site_id       NUMBER             NOT NULL,
  sales_number  VARCHAR2(100 CHAR) NOT NULL,
  seq           NUMBER             NOT NULL,
  pay_mode      VARCHAR2(50 CHAR)  NOT NULL,
  amount        NUMBER(38,2)       NOT NULL,           -- applied (sums to grand_total)
  tendered      NUMBER(38,2)       DEFAULT 0 NOT NULL,  -- amount handed over (cash may exceed applied)
  change_due    NUMBER(38,2)       DEFAULT 0 NOT NULL,
  reference     VARCHAR2(200 CHAR),
  creation_date TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by    VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_sale_payment PRIMARY KEY (site_id, sales_number, seq)
);
CREATE INDEX ix_pos_sale_payment_mode ON pos_sale_payment (site_id, pay_mode);

-- Backfill: give every existing sale one payment row (full grand total under its
-- recorded mode) so historical "sales by mode" and shift reconciliation keep
-- reading cleanly from pos_sale_payment.
INSERT INTO pos_sale_payment (site_id, sales_number, seq, pay_mode, amount, tendered, change_due, creation_date, created_by)
SELECT site_id, sales_number, 1, mode_of_payment, grand_total, payment, change_due, creation_date, created_by
FROM   pos_sale_header;
