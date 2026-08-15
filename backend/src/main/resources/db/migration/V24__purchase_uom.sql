-- =============================================================================
-- Built4U POS — V24: purchase unit of measure (buy-by-box, sell-by-piece).
-- =============================================================================
-- An item's `uom` remains its BASE unit — the unit it is stocked and sold in
-- (e.g. PC, or METER, which already supports fractional selling). This adds an
-- optional PURCHASE unit and pack size so a receipt can be entered in the unit
-- the supplier sells in:
--   purchase_uom  — label of the buying unit (e.g. BOX, ROLL)
--   pack_size     — base units per purchase unit (e.g. 12 pcs per box)
--
-- At goods receipt, receiving in the purchase unit converts qty and cost to base
-- units for stock, moving-average cost, and the GR row. Selling is unchanged —
-- always in the base unit. BUILT4U only; FREEPOS is never touched.
-- =============================================================================

ALTER TABLE pos_inventory ADD (
  purchase_uom VARCHAR2(50 CHAR),
  pack_size    NUMBER(38,4)
);
