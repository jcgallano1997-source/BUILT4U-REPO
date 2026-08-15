# Built4U POS (single-business) — System Test Cases

**Source references:** `TECHNICAL_TEST_REFERENCE.md`, `PROJECT_STATUS.md`, and
the integration tests under `backend/src/test/java/com/built4u/pos/**`.
**Scope:** manual, API, and end-to-end system tests for the implemented Built4U
POS modules through Flyway **V12**.
**Out of scope (see § Backlog):** email delivery, forgot/reset-password,
inventory-movement report UI, shift-history report UI — scoped but not built.
**Default environments:** backend `http://localhost:8083`; frontend
`http://localhost:5173`; API base `/api`.

## Test Execution Notes

- Use a scratch dataset (create your own categories/locations/UOMs/items per run
  with unique codes) or record the IDs and codes created, since this runs against
  the shared **BUILT4U** Oracle schema. **Never touch the live FREEPOS schema.**
- Verify both UI behavior and API behavior when a case mentions RBAC, validation,
  persistence, or site scoping.
- For DB verification, check the relevant `pos_*` rows named in
  `TECHNICAL_TEST_REFERENCE.md`.
- For export cases, confirm the binary signature (`xlsx` starts with `PK`, `pdf`
  starts with `%PDF`) and compare on-screen rows with the export for the same
  filters.
- Unless a case says otherwise, execute as the seeded admin on site **MAIN**.
- Many of these are already covered by automated integration tests (MOCK MvC,
  `@Transactional` rollback); the **Auto** column names the IT that exercises the
  same behavior. Manual execution confirms the UI + real server on :8083/:5173.
- Track execution in **Latest Test Run**; update `Status`/`Notes` as you go.

## Latest Test Run

| Field | Value |
|---|---|
| **Run date** | 2026-07-27 |
| **Method** | Automated via the browser MCP — API assertions executed from the frontend page context (through the Vite `/api` proxy) against the live server. |
| **Environment** | Frontend `http://localhost:5173` · Backend `http://localhost:8083` · API `/api` · Schema `BUILT4U` |
| **Primary credentials** | `admin` @ **MAIN** (seed `admin123` had already been rotated to the operator's chosen password on first-login) |
| **Seeded roles** | `ADMIN` (wildcard), `MANAGER`, `CASHIER` |
| **Seeded payment modes (MAIN)** | CASH, GCASH, PAYMAYA, CARD (confirmed) |
| **Module count** | 41 (confirmed via `/me` and `/roles/_meta/modules`) |
| **Test data** | Created live with `QA…`-tagged codes and left in the schema (per decision). A throwaway CASHIER user (`qacash…`) and two throwaway branches (`QBRA…`, `QBRB…`) were created; the temporary stock-transfer **policy rule was deleted** after the test so transfers stay unrestricted. |

### Run summary — 59/59 executed assertions passed

| Area | Cases exercised | Result |
|---|---|---|
| Auth + read-only (Stage 1) | GEN-001, AUTH-005, AUTH-004, GEN-003, ROLE-001, USER-007, POS-009, INV read, GEN-008, SITE-002, RPT-001, AUD read | ✅ 10/10 |
| POS core (Stage 2) | INV-002, INV-003, SHIFT-002, POS-001, POS-003, SALE-002, SALE-005, SHIFT-004 | ✅ 10/10 |
| Procurement + exports + audit (3a) | PO-001, GR-001, GR-002, GR-003, PO-004, RPT-003 (xlsx `PK`), RPT-005 (pdf `%PDF`), AUD-001, AUD-002, AUD-003 | ✅ 11/11 |
| Accounts receivable / payable (3b) | AR-001…AR-005, AP-001, AP-002, non-AP no-payable | ✅ 9/9 |
| Vouchers + loyalty (3c) | VCH-002, VCH-004, VCH-005, LOY-002, LOY-003, LOY-004, LOY-006 | ✅ 8/8 |
| Stock transfers + policy (3d) | SITE-001, XFER-001, XFER-002, XFER-003, XFER-005 (+ policy cleanup) | ✅ 6/6 |
| RBAC (3e) | USER-001, GEN-002 (users + inventory), GEN-002c, RPT-008 | ✅ 5/5 |

**Notes / observed behavior**
- POST *create* endpoints return **`201 Created`** (sales, receivables, users,
  sites, etc.) — correct REST semantics. (Early harness assertions that expected
  `200` were false negatives and were re-confirmed as passes.)
- Handled errors return the standard envelope `{timestamp,status,error,message}`
  (verified on 404 and 409).
- `SH-2026-0001` was already **open** from prior manual use, so `SHIFT-002`
  (2nd-open → 409) was validated instead of a fresh open; the fresh-open
  `SHIFT-001` and the `SHIFT-005` close-with-variance path were **not** re-run so
  the operator's open shift wasn't disturbed (both remain covered by `PosFlowIT`).

### Not executed via MCP this run (reasons)

| Case(s) | Why skipped | Coverage |
|---|---|---|
| AUTH-012 (per-IP rate limit) | Would trip the 429 limiter and lock out the operator's own localhost logins for the window. | `RateLimitFlowIT` |
| IMP-001…004 (inventory import) | Building a valid `.xlsx` in browser JS is impractical without a library. | `ReportFlowIT` |
| POS-010, GR-004 atomicity, XFER load (concurrency) | Needs a real server + parallel clients. | — (manual) |
| SALE-003 full-refund→REFUNDED, SALE-006/007 refund vs void nuances | Not scripted this pass. | `VoucherLoyaltyFlowIT` / `ArApFlowIT` partial |
| Email delivery, forgot/reset password | Deferred — not built. | Backlog |

## Priority Legend

| Priority | Meaning |
|---|---|
| P0 | Critical path, financial correctness, security, or data integrity |
| P1 | Core feature behavior and common operational flows |
| P2 | Secondary, edge, or presentation behavior |

## Common Test Data

| Data | Purpose | How to get it |
|---|---|---|
| ADMIN user (wildcard) | Full access + configuration | `admin` / `admin123` @ MAIN |
| MANAGER user | Operational access, no admin/config | Create via `/admin/users` with role `MANAGER` |
| CASHIER user | POS + own transactions; restricted RBAC | Create via `/admin/users` with role `CASHIER` |
| Custom role missing one target module | Negative RBAC tests | Create via `/admin/roles` (e.g. `VIEWER` = `SALES_REPORTS` only) |
| Second site (Site B) | Cross-site scoping / stock transfers | Create `BR2` via `/admin/sites`, grant to the test user |
| Active customer with credit limit + points | POS, AR, loyalty, voucher tests | Create via `/customers` (`creditLimit`, `points`) |
| AP-enabled supplier | AP payable auto-creation | Create via `/suppliers` (`apEnabled=true`, `payableDays`) |
| AR payment mode | Credit-sale tests | Create via `/admin/payment-modes` (`accountsReceivable=true`, `customerRequired=true`) |
| Item with category/location/UOM + stock | Inventory/POS/reports/procurement | Create category → location → uom → item |
| Low-stock / out-of-stock item | Stock-level + insufficient-stock tests | Set `quantity` at/below `warning`/`critical`, or 0 |

---

## A. Cross-Cutting: Security, Session, Errors

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| GEN-001 | P0 | Unauthenticated protected API rejected | Call `GET /api/items` with no access token. | 401; no business data. | |
| GEN-002 | P0 | Role without required module → 403 | Sign in as a role missing `MOD_INVENTORY`; call `POST /api/items`. | 403; the nav/route is hidden in the UI. | |
| GEN-003 | P0 | ADMIN wildcard grants every module | Sign in as ADMIN; `GET /api/auth/me`. | `modules` contains all 41 codes (e.g. `POS`, `AUDIT_LOG`, `STOCK_TRANSFER`). | AuthFlowIT |
| GEN-004 | P0 | Site scoping isolates reads | Create an item under site A; log into site B and list/search items. | Site B does not see site A's item; lists are scoped to the active site. | StockTransferFlowIT (two sites) |
| GEN-005 | P0 | Cross-site by-ID access is not-found | Under site B, `GET /api/items/{id}` for a site A item id. | 404 / not-found; no site A data leaks. | |
| GEN-006 | P0 | Access-token refresh is single-flight | With an expired access token but valid refresh, trigger several concurrent UI calls. | One refresh; failed calls retry; user stays signed in. | |
| GEN-007 | P0 | Invalid refresh forces re-login | Revoke/replace the refresh token, then call an API with an expired access token. | Session cleared; UI redirects to `/login`. | AuthFlowIT (rotation) |
| GEN-008 | P1 | Handled errors use the standard envelope | Trigger a 400 (bad body), 404 (missing id), 409 (duplicate). | Body has `timestamp`, `status`, `error`, `message`. | InventoryFlowIT (404/409) |
| GEN-009 | P1 | Unexpected 500 is generic + referenced + logged | Force an unhandled server error in a test setup. | Response is generic with a reference id; a row lands in `pos_error_log`. | ErrorLogFlowIT |
| GEN-010 | P1 | Forced change-password gates the app | Log in as the seeded admin (must-change) or a user whose password expired. | Routed to `/change-password`; other routes inaccessible until changed. | |
| GEN-011 | P1 | Error-log message redaction | Record an error whose message contains `password=…`/`token:…`/`secret=…`. | Stored/returned message masks the secret to `***`. | ErrorLogFlowIT |

## B. Auth

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| AUTH-001 | P0 | Username site lookup auto-selects a single site | On `/login`, enter a username linked to exactly one active site. | Site picker loads and selects it. | |
| AUTH-002 | P1 | Username site lookup requires a choice for multi-site | Enter a username linked to ≥2 active sites. | Must pick a site before submit. | StockTransferFlowIT (admin @ MAIN+BR2) |
| AUTH-003 | P1 | Site lookup hides inactive/unlinked sites | Enter a username whose linked sites include an inactive one. | Only active linked sites returned; empty ⇒ clear no-site state. | |
| AUTH-004 | P0 | Valid login issues tokens + claims | Submit valid `{username,password,siteCode}`. | 200 with `accessToken` + `refreshToken`; `user.roles`, `site.code`; routes to `/` or `/change-password`. | AuthFlowIT |
| AUTH-005 | P0 | Wrong password is generic + counts | Submit an invalid password. | 401 generic message; `failed_attempts` increments. | AuthFlowIT |
| AUTH-006 | P0 | Lockout after 5 failed attempts | 5 wrong passwords, then the correct one within 15 min. | Blocked until the 15-min lock expires even with the right password. | |
| AUTH-007 | P0 | Inactive user cannot log in | Deactivate a user; submit valid credentials. | 401; no token. | |
| AUTH-008 | P0 | Wrong/missing/inactive/unlinked site rejected | Submit login with a missing, inactive, or non-assigned site. | Rejected; no refresh-token row created. | |
| AUTH-009 | P0 | Refresh rotates; old token single-use | Log in; `POST /api/auth/refresh`; refresh again with the original token. | First succeeds with a new token; reusing the original fails. | AuthFlowIT (first half) |
| AUTH-010 | P1 | Logout revokes refresh token | `POST /api/auth/logout`; then refresh with the logged-out token. | Logout 204; refresh fails. | |
| AUTH-011 | P0 | Change password validates and revokes sessions | Submit wrong current pw, too-weak new pw, same-as-old, then a valid change. | Invalid attempts fail; valid change 2xx, clears must-change, revokes existing refresh tokens. | |
| AUTH-012 | P0 | Per-IP login rate limit → 429 | Exceed `login-rate.max-attempts` logins from one IP within the window. | The over-limit attempt returns 429 with `Retry-After`. | RateLimitFlowIT |
| AUTH-013 | P1 | `/me` reflects live roles/modules | After a role change + token refresh, `GET /api/auth/me`. | `modules` reflects the new grants. | AuthFlowIT |

## C. Users, Roles, Sites

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| USER-001 | P1 | Create active user with role + site | `POST /api/admin/users` with username, initial password, ≥1 role, ≥1 site. | Created; appears in list; can log in; username immutable. | AdminFlowIT |
| USER-002 | P1 | Duplicate username rejected | Create a user with an existing username. | 409; no duplicate. | |
| USER-003 | P0 | Update requires ≥1 role and ≥1 site | Edit a user removing all roles or all sites. | 400; assignments unchanged. | |
| USER-004 | P0 | Deactivating a user revokes its sessions | Log in as user X; deactivate X as admin; X refreshes/calls the API. | X's refresh tokens revoked; X cannot authenticate while inactive. | |
| USER-005 | P0 | Last active admin guard | Try to deactivate / strip the wildcard role from the only active ADMIN. | 400; at least one active wildcard admin remains. | |
| USER-006 | P1 | Admin reset forces change + revokes sessions | `POST /api/admin/users/{id}/reset-password`. | Sessions revoked; next login forces `/change-password`. | |
| USER-007 | P1 | User pickers load | `GET /api/admin/users/_meta/roles` and `/_meta/sites`. | Roles list includes ADMIN/MANAGER/CASHIER (+ customs); sites include MAIN. | AdminFlowIT |
| ROLE-001 | P1 | Module catalog loads (41) | `GET /api/admin/roles/_meta/modules`. | Returns all **41** module codes. | AdminFlowIT |
| ROLE-002 | P1 | Create a custom role with modules | `POST /api/admin/roles` (e.g. `VIEWER` = `SALES_REPORTS`). | Created, `builtIn=false`, `moduleCodes` persisted; appears in user role picker. | AdminFlowIT |
| ROLE-003 | P1 | Role code validation | Create a role with lowercase/special chars, and one with reserved code `ADMIN`. | Invalid/reserved codes rejected. | |
| ROLE-004 | P0 | Built-in ADMIN role is immutable | Try to edit modules on / delete the ADMIN role. | Rejected; ADMIN stays wildcard. | |
| ROLE-005 | P1 | Assigned custom role cannot be deleted | Assign a custom role to a user, then delete the role. | 409. | |
| SITE-001 | P1 | Create site (uppercase immutable code) | `POST /api/admin/sites` with a lowercase code. | Code uppercased; appears in pickers; code not editable later. | StockTransferFlowIT (createSite) |
| SITE-002 | P1 | Duplicate site code rejected | Create a site with an existing code (e.g. `MAIN`). | 409. | AdminFlowIT |
| SITE-003 | P0 | Last active site cannot be deactivated | Deactivate the only active site. | 400; site stays active. | |

## D. Dashboard & Navigation

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| DASH-001 | P1 | Dashboard loads for an authenticated user | Sign in; open `/`. | Renders without errors; `GET /api/ping` OK. | |
| DASH-002 | P1 | Nav filtered by modules | Sign in as ADMIN, MANAGER, CASHIER, and a limited custom role. | Each sees only routes their modules allow. | |
| DASH-003 | P2 | Empty-data dashboard is graceful | Use a fresh site with no sales/inventory. | Widgets show zero/empty states, no crash. | |

## E. Inventory & Reference Data

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| INV-001 | P1 | Item list search/filter/paging | Create items across categories; search `/api/items?search=`. | Results match; paging correct; inactive shown only when requested. | ReportFlowIT/StockTransferFlowIT (search) |
| INV-002 | P1 | Item create joins references + computes level | Create an item with active cat/loc/uom, qty above warning. | Created; response has `categoryName`, `locationName`, `stockLevel=OK`. | InventoryFlowIT |
| INV-003 | P0 | Duplicate item code per site → 409 | Create two items with the same code in one site. | Second → 409. | InventoryFlowIT |
| INV-004 | P0 | Duplicate non-blank barcode per site → 409 | Create two items with the same barcode in one site. | 409; blank/null barcodes allowed. | |
| INV-005 | P1 | Threshold cross-field validation | Create/update with `critical` > `warning`. | Rejected (400) in UI + API. | |
| INV-006 | P1 | Unknown/inactive references rejected | Create an item with a non-existent category/location/uom. | 404 / validation failure; not created. | InventoryFlowIT (unknown cat → 404) |
| INV-007 | P1 | Item delete is soft delete | Delete an item. | `active=N`; row remains; hidden from active lists. | |
| INV-008 | P0 | Stock adjust rejects invalid deltas | Submit delta 0; submit a negative delta larger than current qty. | Both rejected; qty + logs unchanged. | |
| INV-009 | P0 | Stock adjust writes signed delta + audit | Adjust with a reason (e.g. −9). | Qty changes; `stockLevel` recomputed (e.g. CRITICAL); `pos_transaction_log` `STOCK_ADJUST` row. | InventoryFlowIT |
| INV-010 | P1 | Barcode lookup returns active only | Look up an active vs inactive item by barcode. | Active returned; inactive not usable. | |
| INV-011 | P1 | Stock-level badges match thresholds | Set qty for out/critical/warning/OK. | Badge matches computed level. | InventoryFlowIT (OK→CRITICAL) |
| INV-012 | P1 | Category CRUD + uniqueness + soft delete | Create, duplicate, update, delete a category. | Duplicate → 409; delete deactivates. | |
| INV-013 | P1 | Location CRUD + capacity + soft delete | Create, duplicate, update capacity, delete a location. | Duplicate → 409; capacity persists; delete deactivates. | InventoryFlowIT (create w/ capacity) |
| INV-014 | P1 | UOM natural-key create/deactivate | Create a UOM, deactivate/reactivate; attempt rename. | Active flag toggles; rename not supported. | |
| IMP-001 | P1 | Inventory import happy path | `POST /api/items/import` with an xlsx of valid rows. | `created` = number of valid rows; items searchable with correct qty. | ReportFlowIT |
| IMP-002 | P1 | Import is per-row (not all-or-nothing) | Upload a sheet with valid rows and one invalid row (unknown category). | Valid rows created; the bad row appears in `errors[]`; the run is **not** rejected wholesale. | ReportFlowIT |
| IMP-003 | P1 | Import auto-uses existing references | Upload rows referencing existing category/location/uom by name. | Rows resolve to those references. | ReportFlowIT |
| IMP-004 | P1 | Oversized upload rejected | Upload a file over the multipart limit. | 413 standard envelope. | |

## F. Shifts, POS, Sales, Refunds

> **Deviation from FreePOS:** POS checkout is **not** blocked by the absence of an
> open shift. Shifts are an independent cash-reconciliation feature; sales rung
> while a shift is open feed that shift's expected cash.

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| SHIFT-001 | P0 | Open a shift | `POST /api/shifts/open` `{openingFloat}`. | Shift `OPEN`, number `SH-YYYY-NNNN`; `GET /api/shifts/current` returns it. | PosFlowIT |
| SHIFT-002 | P0 | One open shift per cashier/site | Open a shift, then open again. | Second → 409. | (ShiftService rule) |
| SHIFT-003 | P1 | Negative opening float rejected | Open with `openingFloat` < 0. | 400. | |
| SHIFT-004 | P0 | Live current-shift totals | Ring cash sales + a refund; void one sale; `GET /api/shifts/current`. | `cashSalesTotal`/`cashRefundsTotal`/`expectedCash`/`saleCount` exclude the voided sale. | PosFlowIT |
| SHIFT-005 | P0 | Close computes expected cash + variance | Close with `countedCash`. | `expectedCash = openingFloat + cashSales − cashRefunds`; `cashVariance = counted − expected`; `closedBy` recorded; status `CLOSED`. | PosFlowIT |
| SHIFT-006 | P1 | Re-close a closed shift rejected | Close an already-closed shift. | 400. | |
| SHIFT-007 | P1 | Shift history is admin-gated | `GET /api/shifts` as a non-`SHIFTS_ADMIN` user. | 403; ADMIN/`SHIFTS_ADMIN` sees all. | |
| POS-001 | P0 | Checkout happy path decrements stock | Add item, CASH, complete checkout. | `salesNumber` `S-YYYY-NNNN`; header+items inserted; stock down; `STOCK_OUT_SALE` logged; status COMPLETED. | PosFlowIT |
| POS-002 | P0 | Server price overrides client price | Submit a checkout with a tampered lower line price. | Sale uses the server-side selling price. | |
| POS-003 | P0 | Insufficient stock blocks checkout | Order qty > available. | 400; no sale; stock unchanged. | PosFlowIT |
| POS-004 | P0 | CASH change computed | CASH with `payment` above total. | `change = payment − grandTotal`; response `change` correct. | PosFlowIT |
| POS-005 | P1 | Customer attached to sale | Checkout with `customerId`. | Sale `customerName` set; visible in sales list. | CustomerSupplierPaymentIT |
| POS-006 | P0 | AR mode requires a customer | Checkout on an `accountsReceivable` mode with no customer. | Rejected (400); with a customer it opens a receivable. | ArApFlowIT |
| POS-007 | P1 | Voucher applied at checkout | Apply a valid voucher code. | `grandTotal` reduced by `discountAll`; voucher consumed. | VoucherLoyaltyFlowIT |
| POS-008 | P1 | Loyalty earned at checkout | With loyalty config set and a customer, complete a sale. | Customer points increase; `EARN` ledger row. | VoucherLoyaltyFlowIT |
| POS-009 | P1 | Payment modes list active only | `GET /api/payment-modes`. | Seeded CASH/GCASH/PAYMAYA/CARD (+ active customs); inactive excluded. | CustomerSupplierPaymentIT |
| POS-010 | P0 | Concurrent checkout on low stock | Two simultaneous checkouts for the same scarce item. | Only available stock sells; no negative stock; one fails or waits safely. | |
| SALE-001 | P1 | Sales list + detail | Create sales; `GET /api/sales`; open a detail. | Matching rows; detail shows lines/payments/voucher/loyalty and `refundableQuantity`. | PosFlowIT/CustomerSupplierPaymentIT |
| SALE-002 | P0 | Partial refund restores stock, keeps COMPLETED | Refund part of a line. | `totalRefunded` correct; stock up by refunded qty; status stays COMPLETED; `refundableQuantity` drops; `STOCK_IN_REFUND` log. | PosFlowIT |
| SALE-003 | P0 | Full refund → REFUNDED (`R-YYYY-NNNN`) | Refund all remaining qty on all lines. | Status REFUNDED; return number `R-YYYY-NNNN`. | |
| SALE-004 | P0 | Refund cannot exceed refundable qty | Refund more than remaining. | Rejected. | |
| SALE-005 | P0 | Void restores stock → VOIDED | Void a COMPLETED sale with no prior refund. | Status VOIDED; stock restored; `STOCK_IN_VOID` log. | PosFlowIT |
| SALE-006 | P0 | Void reverses receivable + loyalty + voucher | Void a credit/loyalty/voucher sale. | Receivable CANCELLED; earned points clawed back; voucher released. | ArApFlowIT / VoucherLoyaltyFlowIT |
| SALE-007 | P1 | Refund does NOT reverse voucher/loyalty | Refund (not void) a voucher/loyalty sale. | Voucher usage + loyalty stay applied. | |
| SALE-008 | P0 | Illegal state transitions blocked | Void an already-VOIDED/REFUNDED sale; refund a VOIDED sale. | Rejected. | |
| SALE-009 | P1 | Receipt PDF | `GET /api/sales/{n}/receipt`. | Returns `application/pdf` (`%PDF…`), non-trivial size. | DocSettingsReceiptFlowIT |

## G. Procurement — Purchase Orders & Goods Receiving

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| PO-001 | P1 | PO auto-approves without an approver mapping | Create a PO as a user with no approver mapping. | Status `APPROVED`, `autoApproved=true`; `grandTotal` + line `remainingQty` correct. | ProcurementFlowIT |
| PO-002 | P0 | Approver routing makes a DRAFT | Map creator → approver; create a PO. | Status `DRAFT`, `autoApproved=false`; appears in `pending-my-approval`. | ProcurementFlowIT |
| PO-003 | P1 | Approve a DRAFT | `POST /api/purchase-orders/{po}/approve`. | Status `APPROVED`; `approvedBy` = actor. | ProcurementFlowIT |
| PO-004 | P1 | Cancel a PO blocks receiving | Cancel an approved PO, then receive against it. | Status `CANCELLED`; receiving → 400. | ProcurementFlowIT |
| GR-001 | P0 | Receive advances PO status + stock + cost | Receive part, then the remainder. | Stock rises each time; PO `PARTIALLY_RECEIVED` → `RECEIVED`; item `costPrice` refreshes to received price. | ProcurementFlowIT |
| GR-002 | P0 | Over-receive rejected | Receive more than remaining (or against a RECEIVED PO). | 400. | ProcurementFlowIT |
| GR-003 | P1 | Direct GR without a PO | `POST /api/goods-receipts` with free-text supplier + `unitCost`, no `poNumber`. | GR created; stock rises; `po_number` null. | ProcurementFlowIT |
| GR-004 | P0 | GR is atomic | Submit a GR with one valid + one invalid/over line. | Whole GR fails; no stock/cost/PO changes committed. | |
| GR-005 | P1 | AP payable auto-created from AP supplier | Receive a PO from an `apEnabled` supplier. | A `PURCHASE`-source payable opens for the received value. | ArApFlowIT |
| GR-006 | P1 | Non-AP supplier creates no payable | Receive from a supplier with `apEnabled=false`. | No payable created. | ArApFlowIT |

## H. Stock Transfers (cross-site)

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| XFER-001 | P0 | Ship decrements source, sets IN_TRANSIT | From site A, `POST /api/stock-transfers` to site B. | Source stock down by qty; header `IN_TRANSIT`; `transferNumber` returned. | StockTransferFlowIT |
| XFER-002 | P0 | Receive increments dest + auto-creates item | Log into site B; `POST /api/stock-transfers/{n}/receive`. | Status `RECEIVED`; item auto-created at B (from active cat+loc) with the shipped qty. | StockTransferFlowIT |
| XFER-003 | P0 | Cancel restores source stock | Cancel a transfer still IN_TRANSIT at the source. | Status `CANCELLED`; source stock restored. | StockTransferFlowIT |
| XFER-004 | P1 | Receive needs active refs at destination | Ship to a site lacking an active category/location. | Receive fails with a clear error until refs exist. | |
| XFER-005 | P0 | Policy allow-list enforcement | Add one `source→dest` rule; list `/destinations`; ship to a non-listed dest, then a listed one. | Policy becomes ENFORCED; `/destinations` shows only allowed sites; disallowed → 400; allowed → IN_TRANSIT. | StockTransferFlowIT |
| XFER-006 | P2 | No rules = all destinations allowed | With zero policy rows, list `/destinations`. | All other active sites are eligible. | |

## I. Accounts Receivable / Accounts Payable

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| AR-001 | P0 | Credit sale opens a receivable | Checkout on an AR mode, `payment=0`. | Receivable `OPEN`, balance = sale total. | ArApFlowIT |
| AR-002 | P0 | Collections drive PARTIAL → PAID | Post a partial payment then the balance. | `PARTIAL` (balance drops) → `PAID` (balance 0); payments recorded. | ArApFlowIT |
| AR-003 | P0 | Over-collect a PAID receivable rejected | Post another payment after PAID. | 400. | ArApFlowIT |
| AR-004 | P0 | Credit limit blocks oversized credit sale | Customer limit ₱700; one 500 sale (OK); a second 500. | First OK; second → 400 (outstanding would exceed limit). `creditLimit=0` = no limit. | ArApFlowIT |
| AR-005 | P0 | Void cancels the receivable | Void the credit sale. | Receivable → `CANCELLED`. | ArApFlowIT |
| AP-001 | P0 | Disbursements drive PARTIAL → PAID | Pay a purchase payable partially then in full. | `PARTIAL` → `PAID`. | ArApFlowIT |
| AP-002 | P1 | Manual EXPENSE payable create + pay | `POST /api/payables` (category/payee/amount/dueDate), then pay in full. | Payable `EXPENSE` source; → `PAID`. | ArApFlowIT |
| AP-003 | P1 | Payable list filters | `GET /api/payables?source=PURCHASE` / `?search=`. | Returns only matching rows. | ArApFlowIT |

## J. Customers & Suppliers

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| CUST-001 | P1 | Customer create + list/search | Create customers; search `/api/customers`. | Created; searchable; page metadata correct. | CustomerSupplierPaymentIT |
| CUST-002 | P1 | Customer email validation | Create/update with an invalid then valid email. | Invalid rejected; valid persists. | |
| CUST-003 | P1 | Customer soft delete/reactivate | Delete, include inactive, reactivate. | Hidden from active lists; reappears with inactive filter; reactivates. | |
| CUST-004 | P1 | Walk-in customer via POS role | As a role with `POS` (not `CUSTOMERS`), create a customer. | Allowed (POST customers permits `MOD_POS`); pure read roles blocked from write. | |
| SUP-001 | P1 | Supplier CRUD + duplicate code | Create, duplicate the code, update, deactivate. | Duplicate `(site, code)` → 409. | CustomerSupplierPaymentIT |
| SUP-002 | P1 | AP fields persist | Create a supplier with `apEnabled`, `payableDays`. | Values persist and drive AP payable creation. | ArApFlowIT |

## K. Vouchers, Loyalty, Payment Modes

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| VCH-001 | P1 | Voucher CRUD + validation | Create PERCENT and FIXED vouchers; try percent >100, invalid dates, dup code. | Valid persist; invalid → validation/409. | |
| VCH-002 | P1 | Validate returns discount for a subtotal | `POST /api/vouchers/validate` `{code,subtotal}`. | `valid=true`, `discountAmount` computed (e.g. 10% of 100 = 10). | VoucherLoyaltyFlowIT |
| VCH-003 | P0 | Discount never exceeds subtotal | Validate a FIXED voucher larger than the subtotal. | Discount capped at subtotal. | |
| VCH-004 | P0 | Single-use consumed at checkout | Checkout with a `usageLimit=1` voucher, then reuse it. | First applies; reuse → 400 (exhausted). | VoucherLoyaltyFlowIT |
| VCH-005 | P1 | Void releases the voucher | Void the sale that used the voucher. | Voucher validates again (usage released). | VoucherLoyaltyFlowIT |
| LOY-001 | P1 | Loyalty config view/update | `GET/PUT /api/admin/loyalty-config` (`pointsRate`, `redeemValue`). | Values persist; negatives rejected. | VoucherLoyaltyFlowIT |
| LOY-002 | P1 | Earn on sale | Sale of ₱100 at 10% for a customer. | Customer earns 10 points; `EARN` ledger row; `liveBalance` reflects it. | VoucherLoyaltyFlowIT |
| LOY-003 | P0 | Redeem a reward spends points | Redeem a reward costing ≤ balance. | `newBalance = balance − cost`; `REDEEM`/redemption recorded. | VoucherLoyaltyFlowIT |
| LOY-004 | P0 | Redeem rejects insufficient balance | Redeem a reward costing more than the balance. | Rejected; no points/stock change. | |
| LOY-005 | P1 | ITEM reward decrements inventory | Redeem an `ITEM` reward tied to a stocked item. | Item qty −1; fails if item missing / qty < 1. | |
| LOY-006 | P0 | Void claws back earned points | Void the earning sale. | Points return to the pre-sale balance (e.g. 10 → 0). | VoucherLoyaltyFlowIT |
| PAY-001 | P1 | Payment-mode admin CRUD | Add a mode with surcharge/flags via `/api/admin/payment-modes`. | Persists; appears in POS list if active. | CustomerSupplierPaymentIT |
| PAY-002 | P1 | Surcharge validation | Create a mode with invalid percent/fixed surcharge. | Invalid rejected; NONE/PERCENT/FIXED valid. | |
| PAY-003 | P0 | CASH mode protected | Try to deactivate/delete CASH. | Rejected; CASH stays active. | |

## L. Reports & Exports

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| RPT-001 | P0 | Sales overview excludes VOIDED | Create a completed + a voided sale in range; `GET /api/reports/sales-overview?from&to`. | `salesCount`/`netSales`/`byMode[]` count only non-voided sales. | ReportFlowIT |
| RPT-002 | P1 | Sales overview date-range validation | Request with `from` > `to` or an oversized range. | 400 for invalid ranges. | |
| RPT-003 | P1 | Sales report export parity (xlsx/pdf) | Export the same filters as xlsx and pdf. | xlsx starts `PK`; pdf starts `%PDF`; rows/totals match on-screen. | ReportFlowIT |
| RPT-004 | P1 | Inventory snapshot lists stock | `GET /api/reports/inventory-snapshot`. | Rows reflect current stock; filters apply. | ReportFlowIT |
| RPT-005 | P1 | Inventory valuation totals | `GET /api/reports/inventory-valuation` (+ `format=pdf`). | Totals = Σ qty×cost; PDF renders with branding. | ReportFlowIT / DocSettingsReceiptFlowIT |
| RPT-006 | P1 | AR / AP reports | `GET /api/reports/receivables`, `/payables`. | Balances + payment history per record; export works. | |
| RPT-007 | P1 | PO / GR / Stock-transfer reports | `GET /api/reports/purchase-orders`, `/goods-receipts`, `/stock-transfers`. | Header + line detail; export works. | |
| RPT-008 | P0 | Report RBAC is per submodule | Grant a role only one report module (e.g. `SALES_REPORTS`). | Only that report's endpoint/tab is reachable; others → 403. | |

## M. Document Settings & Receipts

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| DOC-001 | P1 | Save + read back branding | `PUT /api/admin/doc-settings` (name/address/TIN/accent/receipt title+footer), then `GET`. | Persists; `usingDefault=false`; re-read matches. | DocSettingsReceiptFlowIT |
| DOC-002 | P1 | Branding applied to report PDFs | Export a report PDF after saving branding. | PDF renders with the letterhead/accent. | DocSettingsReceiptFlowIT |
| DOC-003 | P1 | Branding applied to receipt PDF | Fetch a sale receipt PDF after saving branding. | Receipt renders (`%PDF`), reflecting the receipt title/footer. | DocSettingsReceiptFlowIT |
| DOC-004 | P2 | Defaults before first save | `GET /api/admin/doc-settings` on a clean schema. | `usingDefault=true`; sensible fallback values. | |

## N. Audit Log & Error Log

| ID | Priority | Scenario | Steps | Expected Result | Auto |
|---|---|---|---|---|---|
| AUD-001 | P0 | Business write is audited with the actor | Create an item; `GET /api/admin/audit-log?entity=item&action=CREATE`. | A row with `entityName=Item`, `action=CREATE`, `username=admin`, readable `entityId` (e.g. `itemId=…`). | AuditLogFlowIT |
| AUD-002 | P1 | Sale audited, transaction log skip-listed | Ring a sale; query `?entity=sale` then `?entity=transactionlog`. | Sale audited (≥1); transaction-log entity count = 0 (skip-list). | AuditLogFlowIT |
| AUD-003 | P1 | Audit export | `GET /api/admin/audit-log?format=xlsx`. | Binary starts `PK`. | AuditLogFlowIT |
| AUD-004 | P1 | Audit log RBAC | Access `/api/admin/audit-log` without `MOD_AUDIT_LOG`. | 403. | |
| ERR-001 | P0 | Unhandled error recorded + redacted | Trigger/record a server error containing a secret. | Row in `pos_error_log` with ref id, method/path, user/site, exception class, message with secret masked (`***`). | ErrorLogFlowIT |
| ERR-002 | P1 | Error log readable via admin API | `GET /api/admin/error-logs?limit=`. | Returns recorded errors; gated by `MOD_AUDIT_LOG`. | ErrorLogFlowIT |

## O. Release Smoke Pack

| ID | Priority | Scenario | Steps | Expected Result |
|---|---|---|---|---|
| SMK-001 | P0 | Login → dashboard | Start backend/frontend; log in as ADMIN on MAIN (change pw if prompted). | Login succeeds; dashboard renders. |
| SMK-002 | P0 | Shift → POS → receipt | Open a shift; ring one cash sale; download the receipt; close the shift. | All succeed; variance computed. |
| SMK-003 | P0 | Inventory → procurement | Create item + refs; create a PO; receive goods. | Stock rises; PO status advances. |
| SMK-004 | P0 | AR + AP round-trip | Credit sale → collect; AP-supplier GR → pay. | Receivable/payable open and close. |
| SMK-005 | P0 | Stock transfer | Ship MAIN→BR2; receive at BR2. | Stock moves across sites. |
| SMK-006 | P1 | Reports + exports | Open sales overview, valuation, AR/AP; download one xlsx + one pdf. | Render + download with correct MIME. |
| SMK-007 | P0 | RBAC | Sign in as CASHIER + a limited custom role. | Admin/config routes hidden in UI and 403 from API. |
| SMK-008 | P1 | Error envelope | Trigger a 400, 403, 404, 409. | Each follows the standard envelope. |

---

## Traceability Summary

| Area | Covered IDs |
|---|---|
| Cross-cutting security/session/errors | GEN-001 … GEN-011 |
| Auth | AUTH-001 … AUTH-013 |
| Users / roles / sites | USER-001 … USER-007, ROLE-001 … ROLE-005, SITE-001 … SITE-003 |
| Dashboard / navigation | DASH-001 … DASH-003 |
| Inventory / reference / import | INV-001 … INV-014, IMP-001 … IMP-004 |
| Shifts / POS / sales / refunds | SHIFT-001 … SHIFT-007, POS-001 … POS-010, SALE-001 … SALE-009 |
| Procurement | PO-001 … PO-004, GR-001 … GR-006 |
| Stock transfers | XFER-001 … XFER-006 |
| Accounts receivable / payable | AR-001 … AR-005, AP-001 … AP-003 |
| Customers / suppliers | CUST-001 … CUST-004, SUP-001 … SUP-002 |
| Vouchers / loyalty / payment modes | VCH-001 … VCH-005, LOY-001 … LOY-006, PAY-001 … PAY-003 |
| Reports / exports | RPT-001 … RPT-008 |
| Document settings / receipts | DOC-001 … DOC-004 |
| Audit / error logs | AUD-001 … AUD-004, ERR-001 … ERR-002 |
| Release smoke | SMK-001 … SMK-008 |

## Backlog (documented, not yet built — do NOT run as active cases)

| Area | Note |
|---|---|
| Email delivery | No SMTP wired; report/receipt email is deferred. Add `spring.mail.*` + a provider later (see `PROJECT_STATUS.md`). |
| Forgot / reset password | No `forgot-password` / `reset-password` endpoint exists (`pos_password_reset_token` table is present but unused). |
| Inventory movement report | `INVENTORY_MOVEMENT` module exists in the catalog; no report endpoint/UI yet. |
| Shift history report | `SHIFT_HISTORY_REPORT` module exists; no report endpoint/UI yet. |
| Concurrency (POS-010, XFER load) | Needs a real running server + parallel clients; not exercised by the MOCK ITs. |
