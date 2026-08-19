import api from '@/lib/api'

/**
 * Receipt printing has two routes:
 *
 *  - **Server-side** — the backend opens a socket to the printer itself. Works
 *    only while the backend shares a LAN with the printer.
 *  - **Local agent** — the backend returns the ESC/POS bytes and the browser
 *    (which is in the shop) relays them to an agent on this machine. Required
 *    once the backend is hosted off-site, since a private printer address isn't
 *    reachable from the internet.
 *
 * We try the agent first and fall back to the server, so the same build works in
 * either deployment with no switch to flip.
 */
const AGENT_URL = (import.meta.env.VITE_PRINT_AGENT_URL as string | undefined)?.replace(/\/$/, '')
  ?? 'http://localhost:9110'

interface PrintJob { host: string; port: number; dataBase64: string }

let agentUp: boolean | null = null   // cached so we probe once per page load

/** Is a local print agent running? Cached; probes with a short timeout. */
export async function agentAvailable(): Promise<boolean> {
  if (agentUp !== null) return agentUp
  try {
    const ctl = new AbortController()
    const t = setTimeout(() => ctl.abort(), 1200)
    const r = await fetch(`${AGENT_URL}/health`, { signal: ctl.signal })
    clearTimeout(t)
    agentUp = r.ok
  } catch {
    agentUp = false
  }
  return agentUp
}

async function relay(job: PrintJob): Promise<void> {
  const r = await fetch(`${AGENT_URL}/print`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ host: job.host, port: job.port, data: job.dataBase64 }),
  })
  if (!r.ok) {
    const msg = await r.json().catch(() => ({}))
    throw new Error((msg as { error?: string }).error ?? `Print agent returned ${r.status}`)
  }
}

/**
 * Run a printer action: via the local agent when one is running, otherwise
 * straight from the server.
 *
 * @param jobPath   endpoint returning the bytes, e.g. `/printer/jobs/test`
 * @param directPath endpoint that prints server-side, e.g. `/printer/test`
 */
async function run(jobPath: string, directPath: string): Promise<void> {
  if (await agentAvailable()) {
    const { data } = await api.post<PrintJob>(jobPath)
    await relay(data)
    return
  }
  await api.post(directPath)
}

export const printTest = () => run('/printer/jobs/test', '/printer/test')
export const openDrawer = () => run('/printer/jobs/open-drawer', '/printer/open-drawer')
/** Server-side receipt printing lives on the sales endpoint, not under /printer. */
export const printReceipt = (salesNumber: string) =>
  run(`/printer/jobs/receipt/${encodeURIComponent(salesNumber)}`, `/sales/${encodeURIComponent(salesNumber)}/print`)
