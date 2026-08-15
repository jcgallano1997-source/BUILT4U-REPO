-- =============================================================================
-- Built4U POS — V16: separate "Edit item" and "Adjust stock" permissions.
-- =============================================================================
-- Pulls item EDIT (PUT/DELETE /api/items) and stock ADJUST (POST /adjust) out of
-- the general INVENTORY module into their own modules, so each can be granted or
-- revoked per role in the Role editor. After this, plain INVENTORY = view/list.
--
-- Defaults preserve today's access (no one loses anything):
--   * Both are granted to OWNER and MANAGER (who could edit/adjust before).
--   * ADMIN gets them via wildcard.
--   * STOCKTAKE users keep stock-adjust via the stocktake path (hasAnyAuthority).
-- All objects live in the BUILT4U schema. FREEPOS is never touched.
-- =============================================================================

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('INVENTORY_EDIT', 'Edit Inventory Items',
        'Update or remove existing items. Admin, Owner, Manager by default.', 56);

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('INVENTORY_ADJUST', 'Adjust Stock',
        'Post stock adjustments (add/remove quantity with a reason). Admin, Owner, Manager by default.', 57);

-- Grant both to OWNER and MANAGER (admin already has every module via wildcard).
INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code IN ('OWNER', 'MANAGER')
   AND m.code IN ('INVENTORY_EDIT', 'INVENTORY_ADJUST');
