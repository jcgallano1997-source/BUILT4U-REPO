-- =============================================================================
-- Built4U POS — V21: price / discount override + approval.
-- =============================================================================
-- Records, per sale line, the ORIGINAL catalog price at sale time (list_price)
-- alongside the price actually charged (unit_cost). Because list_price is a
-- snapshot, the Discounts & Overrides report stays correct even if the item's
-- catalog price later changes — reports never recompute from current prices.
--
-- A cashier who lacks the PRICE_OVERRIDE module needs a manager's approval to
-- override a price or apply a line discount; the approver's username is stamped
-- on the affected line (approved_by) with an override_reason.
--
-- New DISCOUNTS_REPORT module gates the audit report. Both modules default to
-- OWNER and MANAGER (ADMIN gets them via wildcard). All objects live in BUILT4U.
-- =============================================================================

ALTER TABLE pos_sale_item ADD (
  list_price      NUMBER(38,2),
  override_reason VARCHAR2(255 CHAR),
  approved_by     VARCHAR2(50 CHAR)
);

-- Existing lines had no override: original price = price charged.
UPDATE pos_sale_item SET list_price = unit_cost WHERE list_price IS NULL;

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('PRICE_OVERRIDE', 'Price / Discount Override',
        'Override a line price or apply a line discount at POS (and approve others). Admin, Owner, Manager by default.', 58);

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('DISCOUNTS_REPORT', 'Discounts & Overrides Report',
        'Audit trail of every price override and line discount, with reason and approver. Admin, Owner, Manager by default.', 59);

INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code IN ('OWNER', 'MANAGER')
   AND m.code IN ('PRICE_OVERRIDE', 'DISCOUNTS_REPORT');
