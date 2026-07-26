# Built4U POS (single-business) — Project Status & Roadmap

> **Living tracker.** This is the single source of truth for "where are we". Update
> the checkboxes as phases complete. Committed to git so it survives across sessions
> (if the chat/token context is lost, read this first).
>
> Last updated: **2026-07-26** · Current position: **Phase 2 complete → Phase 3 next**

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

### ⬜ Phase 3 — Frontend login slice  *(NEXT)*
- [ ] Vite + React 19 + TS scaffold, Tailwind, router
- [ ] `authStore` (zustand persist), `api.ts` axios + single-flight refresh interceptor
- [ ] Login page → stores JWT → one protected page reading `/api/auth/me`
- [ ] Change-password screen (must-change flow)
- [ ] End-to-end login working locally

### ⬜ Phase 4 — Admin: users / sites / roles
- [ ] Site management (create/edit/activate branches)
- [ ] User management (create users, assign roles + site access)
- [ ] Role management (custom roles + module grants)
- [ ] Backend services/controllers + frontend pages

### ⬜ Phase 5 — Reference data + inventory
- [ ] Migration for categories / locations / uoms / inventory (+ 5 sequences)
- [ ] Item master (selling price, cost price, barcode, thresholds), category/location/uom lookups
- [ ] Inventory bulk import (xlsx)

### ⬜ Phase 6 — Sales + shifts (the POS core)
- [ ] Shifts (open/close reconciliation, one-open-per-cashier)
- [ ] Sale header / line items / returns / surcharge lines
- [ ] Universal transaction (stock-movement) log
- [ ] POS screen + cart (`posCart` store)

### ⬜ Phase 7 — Customers & suppliers
- [ ] Customer + supplier records
- [ ] Payment modes catalog (per-site, `site_id=0`-free — entity-free from the start)

### ⬜ Phase 8 — Procurement
- [ ] Purchase orders (draft→approved→received) + PO approver routing
- [ ] Goods receipts

### ⬜ Phase 9 — Accounts Receivable / Payable
- [ ] AR: customer credit, receivables, payments
- [ ] AP: supplier payable config, payables, payments

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
