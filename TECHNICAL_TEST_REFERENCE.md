# Built4U POS (single-business) — Technical Test Reference

> Purpose: give QA enough technical detail (what each screen does, the API it
> calls, the database objects touched, the rules/permissions/edge cases) to
> write and execute test cases. Schema = Flyway **V1–V12**. Companion:
> `TEST_CASES.md` (the executable test catalog) and `PROJECT_STATUS.md`
> (history/decisions).
>
> When in doubt, the controllers under `backend/src/main/java/com/built4u/pos/**`
> and the migrations in `backend/src/main/resources/db/migration` are
> authoritative.

---

## 1. What the system is

Built4U POS is a **single-business, de-tenanted fork of FreePOS** (a
multi-tenant SaaS POS). The SaaS ENTITY/tenant layer is gone; the app runs one
business.

- **Backend:** Spring Boot (Java 21), Spring Data JPA + Flyway, Spring Security
  + JWT (HS512). Package `com.built4u.pos`. Runs on **`http://localhost:8083`**
  (`/api/**`). Start locally with `backend/run-local.ps1`.
- **Frontend:** React 19 + TypeScript + Vite + Tailwind 4, served at
  **`http://localhost:5173`** (Vite dev-proxies `/api` → :8083). Auth state in a
  zustand persist store (`built4u-pos-auth`); axios does single-flight refresh.
- **Database:** Oracle Autonomous DB, schema **`BUILT4U`** (connected via mTLS
  wallet, alias `built4u_low`). All objects are prefixed **`pos_`**. The live
  **`FREEPOS`** schema is never touched.
- **Site model:** every business table still has `site_id`; a logged-in user is
  scoped to one active site (chosen at login). A single business can still run
  **multiple sites/branches** (e.g. `MAIN`, `BR2`) — used by stock transfers and
  still the isolation key for reads. There is **no** tenant/ENTITY layer above
  the site.
- **Permissions:** dynamic, admin-managed **roles → modules**. Seeded roles:
  `ADMIN` (wildcard = all modules), `MANAGER`, `CASHIER`. The JWT carries the
  user's module codes; each becomes a Spring authority `MOD_<CODE>`; endpoints
  are gated with `hasAuthority('MOD_X')` / `hasAnyAuthority(...)`. The frontend
  hides nav + guards routes by the same modules. There are **41 modules** (see
  `com.built4u.pos.auth.Modules.ALL` / the `pos_module` catalog).
- **Email: NOT wired yet (deferred).** There is no `forgot-password` /
  `reset-password` flow and no report/receipt email delivery. Email-related
  cases live in the backlog section of `TEST_CASES.md`, not the active suite.

### Cross-cutting behaviour every test should know
- **Auth tokens:** short-lived access JWT + rotating single-use refresh token
  (stored only as a hash, bound to user+site). The frontend auto-refreshes on
  401 (single-flight) and bounces to `/login` if refresh fails. ⇒ permission/role
  changes only fully apply after token refresh or re-login.
- **Per-account lockout:** **5** failed attempts ⇒ `locked_until` set for **15
  minutes**; login blocked (even with the correct password) until it expires.
  (Constants: `AuthService.MAX_FAILED_ATTEMPTS=5`, `LOCKOUT_MINUTES=15`.)
- **Per-IP login rate limit:** `POST /api/auth/login` is throttled per IP —
  default **10 attempts / 900 s** (`app.security.login-rate.*`); exceeding it
  returns **429** with a `Retry-After` header. This is a separate brake from the
  per-account lockout.
- **Error response envelope (handled errors):**
  `{ "timestamp", "status", "error", "message" }`.
  Status mapping: 401 auth, 403 forbidden, 404 not found, 409 conflict,
  400 validation/bad-request, **413** upload too large, **429** rate-limited,
  **500** unexpected → generic message + a **reference id**; full detail is
  written to `pos_error_log` (see §4).
- **Forced change-password:** first login (seeded admin has
  `must_change_password='Y'`), an admin password reset, or a password older than
  `app.security.password-max-age-days` (default **90**) forces
  `/change-password` before anything else.
- **Seed data (dev):** user **`admin`** (role `ADMIN`, site **`MAIN`** "Main
  Branch"); bootstrap password from `app.security.seed-admin-password` (default
  **`admin123`**), force-change on first login. Payment modes seeded at MAIN:
  **CASH, GCASH, PAYMAYA, CARD**. `DataSeeder` rewrites the placeholder hash on
  first boot and refuses the built-in default password under the `prod` profile.

---

## 2. Login page → database objects used

**Route:** `/login` (anonymous). **Frontend:** `LoginPage.tsx`.

### Flow & endpoints
| Step | Endpoint | Notes |
|---|---|---|
| Type username | `GET /api/auth/sites?username=…` | Returns the active sites linked to that user (fired on the username field's `onBlur` and the Site dropdown's `onFocus`). Auto-selects when exactly 1; picker for multi-site; empty ⇒ "Enter username first" / no-site state. On error the dropdown silently shows empty. |
| Submit | `POST /api/auth/login` `{username, password, siteCode}` | Validates credentials + site; issues tokens. |
| (post-login) | redirect `/` (dashboard), or `/change-password` if `mustChangePassword`/expired | |

### Auth API surface (complete)
`GET /api/auth/sites` · `POST /api/auth/login` · `POST /api/auth/refresh` ·
`POST /api/auth/logout` · `POST /api/auth/change-password` · `GET /api/auth/me`.
**There is no forgot-password/reset-password endpoint.**

### Database objects touched by login
| Object | Role in login |
|---|---|
| `pos_user` | Look up by username; verify **BCrypt** `password_hash`; read `active`, `failed_attempts`, `locked_until`, `must_change_password`, `password_changed_at`, `id`, `full_name`. |
| `pos_role`, `pos_user_role` | Resolve roles (codes → JWT `roles` claim). |
| `pos_role_module` | Resolve effective module codes (union of the user's roles' modules; ADMIN wildcard ⇒ all) → JWT `modules` claim. |
| `pos_module` | Module catalog (41 rows). |
| `pos_site`, `pos_user_site` | `/auth/sites` lists the user's active sites; login validates the chosen `siteCode` belongs to the user and is active; JWT gets `siteId`/`siteCode`/`siteName`. |
| `pos_refresh_token` | A new refresh-token row is inserted (hash only, bound to `user_id` + `site_id`). |

### Rules / edge cases to test (login)
- **Wrong password:** 401 generic `"Invalid username or password"`; increments
  `failed_attempts`.
- **Lockout:** 5 failed ⇒ locked 15 min (correct password still blocked).
- **Rate limit:** > N logins/window from one IP ⇒ 429 + `Retry-After`.
- **Inactive user** (`active='N'`): cannot log in.
- **Wrong/missing site, unlinked site, or inactive site:** rejected.
- **First login / admin-reset / password > max age:** login succeeds but forces
  `/change-password`.
- **Multi-site user:** must pick a site; refresh token bound to that site.
- **`/me`:** returns username + roles + effective `modules` (ADMIN wildcard
  expands to all 41).

---

## 3. Feature → API → DB object map (per screen)

Format: **Screen/route → what it does → key endpoint(s) → DB objects →
permission module → main test angles.**

### Dashboard
- `/` — landing; nav cards filtered by the user's modules.
- Endpoints: `GET /api/ping` (health); widgets reuse report endpoints.
- Modules: card visibility gated per module.
- Test: cashier sees far fewer cards than admin; empty data degrades gracefully.

### POS / Checkout
- `/pos` — ring up a sale.
- Endpoints: `GET /api/items` / `GET /api/items/barcode/{code}` (lookup),
  `POST /api/vouchers/validate`, `GET /api/payment-modes`, `POST /api/sales`.
- DB: `pos_inventory` (stock decrement, per-item lock), `pos_sale_header` +
  `pos_sale_item` (insert), `pos_transaction_log` (`STOCK_OUT_SALE` per line),
  `pos_customer` (loyalty), `pos_receivable` (credit sale), `pos_voucher` /
  `pos_voucher_redemption`, `pos_loyalty_ledger`.
- Module: `POS`.
- Request shape: `{modeOfPayment, payment, customerId?, voucherCode?,
  lines:[{itemId, quantity}]}`. Response: `salesNumber` (**`S-YYYY-NNNN`**),
  `grandTotal`, `change`, `discountAll`, `status=COMPLETED`, `customerName`.
- Test: insufficient stock → 400; price taken server-side (client price
  ignored); CASH change; voucher discount + single-use; loyalty earn;
  accounts-receivable mode requires a customer and opens a receivable; sales
  number format. **NOTE (deviation from FreePOS):** checkout does **not** require
  an open shift — shifts are independent cash reconciliation (see below).

### Sales browse / Void / Refund / Receipt
- `/sales` — list, detail, void, partial/full refund, receipt PDF.
- Endpoints: `GET /api/sales`, `GET /api/sales/{salesNumber}`,
  `POST /api/sales/{n}/void`, `POST /api/sales/{n}/refund`,
  `GET /api/sales/returns/{returnNumber}`,
  `GET /api/sales/{n}/receipt` (produces `application/pdf`).
- DB: `pos_sale_header` (status COMPLETED→VOIDED/REFUNDED), `pos_sale_item`,
  `pos_return_item` (refund rows **`R-YYYY-NNNN`**), `pos_inventory` (stock
  restored), `pos_transaction_log`, `pos_customer` (loyalty claw-back),
  `pos_receivable` (cancelled on void), `pos_voucher` (released on void).
- Module: `SALES` for browse/void/refund (read also via `POS`).
- Test: partial refund keeps status COMPLETED and reduces `refundableQuantity`;
  full refund → REFUNDED; void restores stock and reverses receivable + loyalty
  + voucher; refund does **not** reverse voucher/loyalty; receipt returns a valid
  `%PDF`.

### Shift Management (cash reconciliation — independent of POS)
- `/shifts` — open/close console + history.
- Endpoints: `POST /api/shifts/open` `{openingFloat}`, `GET /api/shifts/current`,
  `GET /api/shifts/mine`, `GET /api/shifts` (admin, all), `GET /api/shifts/{n}`,
  `POST /api/shifts/{n}/close` `{countedCash}`.
- DB: `pos_shift` (one OPEN per site+cashier; snapshot at close). Reads
  `pos_sale_header` + `pos_return_item` to compute totals.
- Modules: `SHIFTS` (open/close/own), `SHIFTS_ADMIN` (all-history/force-close).
  `/current` also allowed for `POS`.
- Rules: **2nd open while one OPEN → 409**; closing an already-closed shift →
  400; `expectedCash = openingFloat + Σ CASH sales − Σ cash refunds` (VOIDED
  sales excluded); `cashVariance = countedCash − expectedCash`; `closedBy`
  recorded. Shift numbers **`SH-YYYY-NNNN`**.
- Test: live `/current` preview fields (`cashSalesTotal`, `cashRefundsTotal`,
  `expectedCash`, `saleCount`) update as sales/refunds/voids happen.

### Inventory & Reference data
- `/inventory`, `/categories`, `/locations`, `/units`.
- Endpoints: `GET/POST/PUT/DELETE /api/items`, `POST /api/items/{id}/adjust`,
  `GET /api/items/barcode/{code}`; `/api/categories`, `/api/locations`,
  `/api/uoms`.
- DB: `pos_inventory` (unique `(site_id,item_code)`, partial-unique barcode, FKs
  to category/location), `pos_category`, `pos_location`, `pos_uom`,
  `pos_transaction_log` (`STOCK_ADJUST`).
- Modules: `INVENTORY`; `STOCKTAKE`; `CATEGORIES`/`LOCATIONS`/`UOMS`. Item reads
  are shared with POS/SALES/PO/GR/transfer/report roles (`READ_ANY`).
- Item create body: `{code,name,catId,locId,uom,quantity,sellingPrice,
  costPrice?,warning?,critical?}`; response adds `categoryName`, `locationName`,
  `stockLevel` (OK/WARNING/CRITICAL/…).
- Test: duplicate item code → **409**; unknown category/location → **404**;
  soft delete (`active=N`); stock adjust posts signed delta + reason and rewrites
  `stockLevel`; barcode lookup; site isolation.

### Procurement — Purchase Orders & Goods Receiving
- `/purchase-orders`, `/goods-receipts`, `/admin/po-approvers`.
- Endpoints: `GET/POST /api/purchase-orders`, `/{po}/approve`, `/{po}/cancel`,
  `GET /api/purchase-orders/pending-my-approval`; `GET/POST /api/goods-receipts`
  (`?poNumber=` optional); `GET /api/po-approvers`, `PUT /api/po-approvers/{userId}`.
- DB: `pos_purchase_order` (status DRAFT→APPROVED→PARTIALLY_RECEIVED→
  RECEIVED/CANCELLED), `pos_po_approval`, `pos_po_approver` (creator→approver
  routing), `pos_goods_receipt` (`po_number` nullable = direct receipt),
  `pos_inventory` (qty++ + cost refresh), `pos_transaction_log` (`STOCK_IN_GR`).
- Modules: `PURCHASE_ORDERS`, `GOODS_RECEIPTS` (shared reads), `PO_APPROVERS`.
- Rules: a PO **auto-approves** on create when the creator has no approver
  mapping (`autoApproved=true`, status `APPROVED`); with a mapping it starts
  `DRAFT` and must be approved (ADMIN wildcard may always approve). Receiving
  advances PO status and refreshes item `costPrice`; **over-receive → 400**;
  receiving against a `CANCELLED` PO → 400; direct GR (no PO) uses free-text
  supplier. GR is atomic (stock+cost+audit+PO status one txn).

### Stock Transfers (cross-site) + Policy
- `/stock-transfers`, `/admin/stock-transfer-policy`.
- Endpoints: `GET /api/stock-transfers`, `GET /api/stock-transfers/destinations`,
  `GET /api/stock-transfers/{transferNumber}`, `POST /api/stock-transfers`,
  `POST /api/stock-transfers/{n}/receive`, `POST /api/stock-transfers/{n}/cancel`;
  `GET/POST /api/stock-transfer-policy`, `DELETE /api/stock-transfer-policy/{id}`.
- DB: `pos_stock_transfer` (status IN_TRANSIT→RECEIVED/CANCELLED),
  `pos_stock_transfer_item`, `pos_stock_transfer_policy` (allow-list of
  source→dest site pairs), `pos_inventory` at both sites.
- Modules: `STOCK_TRANSFER`, `STOCK_TRANSFER_POLICY`.
- Rules: ship decrements source stock and sets `IN_TRANSIT`; receive at the
  destination increments dest stock (**auto-creating the item there** from an
  active category+location) and sets `RECEIVED`; cancel (while IN_TRANSIT)
  restores source stock. **Policy:** with zero rules all destinations are
  allowed; adding **any** rule makes the policy **ENFORCED** — the
  `/destinations` list is filtered and shipping to a non-listed destination → 400.

### Accounts Receivable / Accounts Payable
- `/receivables`, `/payables`.
- Endpoints: `GET /api/receivables`, `GET /api/receivables/{id}`,
  `POST /api/receivables/{id}/payments`; `GET /api/payables`,
  `GET /api/payables/{id}`, `POST /api/payables` (manual expense),
  `POST /api/payables/{id}/payments`.
- DB: `pos_receivable` + `pos_receivable_payment`; `pos_payable` +
  `pos_payable_payment`.
- Modules: `RECEIVABLES`, `PAYABLES`.
- AR rules: a checkout on an **accounts-receivable** payment mode
  (`accountsReceivable=true`, `customerRequired=true`) opens a receivable
  (`OPEN`, balance = unpaid amount); collections drive `PARTIAL`→`PAID`;
  over-collecting a PAID receivable → 400; a customer **credit limit** (>0)
  blocks a credit sale that would push outstanding over the limit (→ 400; `0` =
  no limit); voiding the sale sets the receivable `CANCELLED`.
- AP rules: a goods receipt from an **AP-enabled supplier** (`apEnabled=true`,
  `payableDays`) auto-creates a `PURCHASE`-source payable; a non-AP supplier
  creates none; a **manual `EXPENSE` payable** can be created and paid;
  disbursements drive `PARTIAL`→`PAID`.

### Customers & Suppliers
- `/customers`, `/suppliers`.
- Endpoints: `GET/POST/PUT/DELETE /api/customers`, `/api/suppliers`.
- DB: `pos_customer` (`points`, `credit_limit`, `email`), `pos_supplier`
  (unique `(site_id, supplier_code)`, `ap_enabled`, `payable_days`).
- Modules: `CUSTOMERS`, `SUPPLIERS`. Customer reads shared with
  POS/SALES/RECEIVABLES; customer create allowed via `POS` (walk-in). Supplier
  reads shared with PO/GR/PAYABLES.
- Test: search + pagination; soft delete/reactivate; **supplier code duplicate →
  409**; customer email validation; credit-limit persists.

### Vouchers, Loyalty, Payment Modes
- `/admin/vouchers`, `/admin/loyalty-config`, `/admin/loyalty-rewards`,
  `/admin/payment-modes`.
- Endpoints: `POST /api/vouchers/validate` (POS/SALES); admin CRUD
  `/api/admin/vouchers`; `GET/PUT /api/admin/loyalty-config`; CRUD
  `/api/admin/loyalty-rewards`; customer-facing `GET /api/loyalty/rewards`,
  `POST /api/loyalty/redeem-reward`, `GET /api/loyalty/ledger`; `GET
  /api/payment-modes` (POS/SALES) + admin CRUD `/api/admin/payment-modes`.
- DB: `pos_voucher` + `pos_voucher_redemption`; `pos_loyalty_config`,
  `pos_loyalty_reward`, `pos_loyalty_redemption`, `pos_loyalty_ledger`;
  `pos_payment_mode`.
- Modules: `VOUCHERS`, `LOYALTY_CONFIG`, `LOYALTY_REWARDS`, `PAYMENT_MODES`;
  loyalty customer endpoints gated by `CUSTOMERS`.
- Voucher rules: `PERCENT`/`FIXED`, `usageLimit`; `validate` returns
  `{valid, discountAmount}`; applied at checkout (`discountAll`, reduced
  `grandTotal`); single-use consumed on the sale; re-use when exhausted → 400;
  released on void (validates again).
- Loyalty rules: config `pointsRate` (% earn) + `redeemValue`; a sale with a
  customer earns points (`EARN` ledger row); rewards (`FREETEXT`/`ITEM`) redeem
  points (`newBalance`); voiding the earning sale claws points back to 0.
- Payment-mode rules: seeded CASH/GCASH/PAYMAYA/CARD; admin can add modes with
  `surchargeType` NONE/PERCENT/FIXED, `isCash`, `allowsPartial`,
  `customerRequired`, `accountsReceivable`, `arDueDays`.

### Reports + Exports
- `/reports`.
- Endpoints (all under `/api/reports`, each `?format=pdf|xlsx` optional):
  `sales-overview` (`?from&to`), `inventory-snapshot`, `inventory-valuation`,
  `receivables`, `payables`, `purchase-orders`, `goods-receipts`,
  `stock-transfers`.
- DB: **read-only** across the business tables above.
- Modules (one per report): `SALES_REPORTS`, `INVENTORY_SNAPSHOT`,
  `INVENTORY_VALUATION`, `RECEIVABLES_REPORT`, `PAYABLES_REPORT`,
  `PURCHASE_ORDERS_REPORT`, `GOODS_RECEIPTS_REPORT`, `STOCK_TRANSFER_REPORT`.
  (Catalog also defines `INVENTORY_MOVEMENT`, `SHIFT_HISTORY_REPORT`.)
- Rules: `sales-overview` JSON has `salesCount`, `netSales`, `byMode[]` and
  **excludes VOIDED**; `xlsx` bytes start with `PK` (zip); `pdf` bytes start with
  `%PDF`; PDFs carry the doc-settings branding letterhead.

### Inventory Import
- Under `/inventory` (import panel).
- Endpoint: `POST /api/items/import` (multipart xlsx). Module: `INVENTORY_IMPORT`.
- Rules: response `{created, errors:[…]}`. **Per-row, not all-or-nothing** — valid
  rows are created and invalid rows (e.g. unknown category) are reported as
  errors while the valid ones still persist. (Deviation from FreePOS's
  all-or-nothing import.)

### Document Settings + Receipts
- `/admin/doc-settings`.
- Endpoints: `GET/PUT /api/admin/doc-settings`. Module: `DOC_SETTINGS`.
- DB: `pos_doc_settings` (business name/address/contact/TIN/footer/accent color +
  receipt title/footer). `usingDefault=true` until first save.
- Rules: branding is injected into report PDFs (letterhead/accent) and into the
  sale receipt PDF (`GET /api/sales/{n}/receipt`).

### Administration (users / roles / sites)
- `/admin/users`, `/admin/roles`, `/admin/sites`.
- Endpoints: `/api/admin/users` (+`/_meta/roles`, `/_meta/sites`,
  `/{id}/reset-password`), `/api/admin/roles` (+`/_meta/modules`),
  `/api/admin/sites`.
- DB: `pos_user`, `pos_user_role`, `pos_user_site`, `pos_role`,
  `pos_role_module`, `pos_module`, `pos_site`, `pos_refresh_token`.
- Modules: `USERS`, `SITES`, `ROLES`.
- Rules: `/roles/_meta/modules` returns **41** modules; **duplicate site code →
  409**; user create returns roles+sites; username immutable; admin reset forces
  change + revokes sessions; deactivating a user revokes its refresh tokens.

### Audit Log & Error Log
- `/admin/audit-log`, `/admin/error-log`. Both gated by module **`AUDIT_LOG`**.
- Endpoints: `GET /api/admin/audit-log` (`?entity&action&format=xlsx`),
  `GET /api/admin/audit-log/{id}`; `GET /api/admin/error-logs` (`?limit`),
  `GET /api/admin/error-logs/{id}`.
- DB: `pos_audit_log`, `pos_error_log`.
- Audit rules: a universal Hibernate post-insert/update/delete listener writes
  `pos_audit_log` with the request's JWT `username`, entity name, a readable
  `entityId` (e.g. `itemId=…`), and action CREATE/UPDATE/DELETE; a **skip-list**
  (audit log, transaction log, loyalty ledger, refresh/reset tokens) is not
  audited; sensitive fields are redacted; exports to xlsx.
- Error rules: an unhandled 500 is recorded to `pos_error_log` in a
  `REQUIRES_NEW` transaction (so it survives the failed request rollback) with a
  reference id, HTTP method/path, user/site, exception class, and a **redacted**
  message (secrets like `password=`, `token:`, `secret=` masked to `***`).

---

## 4. Database object catalog (summary)

Schema **`BUILT4U`**, prefix `pos_`. As of V12:

- **Core / auth:** `pos_user`, `pos_role`, `pos_user_role`, `pos_role_module`,
  `pos_module`, `pos_site`, `pos_user_site`, `pos_refresh_token`,
  `pos_password_reset_token` (table exists; no endpoint uses it yet).
- **Reference / inventory:** `pos_category`, `pos_location`, `pos_uom`,
  `pos_inventory`, `pos_transaction_log`.
- **Sales / shifts:** `pos_sale_header`, `pos_sale_item`, `pos_return_item`,
  `pos_shift`, `pos_payment_mode`.
- **Customers / suppliers:** `pos_customer`, `pos_supplier`.
- **Procurement:** `pos_purchase_order`, `pos_po_approval`, `pos_po_approver`,
  `pos_goods_receipt`.
- **AR / AP:** `pos_receivable`, `pos_receivable_payment`, `pos_payable`,
  `pos_payable_payment`.
- **Stock transfers:** `pos_stock_transfer`, `pos_stock_transfer_item`,
  `pos_stock_transfer_policy`.
- **Vouchers / loyalty:** `pos_voucher`, `pos_voucher_redemption`,
  `pos_loyalty_config`, `pos_loyalty_reward`, `pos_loyalty_redemption`,
  `pos_loyalty_ledger`.
- **Ops:** `pos_audit_log`, `pos_error_log`, `pos_doc_settings`.
- **Document numbers (generated in Java):** sales `S-YYYY-NNNN`, returns
  `R-YYYY-NNNN`, shifts `SH-YYYY-NNNN`, PO/GR/transfer numbers per their services.
- **`pos_transaction_log.transaction_type` discriminators:** `STOCK_IN_GR`,
  `STOCK_OUT_SALE`, `STOCK_ADJUST`, `STOCK_IN_VOID`, `STOCK_IN_REFUND`,
  stock-transfer out/in.

---

## 5. Suggested test-case dimensions (apply per feature)

1. **Happy path** — valid input, expected DB row(s), correct response + status.
2. **Validation** — required/format/length/cross-field (Bean Validation → 400);
   boundary values; money rounding (NUMBER(*,2)).
3. **AuthN** — no token / expired token / refresh path / forced change-password.
4. **AuthZ** — role WITHOUT the module → 403 (and nav/route hidden); role WITH it
   → allowed. Cover ADMIN (wildcard), MANAGER, CASHIER, and a custom role.
5. **Site scoping** — data created under site A is invisible/unusable under site
   B; cross-site by-ID access is not-found.
6. **Conflict/state** — duplicates → 409; illegal state transitions (void an
   already-refunded sale, receive > remaining, 2nd open shift, over-collect a
   PAID receivable).
7. **Concurrency** — two simultaneous checkouts on the same low-stock item
   (per-item locking; no negative stock).
8. **Money/precision** — totals, change, variance, discounts, loyalty points,
   AR/AP balances.
9. **Unexpected error** — force a 500 → generic body + reference id; row in
   `pos_error_log` with redacted message.
10. **Audit** — a business create/update/delete lands in `pos_audit_log` with the
    acting user; skip-listed entities do not.

*Backlog (not yet implemented): email delivery, forgot/reset-password, inventory
movement report UI, shift-history report UI. Track these in `TEST_CASES.md` §
Backlog only.*
