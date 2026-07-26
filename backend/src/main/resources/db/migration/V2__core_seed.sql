-- =============================================================================
-- Built4U POS (single-business) — V2 core seed.
-- =============================================================================
-- Everything needed to boot and log in:
--   1. Three built-in roles (ADMIN wildcard, MANAGER, CASHIER)
--   2. Module catalog (matches com.built4u.pos.auth.Modules.ALL)
--   3. MANAGER + CASHIER module grants (ADMIN gets none — wildcard short-circuits)
--   4. admin user (placeholder bcrypt; DataSeeder rehashes at boot, force-change on)
--   5. Default site MAIN, bound to admin
--
-- No entity/tenant rows: username and site.code are globally unique here.
-- =============================================================================


-- =============================================================================
-- SECTION 1 — Roles
-- =============================================================================
INSERT INTO pos_role (code, name, description, built_in, wildcard)
VALUES ('ADMIN',   'Administrator', 'Full access to all modules',    'Y', 'Y');

INSERT INTO pos_role (code, name, description, built_in, wildcard)
VALUES ('MANAGER', 'Manager',       'Inventory, reports, and POS',   'Y', 'N');

INSERT INTO pos_role (code, name, description, built_in, wildcard)
VALUES ('CASHIER', 'Cashier',       'POS only and own transactions', 'Y', 'N');


-- =============================================================================
-- SECTION 2 — Module catalog
-- =============================================================================
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('POS',                    'Point of Sale',                'Ring up new sales',                                                                10);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SALES',                  'Sales',                        'Browse, refund, void sales',                                                       20);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SHIFTS',                 'Shifts',                       'Open/close own cashier shift',                                                     30);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SHIFTS_ADMIN',           'Shift History',                'All cashier shift closures',                                                       40);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('INVENTORY',              'Inventory',                    'Items and stock',                                                                  50);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('STOCKTAKE',              'Stocktake',                    'Physical-count workflow',                                                          60);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('CUSTOMERS',              'Customers',                    'Customer records',                                                                 70);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SUPPLIERS',              'Suppliers',                    'Supplier records',                                                                 80);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PURCHASE_ORDERS',        'Purchase Orders',              'Create and track POs',                                                             90);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('STOCK_TRANSFER',         'Stock Transfers',              'Ship & receive inventory between sites',                                           95);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('GOODS_RECEIPTS',         'Goods Receiving',              'Receive stock',                                                                   100);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('CATEGORIES',             'Categories',                   'Item category lookup',                                                            110);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('LOCATIONS',              'Locations',                    'Storage location lookup',                                                         120);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('UOMS',                   'Units (UOM)',                  'Units of measure lookup',                                                         130);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SALES_REPORTS',          'Sales Reports',                'Daily totals, top items, detail',                                                 140);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('INVENTORY_SNAPSHOT',     'Inventory Snapshot Report',    'Current stock levels with filters',                                               150);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('INVENTORY_VALUATION',    'Inventory Valuation Report',   'Stock value by category',                                                         151);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('INVENTORY_MOVEMENT',     'Inventory Movement Report',    'Per-item stock movement history',                                                 152);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('GOODS_RECEIPTS_REPORT',  'Goods Receive Report',         'Header + line detail of goods receipts',                                          153);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PURCHASE_ORDERS_REPORT', 'Purchase Order Report',        'Header + line detail of purchase orders',                                         154);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('STOCK_TRANSFER_REPORT',  'Stock Transfers Report',       'Header + line detail of cross-site stock transfers',                              156);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SHIFT_HISTORY_REPORT',   'Shift History Report',         'Shift summary + the sales rung up during each shift',                             157);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('RECEIVABLES_REPORT',     'Accounts Receivable Report',   'Receivable balances + payment history per record',                                158);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PAYABLES_REPORT',        'Accounts Payable Report',      'Payable balances + payment history per record',                                   159);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('USERS',                  'User Management',              'Manage user accounts',                                                            160);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('SITES',                  'Site Management',              'Branches / outlets',                                                              170);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('ROLES',                  'Role Management',              'Define roles and their modules',                                                  180);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('EMAIL_CONFIG',           'Email Configuration',          'Default report email recipients',                                                 190);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PDF_CONFIG',             'PDF Template',                 'Customize report PDF branding & layout',                                          200);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('INVENTORY_IMPORT',       'Inventory Import',             'Bulk-upload inventory items from a spreadsheet',                                  210);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('RECEIPT_CONFIG',         'Receipt Template',             'Customize the POS sales receipt',                                                 220);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('VOUCHERS',               'Vouchers',                     'Create & manage discount voucher codes',                                          230);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('STOCK_TRANSFER_POLICY',  'Stock Transfer Policy',        'Allow-list of permitted (source -> destination) site pairs for stock transfers',  231);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PAYMENT_MODES',          'Payment Modes',                'Configure payment methods & surcharges',                                          240);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('DOC_SETTINGS',           'Document Settings',            'Per-doc-type PDF generation + auto-email toggles',                                241);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PO_APPROVERS',           'PO Approvers',                 'Per-user PO approver routing (creator -> approver mapping)',                      245);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('LOYALTY_CONFIG',         'Loyalty Points',               'Set the % of points customers earn',                                              250);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('LOYALTY_REWARDS',        'Loyalty Rewards',              'Catalog of rewards customers redeem points for',                                  260);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('RECEIVABLES',            'Accounts Receivable',          'Track credit sales & collect outstanding balances',                               270);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('PAYABLES',               'Accounts Payable',             'Track and pay supplier purchases + manually-entered expenses',                    275);
INSERT INTO pos_module (code, name, description, sort_order) VALUES ('AUDIT_LOG',              'Audit Log',                    'Single source of truth - every create/update/delete change',                      280);


-- =============================================================================
-- SECTION 3 — Role -> module grants
-- =============================================================================
-- ADMIN gets NO rows (wildcard='Y' is short-circuited in JwtAuthenticationFilter).
INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code = 'MANAGER'
   AND m.code IN (
       'POS','SALES','SHIFTS','SHIFTS_ADMIN','INVENTORY','STOCKTAKE',
       'CUSTOMERS','SUPPLIERS','PURCHASE_ORDERS','GOODS_RECEIPTS',
       'CATEGORIES','LOCATIONS','UOMS','SALES_REPORTS',
       'INVENTORY_SNAPSHOT','INVENTORY_VALUATION','INVENTORY_MOVEMENT');

INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code = 'CASHIER'
   AND m.code IN ('POS','SHIFTS','CUSTOMERS');


-- =============================================================================
-- SECTION 4 — admin user + MAIN site
-- =============================================================================
-- Placeholder hash rewritten at boot by com.built4u.pos.config.DataSeeder.
INSERT INTO pos_user (username, password_hash, full_name, email, must_change_password, created_by)
VALUES ('admin', 'PLACEHOLDER_REPLACED_BY_DATA_SEEDER', 'System Administrator', NULL, 'Y', 'SYSTEM');

INSERT INTO pos_user_role (user_id, role_id)
SELECT u.id, r.id FROM pos_user u, pos_role r
 WHERE u.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO pos_site (code, name, address)
VALUES ('MAIN', 'Main Branch', NULL);

INSERT INTO pos_user_site (user_id, site_id)
SELECT u.id, s.id FROM pos_user u, pos_site s
 WHERE u.username = 'admin' AND s.code = 'MAIN';
