#!/usr/bin/env node
/**
 * Built4U POS — local print agent.
 *
 * The receipt printer and cash drawer live on the shop's LAN behind a private
 * address (192.168.x.x), which a hosted backend has no route to. The browser,
 * though, is in the shop: it fetches the ESC/POS bytes from the backend and
 * hands them to this agent, which does the one thing that must happen locally —
 * open a TCP socket to the printer.
 *
 *   Browser ──HTTPS──> backend            (builds the bytes)
 *      └─────HTTP─────> this agent ──TCP──> printer :9100
 *
 * No dependencies — plain Node. Run it on a PC on the same network as the
 * printer:  node agent.js
 *
 * Env:
 *   PORT           port to listen on            (default 9110)
 *   ALLOWED_ORIGIN CORS origin allowed to call  (default *, set this in production)
 */
const http = require('http');
const net = require('net');

const PORT = Number(process.env.PORT || 9110);
const ALLOWED_ORIGIN = process.env.ALLOWED_ORIGIN || '*';
const CONNECT_TIMEOUT_MS = 5000;
const MAX_BODY_BYTES = 1 << 20; // 1 MB — a receipt is a few KB; refuse anything wild

/** Write raw bytes to a network printer (RAW/JetDirect, usually port 9100). */
function sendToPrinter(host, port, data) {
  return new Promise((resolve, reject) => {
    const socket = new net.Socket();
    let settled = false;
    const done = (err) => {
      if (settled) return;
      settled = true;
      socket.destroy();
      err ? reject(err) : resolve();
    };
    socket.setTimeout(CONNECT_TIMEOUT_MS);
    socket.once('timeout', () => done(new Error(`Timed out connecting to ${host}:${port}`)));
    socket.once('error', (e) => done(e));
    socket.connect(port, host, () => {
      // Wait for the bytes to reach the wire before closing, or a short receipt
      // can be cut off mid-print.
      socket.end(data, () => done(null));
    });
  });
}

function cors(res) {
  res.setHeader('Access-Control-Allow-Origin', ALLOWED_ORIGIN);
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Access-Control-Max-Age', '86400');
  // Chrome's Private Network Access preflight: this agent is on a private
  // address and the page calling it is public, so it must opt in explicitly.
  res.setHeader('Access-Control-Allow-Private-Network', 'true');
}

function json(res, status, body) {
  const payload = JSON.stringify(body);
  res.writeHead(status, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) });
  res.end(payload);
}

const server = http.createServer((req, res) => {
  cors(res);

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

  // Lets the app detect whether an agent is running before offering to use it.
  if (req.method === 'GET' && req.url === '/health') {
    json(res, 200, { ok: true, agent: 'built4u-print-agent', version: 1 });
    return;
  }

  if (req.method === 'POST' && req.url === '/print') {
    const chunks = [];
    let size = 0;
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY_BYTES) { json(res, 413, { error: 'Body too large' }); req.destroy(); return; }
      chunks.push(c);
    });
    req.on('end', async () => {
      let body;
      try { body = JSON.parse(Buffer.concat(chunks).toString('utf8')); }
      catch { json(res, 400, { error: 'Invalid JSON' }); return; }

      const { host, port, data } = body || {};
      if (!host || !data) { json(res, 400, { error: 'host and data are required' }); return; }

      let bytes;
      try { bytes = Buffer.from(String(data), 'base64'); }
      catch { json(res, 400, { error: 'data must be base64' }); return; }
      if (bytes.length === 0) { json(res, 400, { error: 'data decoded to zero bytes' }); return; }

      try {
        await sendToPrinter(String(host), Number(port) || 9100, bytes);
        console.log(`[print] ${bytes.length} bytes -> ${host}:${port || 9100}`);
        json(res, 200, { ok: true, bytes: bytes.length });
      } catch (e) {
        console.error(`[print] FAILED ${host}:${port || 9100} — ${e.message}`);
        json(res, 502, { error: e.message });
      }
    });
    return;
  }

  json(res, 404, { error: 'Not found' });
});

// Bind to loopback only: the browser on this machine is the only caller, and
// this endpoint prints without authentication.
server.listen(PORT, '127.0.0.1', () => {
  console.log(`Built4U print agent listening on http://127.0.0.1:${PORT}`);
  console.log(`  GET  /health  POST /print {host, port, data(base64)}`);
  if (ALLOWED_ORIGIN === '*') {
    console.log('  note: ALLOWED_ORIGIN is unset (allows any page) — set it in production.');
  }
});
