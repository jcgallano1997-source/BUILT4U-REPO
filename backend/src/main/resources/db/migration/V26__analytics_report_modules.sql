-- =============================================================================
-- Built4U POS — V26: analytics report modules (profit, sales analytics,
-- dead stock, customer history).
-- =============================================================================
-- Each gates one of the new post-launch analytics reports. Profit/margin is a
-- separate module from SALES_REPORTS so cost/margin can be withheld from staff
-- who may still see plain sales figures. Defaults to OWNER + MANAGER (ADMIN via
-- wildcard). BUILT4U only; FREEPOS is never touched.
-- =============================================================================

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('PROFIT_REPORT', 'Profit &amp; Margin Report',
        'Revenue, cost of goods and margin per item. Admin, Owner, Manager by default.', 61);

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('SALES_ANALYTICS', 'Sales Analytics (by cashier / hour)',
        'Sales broken down by cashier and by hour of day. Admin, Owner, Manager by default.', 62);

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('DEAD_STOCK_REPORT', 'Dead Stock Report',
        'Slow / non-moving items with idle days and tied-up value. Admin, Owner, Manager by default.', 63);

INSERT INTO pos_module (code, name, description, sort_order)
VALUES ('CUSTOMER_REPORT', 'Customer Purchase Report',
        'Per-customer purchase totals, frequency and recency. Admin, Owner, Manager by default.', 64);

INSERT INTO pos_role_module (role_id, module_code)
SELECT r.id, m.code
  FROM pos_role r, pos_module m
 WHERE r.code IN ('OWNER', 'MANAGER')
   AND m.code IN ('PROFIT_REPORT', 'SALES_ANALYTICS', 'DEAD_STOCK_REPORT', 'CUSTOMER_REPORT');
