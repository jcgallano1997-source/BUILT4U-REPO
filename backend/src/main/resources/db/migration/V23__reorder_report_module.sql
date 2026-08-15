-- =============================================================================
-- Built4U POS — V23: Reorder Suggestions report module.
-- =============================================================================
-- Gates a low-stock report: active items at/below their warning or critical
-- threshold, with a suggested reorder quantity and its estimated cost. Defaults
-- to OWNER and MANAGER (ADMIN via wildcard). BUILT4U only; FREEPOS untouched.
-- =============================================================================

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('REORDER_REPORT', 'Reorder Suggestions Report',
        'Low-stock items with suggested reorder quantities. Admin, Owner, Manager by default.', 60);

INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code IN ('OWNER', 'MANAGER')
   AND m.code = 'REORDER_REPORT';
