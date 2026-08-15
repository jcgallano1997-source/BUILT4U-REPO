-- =============================================================================
-- Built4U POS — V14: separate "Add inventory item" permission.
-- =============================================================================
-- Pulls item creation (POST /api/items) out of the general INVENTORY module into
-- its own INVENTORY_CREATE module, so it can be granted/revoked per role in the
-- Role editor. Default holders: ADMIN (via wildcard) + OWNER (granted below).
-- Managers/cashiers keep INVENTORY (view/edit/adjust) but can no longer ADD new
-- items until this module is granted to their role.
--
-- Inventory IMPORT already has its own module (INVENTORY_IMPORT, admin+owner),
-- so no change is needed there.
-- All objects live in the BUILT4U schema. FREEPOS is never touched.
-- =============================================================================

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('INVENTORY_CREATE', 'Add Inventory Items',
        'Create new inventory items (add SKUs). Admin + Owner by default.', 55);

-- Grant it to the business OWNER (admin already gets every module via wildcard).
INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, 'INVENTORY_CREATE' FROM pos_role r WHERE r.code = 'OWNER';
