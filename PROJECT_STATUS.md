# Built4U POS (single-business) — Project Status & Roadmap

> **Living tracker.** This is the single source of truth for "where are we". Update
> the checkboxes as phases complete. Committed to git so it survives across sessions
> (if the chat/token context is lost, read this first).
>
> Last updated: **2026-07-26** · Current position: **Phase 9 complete → Phase 10 next**

---

## 0. What this is

A **single-business** point-of-sale system: one business, many **sites** (branches).
It is a de-tenanted fork of **FreePOS** (a multi-tenant SaaS POS) — the ENTITY/tenant
layer and the whole SaaS stack (plans, billing, superadmin, self-signup) are removed,
and **`site_id` is the top data-isolation key**. Same tech stack (Java 21 / Spring Boot
+ React/Vite). Local-only for now; hosting comes later.

- **FreePOS source to port from (read-only reference):** `C:\CLAUDE CODE\FreePOS`
- **This repo (local):** `C:\CLAUDE CODE\NEW_POS`
- **GitHub:** https://github.com/jcgallano1997-source/BUILT4U-REPO.git

---

## 1. Quick reference

| Thing | Value |
|---|---|
| Backend package | `com.built4u.pos` |
| DB object prefix | `pos_` |
| Oracle schema | **`BUILT4U`** (empty, isolated) on ADB `G73342F118533B2_BUILT4U` (ap-singapore-1) |
| ⛔ Do NOT touch | the **`FREEPOS`** schema (the live product) |
| Backend port (local) | `http://localhost:8083` |
| Seed login | user **`admin`** · pass **`admin123`** · site **`MAIN`** (must-change on first login) |
| DB user | `built4u` (password NOT stored here — set `DB_PASSWORD` env / run script) |

**Run the backend (your own terminal — the agent shell can't bind a local server):**
```powershell
cd "C:\CLAUDE CODE\NEW_POS\backend"; .\run-local.ps1
```

**Run the frontend (separate terminal; proxies /api → :8083):**
```powershell
cd "C:\CLAUDE CODE\NEW_POS\frontend"; .\run-frontend.ps1
```
Then open http://localhost:5173 and log in. (First install uses `npm install --legacy-peer-deps`.)

**Run backend integration tests (works anywhere — MOCK web env, no Tomcat):**
```powershell
cd "C:\CLAUDE CODE\NEW_POS\backend"
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
$env:DB_USERNAME='built4u'; $env:DB_PASSWORD='<pw>'; $env:TNS_ADMIN='C:\CLAUDE CODE\NEW_POS\backend\wallet'
.\mvnw.cmd test
```

**Environment note:** the agent's shell can't boot a Java web server (NIO loopback
self-pipe is blocked) — Flyway migrations + `MOCK`-web tests DO run there; the **user**
runs the real server locally for HTTP smoke tests.

---

## 2. Locked decisions

- **D1** — Single business. No ENTITY/tenant layer. `site_id` is the isolation key.
- **D2** — Own Oracle schema (`BUILT4U`), fully separate from the live `FREEPOS`.
- **D3** — Objects prefixed `pos_`; own Flyway ledger (`flyway_schema_history` in `BUILT4U`).
- **D4** — Access = **role grants only** (no plan entitlements). ADMIN=wildcard.
- **D5** — Tenant/site context derives from the JWT (security context) only, never request params.
- **Oracle:** every length-bearing column is `VARCHAR2(n CHAR)`; migrations are append-only;
  read the live data dictionary before writing DDL.

---

## 3. Phase checklist

### ✅ Phase 0 — Setup & isolation  *(DONE)*
- [x] Decide isolation: separate Oracle schema (`BUILT4U`)
- [x] Connect via wallet (mTLS) + credentials confirmed
- [x] Map schema landscape (FREEPOS = live, BUILT4U = empty target)
- [x] New local repo + GitHub repo, wallet in place (gitignored)

### ✅ Phase 1 — Skeleton + core schema  *(DONE)*
- [x] Spring Boot skeleton (`com.built4u.pos`), wallet-wired datasource
- [x] Flyway **V1** core baseline → 9 `pos_` tables in BUILT4U (de-tenanted)
- [x] Boot + Flyway verified against BUILT4U; `/api/ping`
- [x] Committed + pushed

### ✅ Phase 2 — Auth + RBAC slice (backend)  *(DONE)*
- [x] Core entities (User/Site/Role/Module + refresh/reset tokens) → `pos_` tables
- [x] Site-scoped JWT (HS512), refresh rotation, BCrypt-12, 5-attempt lockout
- [x] `SecurityConfig` (no OAuth/plan gating), `TenantContext` site-only
- [x] Flyway **V2** seed: 3 roles, 41 modules, admin user, MAIN site
- [x] `AuthFlowIT` (login / me / refresh / 401) green
- [x] Committed + pushed

### ✅ Phase 3 — Frontend login slice  *(DONE)*
- [x] Vite 8 + React 19 + TS scaffold, Tailwind 4, router
- [x] `authStore` (zustand persist, key `built4u-pos-auth`), `api.ts` axios + single-flight refresh interceptor
- [x] Login page (username + site-select + password) → stores JWT → protected Dashboard reading `/api/auth/me`
- [x] Change-password screen (must-change flow, guarded redirect)
- [x] `npm run build` green; login page render verified in browser
- [ ] **Your smoke test:** run backend + `frontend/run-frontend.ps1`, log in `admin`/`admin123`/`MAIN`

### ✅ Phase 4 — Admin: users / sites / roles  *(DONE)*
- [x] Site management (create/edit/activate branches) — `/api/admin/sites`, SitesPage
- [x] User management (create/edit, assign roles + site access, reset password) — `/api/admin/users`, UsersPage
- [x] Role management (custom roles + module-grant grid; built-in/last-admin guards) — `/api/admin/roles`, RolesPage
- [x] Module-filtered nav in AppLayout; `@PreAuthorize(MOD_SITES/USERS/ROLES)`
- [x] `AdminFlowIT` green (create site/role/user, pickers, 409 on dup); `npm run build` green
- [ ] **Your smoke test:** log in as admin → Sites/Users/Roles tabs; create a branch + a cashier user

### ✅ Phase 5 — Reference data + inventory  *(DONE)*
- [x] Flyway **V3**: pos_category / pos_location / pos_uom / pos_inventory / pos_transaction_log (+ 3 sequences)
- [x] Site-scoped composite-key entities (@IdClass + sequence); JPA auditing (AuditorAware)
- [x] Item master (selling vs cost price, barcode, warning/critical thresholds, stock-level compute)
- [x] Category / Location / UOM lookups; stock **adjust** writes a transaction-log row
- [x] Backend CRUD scoped by `TenantContext.getSiteId()`; frontend Inventory/Categories/Locations/Units pages + nav
- [x] `InventoryFlowIT` green (create chain, stock adjust→CRITICAL, dup→409, unknown-ref→404); `npm run build` green
- [ ] **Your smoke test:** create a category+location+unit, then an item; use Adjust to change stock
- [ ] *(deferred to backlog)* Inventory bulk import (xlsx) — needs POI + the report/export layer

### ✅ Phase 6 — Sales + shifts (the POS core)  *(DONE)*
- [x] Flyway **V4**: pos_shift / pos_sale_header / pos_sale_item / pos_return_item (one-open-shift-per-cashier functional index)
- [x] Shifts: open/current/close with cash reconciliation (expected = float + cash sales − refunds; snapshot at close)
- [x] Checkout: stock decrement (FOR UPDATE lock, ascending itemId) + STOCK_OUT_SALE log; price snapshot; discounts
- [x] Void (restore all stock) + partial Refund (returns, restore stock, REFUNDED when fully returned)
- [x] Frontend: POS (shift-gated cart→checkout), Shifts (open/close + history), Sales (list + detail + void + refund) + nav
- [x] `PosFlowIT` green (checkout→refund→void→reconciliation, variance 0; insufficient-stock→400); `npm run build` green
- [ ] **Your smoke test:** open a shift → ring a sale in POS → refund/void it in Sales → close the shift
- [ ] *(deferred to later phases)* surcharges, AR/CHARGE credit, loyalty, vouchers, receipt PDF/email

### ✅ Phase 7 — Customers & suppliers (+ payment modes)  *(DONE)*
- [x] Flyway **V5**: pos_customer / pos_supplier (+ seqs), pos_payment_mode (seeded CASH/GCASH/PAYMAYA/CARD for MAIN)
- [x] Customers CRUD (points column present, loyalty logic deferred) + Suppliers CRUD (unique code)
- [x] Payment-mode catalog (per-site, entity-free): surcharge type/value + is_cash/AR/customer-required flags
- [x] POS loads modes from the catalog + optional customer attach; Sales show the customer
- [x] `CustomerSupplierPaymentIT` green (CRUD, dup→409, seeded modes, customer-attached checkout); `npm run build` green
- [ ] **Your smoke test:** add a customer + a payment mode; ring a sale in POS with a customer + non-cash mode
- [ ] *(deferred)* surcharge application at checkout; AR/CHARGE credit (Phase 9)

### ✅ Phase 8 — Procurement  *(DONE)*
- [x] Flyway **V6**: pos_purchase_order / pos_goods_receipt (line-per-row, header denormalized) + pos_po_approver (business-wide creator→approver map) + pos_po_approval (approval sidecar)
- [x] Purchase orders: create (auto-approve when creator has no approver) → approve → cancel; PO# `PO-YYYY-NNNN`; status DRAFT→APPROVED→PARTIALLY_RECEIVED→RECEIVED / CANCELLED aggregated from line rows
- [x] Goods receipts: receive against a PO (remaining-qty guard, falls back to PO price) or **direct** (no PO); bumps item stock + refreshes cost price + writes STOCK_IN_GR log; GR# `GR-YYYY-NNNN`; recomputes PO status
- [x] PO approver routing: per-user creator→approver map (ADMIN admin page); DRAFT until designated approver / ADMIN approves; "pending my approval"
- [x] Frontend: PurchaseOrdersPage (list/filter/create+line builder/detail approve+cancel), GoodsReceiptsPage (PO-based or direct receive), PoApproversPage + nav/routes
- [x] `ProcurementFlowIT` green (auto-approve→receive→RECEIVED, over-receive→400, approver routing DRAFT→approve, direct GR, cancel blocks receiving); `npm run build` green
- [ ] **Your smoke test:** create a PO → receive part of it in Receiving (stock rises) → receive the rest (PO shows Received)
- [ ] *(deferred to Phase 9)* AP payable auto-creation on receipt for AP-flagged suppliers

### ✅ Phase 9 — Accounts Receivable / Payable  *(DONE)*
- [x] Flyway **V7**: pos_receivable / pos_receivable_payment, pos_payable / pos_payable_payment; + credit_limit on pos_customer; + ap_enabled/payable_days on pos_supplier
- [x] AR: a credit sale (checkout on an `accounts_receivable` payment mode) opens a receivable for the unpaid balance; per-customer credit limit blocks oversized credit; collections (OPEN→PARTIAL→PAID); voiding the sale cancels the receivable
- [x] AP: goods receipt from an AP-enabled supplier auto-creates a PURCHASE payable (the hook deferred from Phase 8; non-AP suppliers unaffected); manual EXPENSE payables; disbursements (OPEN→PARTIAL→PAID)
- [x] Customer form gains a credit limit; supplier form gains AP tracking + terms; POS supports credit checkout (customer-gated, partial down-payment, "on account" shown)
- [x] `ArApFlowIT` green (5 tests: credit sale→collect→paid, credit-limit block, void→cancel, GR→payable→pay, non-AP no-payable + expense); PosFlowIT/CustomerSupplierPaymentIT still green; `npm run build` green
- [ ] **Your smoke test:** set a customer credit limit + an AR payment mode → sell on credit in POS → collect in Receivables; flag a supplier AP + receive a PO → pay it in Payables
- [ ] *(deferred to Phase 12)* AR/AP report exports (PDF/xlsx)

### ⬜ Phase 10 — Stock transfers (cross-site)
- [ ] Transfer ship/receive + line items + allow-list policy

### ⬜ Phase 11 — Vouchers & loyalty
- [ ] Vouchers + redemptions
- [ ] Loyalty config / ledger / rewards / redemptions

### ⬜ Phase 12 — Reports + exports
- [ ] Sales / inventory / AR / AP / procurement / shift-history reports
- [ ] PDF (Flying Saucer) + xlsx (POI) export engine

### ⬜ Phase 13 — Config, docs & audit
- [ ] PDF template / receipt template / doc settings / report-email config
- [ ] Business audit log + persistent error log
- [ ] Bundled User Guide PDF (help endpoint)

### ⬜ Phase 14 — Hardening + deployment
- [ ] Per-IP auth rate limiter, prod CSP/headers, `include-message: never`
- [ ] Prod profile + env vars; choose hosting (Render/other) + wallet deploy
- [ ] Smoke harness; DataSeeder fail-closed verified in prod

---

## 4. Notes / backlog
- Email flows (forgot-password / reset / report email) were deferred out of Phase 2 —
  add when the mail dependency + `EmailService` are ported (Phase 13 or when needed).
- `PasswordResetToken` table/entity exist from V1 but the issuing flow is not wired yet.
- Frontend CSP, security headers, and the cross-domain refresh-cookie question are
  deferred to Phase 14 (deployment).
