-- =============================================================================
-- Built4U POS — V22: moving-average costing (cost-of-goods snapshot on sales).
-- =============================================================================
-- pos_inventory.sup_price becomes a running MOVING-AVERAGE cost: instead of being
-- overwritten to the latest receipt price, each goods receipt recomputes it as
--   newCost = (oldQty·oldCost + recvQty·recvCost) / (oldQty + recvQty).
-- (That logic lives in GoodsReceiptService — no schema change needed for it.)
--
-- To keep margin reporting accurate and stable over time, each sale line snapshots
-- the item's moving-average cost at the moment of sale into unit_cogs (cost of
-- goods sold per unit). Reports read this snapshot and never recompute margin from
-- the item's current cost — the same reports-alignment rule used elsewhere.
--
-- Backfill: existing sale lines get the item's current cost as a best-effort COGS.
-- All objects live in the BUILT4U schema. FREEPOS is never touched.
-- =============================================================================

ALTER TABLE pos_sale_item ADD (unit_cogs NUMBER(38,2));

UPDATE pos_sale_item si
   SET unit_cogs = (
     SELECT inv.sup_price FROM pos_inventory inv
      WHERE inv.site_id = si.site_id AND inv.item_id = si.item_id)
 WHERE unit_cogs IS NULL;

-- Any line whose item no longer exists (or had null cost) → 0 rather than null.
UPDATE pos_sale_item SET unit_cogs = 0 WHERE unit_cogs IS NULL;
