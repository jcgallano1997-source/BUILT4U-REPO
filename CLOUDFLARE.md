# Built4U POS — Deployment (Cloudflare + Oracle Cloud)

Runbook for hosting on your own domain. Follow it top to bottom; each step says
how to check it worked before you move on.

```
pos.built4u-pos.com   Cloudflare Pages   the React app (static)
api.built4u-pos.com   Oracle Cloud VM    the Spring Boot API
                      Oracle ADB         the BUILT4U schema (already yours)
till PC               print agent        receipt printer + cash drawer
```

**Why the backend goes on Oracle Cloud:** it lands in the same region as your
ADB, so a database round-trip drops from ~100 ms to ~1 ms. That is the difference
between a bulk import taking 16 minutes and taking seconds. The Always Free ARM
shape (4 cores / 24 GB) costs nothing and, unlike free tiers elsewhere, never
sleeps — a register cannot wait 50 seconds for a cold start.

**Database:** production uses the existing **BUILT4U** schema — the one holding
your live items. It is already migrated through V27, so the first cloud boot only
applies V28 and V29. Nothing is recreated and no data is touched.

---

## 0. Before you start

- [ ] The repo is pushed to GitHub (Pages deploys from it)
- [ ] Your Oracle wallet folder (`backend/wallet`) is to hand — **never commit it**
- [ ] You know the BUILT4U schema password
- [ ] `built4u-pos.com` is active in Cloudflare

---

## 1. Backend — Oracle Cloud VM

### 1.1 Create the VM
OCI Console → **Compute → Instances → Create**:

- **Image** Ubuntu 22.04 · **Shape** `VM.Standard.A1.Flex` (Ampere, Always Free) — 2 OCPU / 12 GB is plenty
- **Region** — the same one as your Autonomous Database. This is the whole point; check your ADB's region first.
- Save the SSH key it offers you.

### 1.2 Install Docker
```bash
sudo apt update && sudo apt install -y docker.io git
sudo usermod -aG docker $USER   # log out and back in
```

### 1.3 Copy the wallet up
From your PC:
```bash
scp -r "C:\CLAUDE CODE\NEW_POS\backend\wallet" ubuntu@<VM_IP>:~/wallet
```

### 1.4 Build and run
```bash
git clone https://github.com/<you>/<repo>.git && cd <repo>/backend
docker build -t built4u-api .

docker run -d --name built4u-api --restart unless-stopped \
  -p 127.0.0.1:8083:8083 \
  -v ~/wallet:/wallet:ro \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e TNS_ADMIN=/wallet \
  -e DB_URL='jdbc:oracle:thin:@built4u_low' \
  -e DB_USERNAME='built4u' \
  -e DB_PASSWORD='<schema password>' \
  -e JWT_SECRET='<64+ random chars>' \
  -e APP_CORS_ALLOWED_ORIGINS='https://pos.built4u-pos.com' \
  -e APP_SECURITY_SEED_ADMIN_PASSWORD='<strong bootstrap password>' \
  built4u-api
```

Generate the JWT secret with `openssl rand -base64 64`. Every one of those
variables is required — the prod profile refuses to start without them, on
purpose.

Binding to `127.0.0.1:8083` keeps the API off the public internet; step 2 exposes
it through Cloudflare.

**Check:** `docker logs -f built4u-api` → `Started Application`, and Flyway
reporting V28/V29 applied. Then `curl localhost:8083/actuator/health` → `{"status":"UP"}`.

> If it fails on `ORA-17868 unknown host`, `TNS_ADMIN` isn't pointing at the
> wallet. On `ORA-01017`, the username/password is wrong.

### 1.5 Expose it with a Cloudflare Tunnel
Use a tunnel rather than opening a port: OCI security lists *and* the image's own
firewall both block inbound traffic by default, and this way there's nothing
public to attack and no TLS certificate to manage on the VM.

```bash
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 -o cloudflared
sudo install cloudflared /usr/local/bin/
cloudflared tunnel login
cloudflared tunnel create built4u-api
cloudflared tunnel route dns built4u-api api.built4u-pos.com
```

`~/.cloudflared/config.yml`:
```yaml
tunnel: built4u-api
credentials-file: /home/ubuntu/.cloudflared/<TUNNEL_ID>.json
ingress:
  - hostname: api.built4u-pos.com
    service: http://localhost:8083
  - service: http_status:404
```

```bash
sudo cloudflared service install
sudo systemctl enable --now cloudflared
```

The DNS record is created for you. **Check:**
`curl https://api.built4u-pos.com/actuator/health` → `{"status":"UP"}`.

---

## 2. Frontend — Cloudflare Pages

Cloudflare Dashboard → **Workers & Pages → Create → Pages → Connect to Git**:

| Setting | Value |
|---|---|
| Root directory | `frontend` |
| Build command | `npm run build` |
| Output directory | `dist` |
| Environment variable | `VITE_API_BASE_URL` = `https://api.built4u-pos.com/api` |

`VITE_API_BASE_URL` is read **at build time**, so changing it later means
redeploying. It also feeds the production CSP automatically, so the browser is
allowed to call the API.

Then **Custom domains → Set up a domain** → `pos.built4u-pos.com`.

**Check:** open `https://pos.built4u-pos.com`, log in, then reload the page while
on an inner screen. If a refresh 404s, `frontend/public/_redirects` didn't ship.

---

## 3. Print agent (till PC)

The printer sits on your LAN at a private address, which the cloud has no route
to — so the browser relays the bytes to an agent on the till PC. See
[print-agent/README.md](print-agent/README.md) for the reasoning.

```bash
# on the till PC, with Node 18+ installed
cd print-agent
set ALLOWED_ORIGIN=https://pos.built4u-pos.com
node agent.js
```

Make it start automatically: Task Scheduler → "At log on".

**Check:** Admin → Document settings → **Test print**. A slip prints and the
drawer kicks. Without the agent running, the app falls back to server-side
printing, which now silently fails from the cloud — so if a receipt doesn't
appear, check the agent first.

---

## 4. Email (Resend + DNS)

Two different things, often confused:

- **Sending** reports → **Resend**. Needs DNS records; can then send to *anyone*.
- **Receiving** at `@built4u-pos.com` → **Cloudflare Email Routing**. Forwards to
  an inbox you verify. Not involved in sending.

### 4.1 Verify the domain with Resend
Create a Resend account, add `built4u-pos.com`, and copy the records it gives you
into Cloudflare DNS — typically:

| Type | Name | Value |
|---|---|---|
| TXT | `send` | `v=spf1 include:amazonses.com ~all` |
| TXT | `resend._domainkey` | (the DKIM key Resend shows) |
| TXT | `_dmarc` | `v=DMARC1; p=none;` |

Set these **DNS only** (grey cloud). Use the exact values from your Resend
dashboard — the above is illustrative.

### 4.2 Turn delivery on
Add to the `docker run` and restart the container:
```
-e APP_MAIL_RESEND_API_KEY='re_...'
-e APP_MAIL_FROM='Built4U <reports@built4u-pos.com>'
```
Until the key is set, the app saves recipients but sends nothing — by design.

**Check:** Admin → Report email; the "delivery is disabled" banner should be gone.
Set recipients on a report, then use **Email** on that report.

### 4.3 Inbound (optional)
Cloudflare → **Email → Email Routing** → forward `sales@built4u-pos.com` to your
Gmail. The destination address has to confirm once by email.

---

## 5. Go-live checks

- [ ] `https://api.built4u-pos.com/actuator/health` returns `{"status":"UP"}`
- [ ] Log in at `https://pos.built4u-pos.com`
- [ ] Refresh on an inner page — no 404
- [ ] Items load in POS (proves the DB path)
- [ ] Test print + drawer via the agent
- [ ] Generate a report, download the PDF
- [ ] Email a report to two users
- [ ] **Change the seeded admin password**
- [ ] Ring up a ₱1 sale end to end, then void it

## Cost

| | |
|---|---|
| Oracle Cloud Always Free VM | $0 |
| Cloudflare Pages / DNS / Tunnel | $0 |
| Oracle ADB | your existing tier |
| Resend | free to 3,000 emails/month |
| **Total** | **~$0/month** plus the domain |

## Notes

- **Backups.** ADB takes automatic backups; confirm the retention on your tier.
  A weekly Data Pump export to Object Storage is worth adding.
- **Updates.** `git pull && docker build -t built4u-api . && docker restart built4u-api`.
  New migrations apply on boot.
- **If the internet drops, the till stops.** The database is in the cloud, so this
  is inherent to hosting — not something the print agent solves. True offline
  operation needs a local database and sync, which is a separate project.
