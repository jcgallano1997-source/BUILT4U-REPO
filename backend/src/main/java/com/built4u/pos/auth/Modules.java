package com.built4u.pos.auth;

import java.util.List;

/**
 * Single source of truth for permissionable module codes.
 *
 * <p>Must stay in sync with the {@code pos_module} catalog seeded by Flyway.
 * {@link #ALL} drives wildcard (ADMIN) authority expansion in
 * {@link JwtAuthenticationFilter} without a DB hit.
 *
 * <p>The SaaS-only SUPER_ADMIN module from FreePOS is intentionally dropped —
 * this is a single-business app with no cross-tenant console.
 */
public final class Modules {

    private Modules() {}

    public static final String POS               = "POS";
    public static final String SALES             = "SALES";
    public static final String SHIFTS            = "SHIFTS";
    public static final String SHIFTS_ADMIN      = "SHIFTS_ADMIN";
    public static final String INVENTORY         = "INVENTORY";
    public static final String STOCKTAKE         = "STOCKTAKE";
    public static final String CUSTOMERS         = "CUSTOMERS";
    public static final String SUPPLIERS         = "SUPPLIERS";
    public static final String PURCHASE_ORDERS   = "PURCHASE_ORDERS";
    public static final String GOODS_RECEIPTS    = "GOODS_RECEIPTS";
    public static final String CATEGORIES        = "CATEGORIES";
    public static final String LOCATIONS         = "LOCATIONS";
    public static final String UOMS              = "UOMS";
    public static final String SALES_REPORTS       = "SALES_REPORTS";
    public static final String INVENTORY_SNAPSHOT  = "INVENTORY_SNAPSHOT";
    public static final String INVENTORY_VALUATION = "INVENTORY_VALUATION";
    public static final String INVENTORY_MOVEMENT  = "INVENTORY_MOVEMENT";
    public static final String USERS             = "USERS";
    public static final String SITES             = "SITES";
    public static final String ROLES             = "ROLES";
    public static final String EMAIL_CONFIG      = "EMAIL_CONFIG";
    public static final String PDF_CONFIG        = "PDF_CONFIG";
    public static final String INVENTORY_IMPORT  = "INVENTORY_IMPORT";
    public static final String RECEIPT_CONFIG     = "RECEIPT_CONFIG";
    public static final String VOUCHERS           = "VOUCHERS";
    public static final String PAYMENT_MODES      = "PAYMENT_MODES";
    public static final String LOYALTY_CONFIG     = "LOYALTY_CONFIG";
    public static final String LOYALTY_REWARDS    = "LOYALTY_REWARDS";
    public static final String RECEIVABLES        = "RECEIVABLES";
    public static final String GOODS_RECEIPTS_REPORT = "GOODS_RECEIPTS_REPORT";
    public static final String PURCHASE_ORDERS_REPORT = "PURCHASE_ORDERS_REPORT";
    public static final String AUDIT_LOG          = "AUDIT_LOG";
    public static final String PAYABLES           = "PAYABLES";
    public static final String STOCK_TRANSFER     = "STOCK_TRANSFER";
    public static final String STOCK_TRANSFER_POLICY = "STOCK_TRANSFER_POLICY";
    public static final String STOCK_TRANSFER_REPORT = "STOCK_TRANSFER_REPORT";
    public static final String SHIFT_HISTORY_REPORT  = "SHIFT_HISTORY_REPORT";
    public static final String RECEIVABLES_REPORT    = "RECEIVABLES_REPORT";
    public static final String PAYABLES_REPORT       = "PAYABLES_REPORT";
    public static final String DOC_SETTINGS          = "DOC_SETTINGS";
    public static final String PO_APPROVERS          = "PO_APPROVERS";

    /** Every module code. Order matches the catalog sort order. */
    public static final List<String> ALL = List.of(
        POS, SALES, SHIFTS, SHIFTS_ADMIN, INVENTORY, STOCKTAKE,
        CUSTOMERS, SUPPLIERS, PURCHASE_ORDERS, GOODS_RECEIPTS,
        CATEGORIES, LOCATIONS, UOMS, SALES_REPORTS,
        INVENTORY_SNAPSHOT, INVENTORY_VALUATION, INVENTORY_MOVEMENT,
        USERS, SITES, ROLES, EMAIL_CONFIG, PDF_CONFIG, INVENTORY_IMPORT,
        RECEIPT_CONFIG, VOUCHERS, PAYMENT_MODES, LOYALTY_CONFIG, LOYALTY_REWARDS,
        RECEIVABLES, GOODS_RECEIPTS_REPORT, PURCHASE_ORDERS_REPORT, AUDIT_LOG,
        PAYABLES, STOCK_TRANSFER, STOCK_TRANSFER_POLICY,
        STOCK_TRANSFER_REPORT, SHIFT_HISTORY_REPORT, RECEIVABLES_REPORT, PAYABLES_REPORT,
        DOC_SETTINGS, PO_APPROVERS
    );

    /** Wildcard sentinel placed in the JWT {@code modules} claim for ADMIN. */
    public static final String WILDCARD = "*";
}
