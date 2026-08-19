# Built4U print agent

Prints receipts and kicks the cash drawer when the backend is **not** on the
shop's network.

## Why it exists

The thermal printer sits on the shop LAN at a private address (`192.168.x.x`).
Private addresses are not routable from the internet — every shop reuses the same
ranges — so a hosted backend has no path to the printer, no matter how the
firewall is configured.

The browser, though, is *in the shop*. So it does the relay:

```
Browser ──HTTPS──> backend         builds the ESC/POS bytes
   └──── HTTP ────> print agent ──TCP──> printer :9100
```

The bytes are exactly the same ones the backend would have sent itself; only the
last hop changes. When the backend *is* on the shop LAN (a local install), the
app skips the agent and prints server-side as before — no configuration switch.

## Running it

Needs Node 18+. No dependencies.

```bash
node agent.js
```

| Env | Default | Notes |
|-----|---------|-------|
| `PORT` | `9110` | Port to listen on |
| `ALLOWED_ORIGIN` | `*` | Set to your app's URL in production, e.g. `https://pos.built4u-pos.com` |

It binds to `127.0.0.1` only, so nothing off this machine can reach it — worth
keeping, since `/print` takes no authentication and drives the cash drawer.

Run it on a PC on the same network as the printer — normally the till PC. To
start it automatically, use Task Scheduler ("At log on") on Windows or a systemd
unit on Linux.

## API

- `GET /health` → `{ ok: true, ... }`. The app probes this to decide whether to
  relay or print server-side.
- `POST /print` → `{ host, port, data }` where `data` is base64 ESC/POS bytes.
  `200` on success, `400` for a bad request, `502` when the printer can't be
  reached (with the reason).

## Checking it works

With the agent running, print a test slip from **Admin → Document settings**, or
by hand:

```bash
curl -X POST http://127.0.0.1:9110/print \
  -H 'Content-Type: application/json' \
  -d '{"host":"192.168.1.50","port":9100,"data":"G0BIRUxMTwo="}'
```

A `502` almost always means the printer IP or port is wrong, or the printer is
off — the agent reports the underlying socket error.
