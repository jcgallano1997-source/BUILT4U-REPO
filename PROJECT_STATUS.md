# Built4U POS (single-business) — Project Status & Roadmap

> **Living tracker.** This is the single source of truth for "where are we". Update
> the checkboxes as phases complete. Committed to git so it survives across sessions
> (if the chat/token context is lost, read this first).
>
> Last updated: **2026-08-15** · Current position: **Phase 14 deploy artifacts DONE (awaiting owner Render deploy); post-launch enhancements #2, #3, and most of #4 shipped — see §5. Migrations now V1–V26.**

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

### ✅ Phase 10 — Stock transfers (cross-site)  *(DONE)*
- [x] Flyway **V8**: pos_stock_transfer / pos_stock_transfer_item (line snapshots) + pos_stock_transfer_policy (allow-list)
- [x] Ship (source-site): locks + decrements source stock, STOCK_OUT_TRANSFER log, status IN_TRANSIT; `ST-YYYY-NNNN`
- [x] Receive (dest-site): increments dest stock, **auto-creates the item** at the destination if its code is new (cat/loc = dest's first active), STOCK_IN_TRANSFER log, status RECEIVED
- [x] Cancel (source-site, only while IN_TRANSIT): restores source stock, STOCK_IN_XFER_CANCEL log, status CANCELLED
- [x] Allow-list **policy** (empty = OPEN, ≥1 row = ENFORCED); destination picker filtered by it; first module to span two site contexts
- [x] Frontend: StockTransfersPage (list + direction/status filters + ship form + detail with site-gated receive/cancel), StockTransferPolicyPage (admin) + nav/routes
- [x] `StockTransferFlowIT` green (3 tests: ship→receive across MAIN/BR2 with auto-create, cancel restores, policy blocks/permits); `npm run build` green
- [ ] **Your smoke test:** create a 2nd site + give admin access to it; at MAIN ship an item to it; log into the 2nd site and Receive (stock lands there)
- [ ] *(deferred to Phase 12)* stock-transfer report export (PDF/xlsx)

### ✅ Phase 11 — Vouchers & loyalty  *(DONE)*
- [x] Flyway **V9**: pos_voucher / pos_voucher_redemption, pos_loyalty_config / pos_loyalty_ledger / pos_loyalty_reward / pos_loyalty_redemption
- [x] Vouchers: admin CRUD (FIXED/PERCENT, min-spend, validity, usage limit); POS `/vouchers/validate`; checkout applies + **atomically consumes** (single-use guard), folds into discountAll, records a redemption; **void releases** the voucher
- [x] Loyalty: per-site earn-% config; **earn points on checkout** (floored) when a customer is attached, with a ledger EARN entry; **void claws points back** (ADJUST)
- [x] Reward catalog (ITEM decrements stock / FREETEXT); redeem points for a reward (locks customer+reward, REDEEM ledger); customer points ledger view
- [x] Frontend: VouchersPage, LoyaltyConfigPage, LoyaltyRewardsPage (admin); POS voucher field (validate→discount→checkout); Customers "Points" panel (balance + ledger + redeem) + nav/routes
- [x] `VoucherLoyaltyFlowIT` green (3 tests: voucher apply/single-use/void-release, earn+reward-redeem, void claw-back); Pos/CustomerSupplierPayment/ArAp ITs still green; `npm run build` green
- [ ] **Your smoke test:** make a % voucher + set loyalty % + a reward; ring a sale with a customer + the voucher (discount applies, points earned); open the customer's Points panel and redeem the reward
- [ ] *(deferred)* points expiry (scheduled sweep) and points→voucher conversion

### ✅ Phase 12 — Reports + exports  *(DONE)*
- [x] Export engine: format-neutral `ExportTable` + **POI xlsx** exporter + **self-contained OpenPDF** exporter (no Flying-Saucer/templating — per-doc branding deferred to Phase 13); new deps `poi-ooxml` + `openpdf`
- [x] Reports hub `/api/reports/*` (JSON default, `?format=pdf|xlsx` download): sales-overview (new query: totals + by-mode + by-day), inventory-snapshot, inventory-valuation, receivables, payables, purchase-orders, goods-receipts, stock-transfers — each gated by its `*_REPORT` module
- [x] **Inventory import**: POST `/api/items/import` reads an .xlsx (POI), matches category/location/uom by name, upserts items by code, returns created/updated/skipped + per-row errors
- [x] Frontend: ReportsPage hub (sales-overview view + PDF/Excel download buttons per report), Inventory "Import xlsx" button + nav/route
- [x] `ReportFlowIT` green (sales-overview JSON, xlsx `PK` + pdf `%PDF` binaries, snapshot, import create/update/error); no migration (no new tables)
- [ ] **Your smoke test:** ring a sale, open **Reports** → Sales overview → View + Download; **Inventory → Import xlsx** with columns code,name,category,location,uom,quantity,sellingPrice,costPrice
- [ ] *(deferred to Phase 13)* per-document PDF branding/templates; email delivery of reports; shift-history & inventory-movement reports

> **Build note:** Phase 12 added Maven deps (Apache POI, OpenPDF) — the first
> `run-local.ps1` / `mvnw` after pulling needs network access to download them.

### ✅ Phase 13 — Config, docs & audit  *(DONE)*
- [x] **Business audit log** — Flyway **V10** `pos_audit_log`; a universal Hibernate post-insert/update/delete listener writes one row per business change via JDBC (joins the tx, rolls back with it, never re-triggers). Captures JWT user + site + before→after field changes; redacts sensitive fields; **skips** high-volume internal logs (transaction log, loyalty ledger, auth tokens). Filter/paginate API + pdf/xlsx export (`MOD_AUDIT_LOG`); AuditLogPage with a change-diff modal + nav/route
- [x] `AuditLogFlowIT` green (capture with user + readable entity id, skip-list, xlsx export); Pos/Admin ITs still green; `npm run build` green
- [x] **Persistent error log** — Flyway **V11** `pos_error_log`; the global exception handler records every unhandled 5xx in a `REQUIRES_NEW` tx (commits even as the request rolls back), redacting credential-like fragments; read API + detail (`MOD_AUDIT_LOG`); ErrorLogPage (list + stack-trace modal) + nav/route. `ErrorLogFlowIT` green (record → read-back → redaction, self-cleaning; redact unit test)
- [x] **Doc / branding settings** — Flyway **V12** `pos_doc_settings` (business name/address/contact/TIN, report footer note, accent colour, receipt title/footer); admin get/save (`MOD_DOC_SETTINGS`); DocSettingsPage
- [x] **Branded report PDFs** — `ReportPdfExporter` now stamps the letterhead (business identity) + accent-coloured header row + footer note from doc settings
- [x] **Sale receipt PDF** — `GET /api/sales/{n}/receipt` renders an 80mm-style OpenPDF receipt from the sale + branding; "Receipt" button on the Sales detail
- [x] `DocSettingsReceiptFlowIT` green (branding save/read-back, branded report PDF `%PDF`, receipt PDF `%PDF`); `npm run build` green
- [ ] *(deferred to Phase 14)* report/receipt **email delivery** — needs SMTP config, which lands with deployment
- [ ] *(backlog, low value)* bundled User-Guide PDF; logo-image upload on documents; `AuditContext` "why" stamping on key flows

### 🔄 Phase 14 — Hardening + deployment  *(IN PROGRESS)*
- [x] **Per-IP login rate limiter** (`LoginRateLimitFilter`, fixed window, 429 + Retry-After) on `POST /api/auth/login`; configurable, neutralised in the test suite; `RateLimitFlowIT` green
- [x] **Security headers** — CSP (already), + frame-options DENY, permissions-policy, and HSTS (toggled by `app.security.hsts-enabled`, on in prod / off local http)
- [x] **Prod profile** `application-prod.yml` — all secrets from env with NO defaults for JWT_SECRET / DB / CORS / seed-admin (fail-closed at startup); `server.error.include-message: never` (+stacktrace/binding/exception off); `forward-headers-strategy: framework`; quieter logging
- [x] DataSeeder already **fails closed** on the weak default admin password under `prod` (verified)
- [x] Full backend IT suite green with the hardening in place
- [x] **Deploy artifacts (Render)** — `backend/Dockerfile` (+`.dockerignore`), `render.yaml` (Dockerized API + static React site), prod CSP that whitelists the API origin, and `DEPLOYMENT.md` runbook (wallet upload, env-var table, CORS loop, smoke check). Frontend builds verified for both local (`connect-src 'self'`) and hosted (`+ API origin`)
- [ ] **Owner action:** create the Render services, upload the wallet as Secret Files, set prod env vars (per DEPLOYMENT.md), deploy
- [ ] **Owner+me:** prod smoke check (health → login → one sale) after first deploy
- [ ] *(deferred)* SMTP config → wire report/receipt **email delivery**

---

## 5. Enhancement backlog (post-launch "what's missing" assessment — 2026-08-15)

From an assessment of gaps in the POS. Numbering kept from that review.

### ✅ Done (2026-08-15, verified live)
- **#2 — Checkout / cash handling:**
  - (A) Split / multiple tender per sale (V19 `pos_sale_payment`; reports/shift reconcile by applied amount)
  - (B) Mid-shift cash in/out + denomination count at close (V20; expected cash = float + cash sales − refunds + in − out)
  - (C) Price/discount override with manager approval + **Discounts & Overrides** report (V21; `PRICE_OVERRIDE`, `DISCOUNTS_REPORT` modules; `list_price`/`approved_by` snapshot per line)
- **#3 — Inventory realities:**
  - Moving-average costing (V22; running weighted-avg cost on receipt; `unit_cogs` snapshot per sale line; **Cost + Margin** columns in Sales → Detail)
  - **Reorder Suggestions** report (V23; `REORDER_REPORT` module)
  - Purchase-UOM conversions — buy-by-box / sell-by-piece (V24; `purchase_uom` + `pack_size` on items; converted at direct goods receipt)
- **#4 (partial) — hardware + analytics:**
  - Direct hardware: **network ESC/POS thermal printing + cash-drawer kick** (V25; per-site printer host/port/enable/open-drawer on `pos_doc_settings`; `printer` package; Print button on Sales; Test print / Open drawer in Doc settings; encoder + transport unit-tested with a loopback socket)
  - Analytics reports (V26): **Profit & Margin**, **Sales by Cashier**, **Sales by Hour**, **Dead Stock**, **Customer Purchases** (`PROFIT_REPORT`, `SALES_ANALYTICS`, `DEAD_STOCK_REPORT`, `CUSTOMER_REPORT` modules)
  - Receiving hardening: searchable **registered-supplier** picker + mandatory Supplier/Reference/Unit-cost; **reprice-on-receive** — when the moving-average cost rises, prompt a markup-preserving new selling price per item (`PUT /items/{id}/selling-price`)
- **UI — "Hardware Edition" redesign** (frontend): amber accent + signature motifs (safety-stripe, blueprint-grid, amber active-nav inset); rebuilt Login (blueprint brand panel) and Change Password (live rule chips); safety-stripe headers on the shared `Modal` (all dialogs) + refined buttons/inputs app-wide; POS cart-rail stripe. Applied via shared components, so it lands across all screens.

### ⬜ Open items
- [ ] **#1 — BIR / PH tax compliance** *(biggest gap; legally required for a real PH store; should be switchable on/off since not every business is VAT-registered)*
  - VAT (12%) computation + receipt breakdown: VATable Sales, VAT Amount, VAT-Exempt, Zero-Rated
  - Senior Citizen & PWD discounts (20% + VAT exemption, ID/name capture logged for BIR)
  - Z-reading / X-reading (accumulated grand total, reset counter, non-resettable series)
  - Official-Receipt essentials (permit/accreditation numbers, OR series, "This serves as your Official Receipt")
- [ ] **#4 (remaining) — Reliability**
  - Offline mode (keep selling when the internet drops)

---

## 4. Notes / backlog
- Email flows (forgot-password / reset / report email) were deferred out of Phase 2 —
  add when the mail dependency + `EmailService` are ported (Phase 13 or when needed).
- `PasswordResetToken` table/entity exist from V1 but the issuing flow is not wired yet.
- Frontend CSP, security headers, and the cross-domain refresh-cookie question are
  deferred to Phase 14 (deployment).
