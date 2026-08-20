# Built4U POS — Deployment (Render)

Two services deploy from this repo:

| Service | Type | Root | Notes |
|---|---|---|---|
| `built4u-pos-api` | Docker web service | `backend/` | Spring Boot API, `prod` profile |
| `built4u-pos-web` | Static site | `frontend/` | React build served as static files |

The database is the **existing** Oracle ADB `BUILT4U` schema (unchanged from
local). The live `FREEPOS` schema is never touched. Flyway runs **V1–V24** on
first boot against `BUILT4U` (any already applied locally are skipped; new ones
apply automatically on each deploy).

You can deploy via the **Blueprint** (`render.yaml`) or the **dashboard**
(steps below). Either way you provide the same env vars + wallet.

---

## 0. Before you start
- GitHub repo connected to Render (`jcgallano1997-source/BUILT4U-REPO`).
- Your local `backend/wallet/` folder (the ADB wallet — `cwallet.sso`,
  `tnsnames.ora`, `sqlnet.ora`, `ojdbc.properties`, `ewallet.p12`, …). **Never
  commit these** — they're uploaded to Render as Secret Files.
- The BUILT4U schema password, and a strong first-run admin password you choose.

---

## 1. Backend API (`built4u-pos-api`)
1. **New → Web Service → Docker**, repo root `backend/`, Dockerfile `./Dockerfile`.
2. **Secret Files** — upload every file from your local `backend/wallet/` folder.
   Render mounts them at `/etc/secrets/<filename>`.
3. **Environment variables:**

   | Key | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `TNS_ADMIN` | `/etc/secrets` |
   | `DB_URL` | `jdbc:oracle:thin:@built4u_low` |
   | `DB_USERNAME` | `built4u` |
   | `DB_PASSWORD` | *(the BUILT4U schema password)* |
   | `JWT_SECRET` | *(≥ 64 random chars — or let Render generate)* |
   | `APP_CORS_ALLOWED_ORIGINS` | *(the web URL — fill after step 2, e.g. `https://built4u-pos-web.onrender.com`)* |
   | `APP_SECURITY_SEED_ADMIN_PASSWORD` | *(your first-run admin password)* |
   | `APP_SECURITY_HSTS_ENABLED` | `true` |

   > The `prod` profile has **no defaults** for `JWT_SECRET`, `DB_*`,
   > `APP_CORS_ALLOWED_ORIGINS`, or `APP_SECURITY_SEED_ADMIN_PASSWORD` — a missing
   > one fails startup on purpose. The seed admin password must **not** be
   > `admin123` (DataSeeder rejects it under `prod`).
4. **Health check path:** `/actuator/health`. Deploy.

## 2. Frontend static site (`built4u-pos-web`)
1. **New → Static Site**, repo root `frontend/`.
2. **Build command:** `npm ci --legacy-peer-deps && npm run build`
   **Publish directory:** `dist`
3. **Environment variable:** `VITE_API_BASE_URL` = the API URL **+ `/api`**,
   e.g. `https://built4u-pos-api.onrender.com/api`. (This is baked in at build
   time and also whitelisted in the app's `connect-src` CSP.)
4. **Rewrite rule** (SPA routing): `/*` → `/index.html` (Action: Rewrite).
5. Deploy, then copy this site's URL.

## 3. Close the CORS loop
Set the API's `APP_CORS_ALLOWED_ORIGINS` to the web URL from step 2 and
redeploy the API. (Multiple origins: comma-separated, no spaces.)

---

## 4. First run & smoke check
1. `GET https://<api>/actuator/health` → `{"status":"UP"}`.
2. Open the web URL, log in **admin / `<APP_SECURITY_SEED_ADMIN_PASSWORD>` / MAIN**;
   you'll be forced to change the password.
3. Ring one sale in POS; open **Reports** and download a PDF; open a sale
   **Receipt**. If those work, the DB, auth, exports, and branding are all live.
4. Optional deeper check of the post-launch features: split-tender a sale;
   record a mid-shift cash-out and close a shift with a denomination count;
   receive stock twice at different costs and confirm the item's cost is a
   moving average; open **Reports → Reorder suggestions** and **Discounts &
   overrides**. (Their modules — `PRICE_OVERRIDE`, `DISCOUNTS_REPORT`,
   `REORDER_REPORT` — are seeded by Flyway, granted to Owner/Manager by default.)

## Notes
- **Free tier** spins the API down when idle → the first request after a lull is
  slow (cold start). Fine for pilot use; upgrade the API plan to keep it warm.
- **Migrations:** Flyway auto-runs on boot. New migrations ship by pushing to
  the branch Render auto-deploys.
- **Rollback:** Render keeps deploy history — "Rollback" to a previous deploy,
  or push a revert commit.
- **Email:** working. Reports send via **Resend** from the verified domain
  `built4u-pos.com` (SPF/DKIM/DMARC live in Cloudflare DNS). Set
  `APP_MAIL_RESEND_API_KEY`; `APP_MAIL_FROM` defaults to
  `Built4U <reports@built4u-pos.com>`. Recipients are picked per report as
  *users* (Admin -> Report email) and resolved to their address at send time.
- **Receipt printing:** a hosted API has no route to the shop's LAN printer, so run
  `print-agent/` on the till PC (see its README), with `ALLOWED_ORIGIN` set to
  the site's public URL. The frontend probes for the agent and falls back to
  server-side printing when there isn't one.
- **Wallet security:** the wallet lives only in Render Secret Files and your
  local machine — never in git (enforced by `.gitignore` / `.dockerignore`).
