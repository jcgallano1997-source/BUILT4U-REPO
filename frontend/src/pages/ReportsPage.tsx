import { useEffect, useMemo, useState } from 'react'
import {
  BarChart3, Calendar, FileText, Receipt, Search, Sheet, ShoppingBag, Tags, TrendingUp,
} from 'lucide-react'
import { toast } from 'sonner'
import { btnGhost, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import { useAuthStore } from '@/store/authStore'
import {
  downloadReport, getSalesDetailed, getSalesOverview, reportErr,
  type SalesDetailLine, type SalesDetailed, type SalesOverview,
} from '@/lib/reports'

// ── date helpers ─────────────────────────────────────────────────────────────
const todayIso = () => new Date().toISOString().slice(0, 10)
const isoMinusDays = (n: number) => new Date(Date.now() - n * 864e5).toISOString().slice(0, 10)
const startOfMonthIso = () => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10) }

type Tab = 'overview' | 'top-items' | 'by-category' | 'detail'

export default function ReportsPage() {
  const modules = useAuthStore((s) => s.user?.modules ?? [])
  const has = (m: string) => modules.includes(m)

  return (
    <div className="mx-auto max-w-[1100px] space-y-4">
      <div>
        <h1 className="flex items-center gap-2 text-[22px] font-extrabold tracking-tight text-slate-900">
          <BarChart3 className="text-blue-600" size={22} /> Sales Report
        </h1>
        <p className="mt-0.5 text-[13px] text-slate-500">Sales analytics across the selected date range.</p>
      </div>

      {has('SALES_REPORTS')
        ? <SalesReports />
        : <p className="rounded-2xl border border-slate-200/70 bg-white p-6 text-sm text-slate-500">You don't have access to sales reports.</p>}
    </div>
  )
}

// ── Export buttons ───────────────────────────────────────────────────────────
function ExportButtons({ report, from, to }: { report: string; from?: string; to?: string }) {
  const [busy, setBusy] = useState<'pdf' | 'xlsx' | null>(null)
  async function dl(fmt: 'pdf' | 'xlsx') {
    setBusy(fmt)
    try { await downloadReport(report, fmt, from && to ? { from, to } : {}) }
    catch (e) { toast.error(reportErr(e, 'Download failed')) } finally { setBusy(null) }
  }
  return (
    <div className="flex gap-2">
      <button className={btnGhost} disabled={busy !== null} onClick={() => dl('pdf')}>
        <FileText size={14} /> {busy === 'pdf' ? '…' : 'PDF'}
      </button>
      <button className={btnGhost} disabled={busy !== null} onClick={() => dl('xlsx')}>
        <Sheet size={14} /> {busy === 'xlsx' ? '…' : 'Excel'}
      </button>
    </div>
  )
}

// ── Sales reports (tabbed) ───────────────────────────────────────────────────
function SalesReports() {
  const [from, setFrom] = useState(isoMinusDays(6))
  const [to, setTo] = useState(todayIso())
  const [tab, setTab] = useState<Tab>('overview')
  const [overview, setOverview] = useState<SalesOverview | null>(null)
  const [detail, setDetail] = useState<SalesDetailed | null>(null)
  const [loading, setLoading] = useState(false)
  const invalid = from > to

  useEffect(() => {
    if (invalid) return
    let cancelled = false
    setLoading(true)
    Promise.all([getSalesOverview(from, to), getSalesDetailed(from, to)])
      .then(([o, d]) => { if (!cancelled) { setOverview(o); setDetail(d) } })
      .catch((e) => { if (!cancelled) toast.error(reportErr(e, 'Failed to load sales data')) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [from, to, invalid])

  const presets: [string, () => void][] = [
    ['Today', () => { const t = todayIso(); setFrom(t); setTo(t) }],
    ['Last 7 days', () => { setFrom(isoMinusDays(6)); setTo(todayIso()) }],
    ['Last 30 days', () => { setFrom(isoMinusDays(29)); setTo(todayIso()) }],
    ['This month', () => { setFrom(startOfMonthIso()); setTo(todayIso()) }],
  ]

  const tabs: { id: Tab; label: string; icon: typeof TrendingUp }[] = [
    { id: 'overview', label: 'Overview', icon: TrendingUp },
    { id: 'top-items', label: 'Top items', icon: ShoppingBag },
    { id: 'by-category', label: 'By category', icon: Tags },
    { id: 'detail', label: 'Detail', icon: Receipt },
  ]

  return (
    <div className="space-y-4">
      {/* Date range + presets */}
      <div className="rounded-2xl border border-slate-200/70 bg-white p-4">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="mb-1 block text-xs text-slate-500">From</label>
            <input className={`${inputCls} w-40`} type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-slate-500">To</label>
            <input className={`${inputCls} w-40`} type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} />
          </div>
          <div className="flex flex-wrap gap-1.5">
            {presets.map(([label, fn]) => (
              <button key={label} onClick={fn}
                className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-[12.5px] font-medium text-slate-600 hover:bg-slate-50">
                {label === 'Today' && <Calendar size={12} />} {label}
              </button>
            ))}
          </div>
          {invalid && <p className="text-sm text-red-600">"From" must be on or before "To".</p>}
        </div>
      </div>

      {/* Tab strip */}
      <div className="flex flex-wrap gap-1 border-b border-slate-200">
        {tabs.map((t) => {
          const Icon = t.icon
          return (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium ${
                tab === t.id ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}>
              <Icon size={15} /> {t.label}
            </button>
          )
        })}
      </div>

      {invalid ? null : loading && !overview ? (
        <p className="py-8 text-center text-sm text-slate-400">Loading…</p>
      ) : (
        <>
          {tab === 'overview' && <OverviewTab overview={overview} detail={detail} from={from} to={to} />}
          {tab === 'top-items' && <TopItemsTab detail={detail} from={from} to={to} />}
          {tab === 'by-category' && <ByCategoryTab detail={detail} from={from} to={to} />}
          {tab === 'detail' && <DetailTab detail={detail} from={from} to={to} />}
        </>
      )}
    </div>
  )
}

function Kpi({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200/70 bg-white p-4">
      <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">{label}</div>
      <div className="num mt-1 text-[26px] font-bold text-slate-900">{value}</div>
    </div>
  )
}

function OverviewTab({ overview, detail, from, to }: { overview: SalesOverview | null; detail: SalesDetailed | null; from: string; to: string }) {
  if (!overview) return null
  const count = overview.salesCount
  const net = overview.netSales
  const avg = count > 0 ? net / count : 0
  const itemsSold = detail?.totalQty ?? 0
  const empty = count === 0
  const maxDay = Math.max(1, ...overview.byDay.map((d) => d.net))
  const maxMode = Math.max(1, ...overview.byMode.map((m) => m.total))

  return (
    <div className="space-y-4">
      <div className="flex justify-end"><ExportButtons report="sales-overview" from={from} to={to} /></div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Kpi label="Total sales" value={peso(net)} />
        <Kpi label="Sales count" value={String(count)} />
        <Kpi label="Avg sale" value={peso(avg)} />
        <Kpi label="Items sold" value={Number(itemsSold).toFixed(0)} />
      </div>

      {empty ? (
        <div className="rounded-2xl border border-slate-200/70 bg-white py-12 text-center text-slate-400">No sales in this range.</div>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.7fr_1fr]">
          <div className="rounded-2xl border border-slate-200/70 bg-white p-5">
            <div className="mb-4 text-[14.5px] font-bold">Daily sales</div>
            <div className="flex h-56 items-end gap-2">
              {overview.byDay.map((d) => (
                <div key={d.date} className="flex h-full flex-1 flex-col items-center justify-end gap-1.5">
                  <div className="w-full max-w-[42px] rounded-t-[6px] bg-blue-500" style={{ height: `${Math.max(4, (d.net / maxDay) * 190)}px` }} title={`${d.date}: ${peso(d.net)}`} />
                  <span className="text-[10px] text-slate-400">{d.date.slice(5)}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-200/70 bg-white p-5">
            <div className="mb-4 text-[14.5px] font-bold">By payment mode</div>
            <div className="flex flex-col gap-3">
              {overview.byMode.map((m) => (
                <div key={m.mode}>
                  <div className="mb-1 flex justify-between text-[12.5px]">
                    <span className="font-semibold text-slate-700">{m.mode}</span>
                    <span className="num text-slate-500">{peso(m.total)} · {m.count}</span>
                  </div>
                  <div className="h-[7px] overflow-hidden rounded bg-slate-100">
                    <div className="h-full rounded bg-blue-500" style={{ width: `${Math.max(4, (m.total / maxMode) * 100)}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {overview.lineDiscounts + overview.orderDiscounts > 0 && (
        <p className="text-xs text-slate-500">
          Total discounts given (line + cart): <span className="font-medium text-slate-700">{peso(overview.lineDiscounts + overview.orderDiscounts)}</span>
          {' · '}VOIDED sales excluded from all aggregates.
        </p>
      )}
    </div>
  )
}

// aggregate helpers
function aggregate<T extends string>(lines: SalesDetailLine[], key: (l: SalesDetailLine) => T) {
  const m = new Map<T, { key: T; qty: number; revenue: number; lines: number }>()
  for (const l of lines) {
    const k = key(l)
    const cur = m.get(k) ?? { key: k, qty: 0, revenue: 0, lines: 0 }
    cur.qty += Number(l.qty) || 0
    cur.revenue += Number(l.lineTotal) || 0
    cur.lines += 1
    m.set(k, cur)
  }
  return [...m.values()]
}

function TopItemsTab({ detail, from, to }: { detail: SalesDetailed | null; from: string; to: string }) {
  const [sortBy, setSortBy] = useState<'revenue' | 'quantity'>('revenue')
  const [limit, setLimit] = useState(10)
  const [search, setSearch] = useState('')
  const rows = useMemo(() => {
    if (!detail) return []
    const q = search.trim().toLowerCase()
    let agg = aggregate(detail.lines, (l) => l.item || '(unknown)')
    if (q) agg = agg.filter((r) => r.key.toLowerCase().includes(q))
    agg.sort((a, b) => (sortBy === 'revenue' ? b.revenue - a.revenue : b.qty - a.qty))
    return agg.slice(0, limit)
  }, [detail, sortBy, limit, search])

  return (
    <div className="rounded-2xl border border-slate-200/70 bg-white">
      <div className="flex flex-wrap items-center justify-between gap-2 p-4">
        <div className="text-[14.5px] font-bold">Top {limit} items</div>
        <div className="flex items-center gap-2">
          <div className="flex overflow-hidden rounded-lg border border-slate-200">
            {(['revenue', 'quantity'] as const).map((s) => (
              <button key={s} onClick={() => setSortBy(s)} className={`px-3 py-1 text-xs ${sortBy === s ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-50'}`}>
                By {s === 'revenue' ? 'revenue' : 'quantity'}
              </button>
            ))}
          </div>
          <div className="flex overflow-hidden rounded-lg border border-slate-200">
            {[10, 25, 50].map((n) => (
              <button key={n} onClick={() => setLimit(n)} className={`px-3 py-1 text-xs ${limit === n ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-50'}`}>Top {n}</button>
            ))}
          </div>
          <div className="relative w-48">
            <Search size={14} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input className={`${inputCls} pl-8`} placeholder="Search item…" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <ExportButtons report="sales-detailed" from={from} to={to} />
        </div>
      </div>
      {rows.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-slate-400">No items sold in this range.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-y border-slate-100 bg-slate-50 text-left text-slate-500">
              <tr>
                <th className="w-10 px-4 py-2 text-right font-medium">#</th>
                <th className="px-4 py-2 font-medium">Item</th>
                <th className="px-4 py-2 text-right font-medium">Qty sold</th>
                <th className="px-4 py-2 text-right font-medium">Revenue</th>
                <th className="px-4 py-2 text-right font-medium">Lines</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r, i) => (
                <tr key={r.key}>
                  <td className="px-4 py-2 text-right text-xs text-slate-400">{i + 1}</td>
                  <td className="px-4 py-2 font-medium text-slate-700">{r.key}</td>
                  <td className="num px-4 py-2 text-right">{r.qty.toFixed(0)}</td>
                  <td className="num px-4 py-2 text-right font-medium">{peso(r.revenue)}</td>
                  <td className="num px-4 py-2 text-right text-slate-400">{r.lines}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function ByCategoryTab({ detail, from, to }: { detail: SalesDetailed | null; from: string; to: string }) {
  const [search, setSearch] = useState('')
  const rows = useMemo(() => {
    if (!detail) return []
    const q = search.trim().toLowerCase()
    let agg = aggregate(detail.lines, (l) => l.category || '(uncategorized)')
    if (q) agg = agg.filter((r) => r.key.toLowerCase().includes(q))
    agg.sort((a, b) => b.revenue - a.revenue)
    return agg
  }, [detail, search])
  const max = Math.max(1, ...rows.map((r) => r.revenue))

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="relative w-56">
          <Search size={14} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input className={`${inputCls} pl-8`} placeholder="Search category…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <ExportButtons report="sales-detailed" from={from} to={to} />
      </div>
      {rows.length === 0 ? (
        <div className="rounded-2xl border border-slate-200/70 bg-white py-12 text-center text-slate-400">No category sales in this range.</div>
      ) : (
        <div className="rounded-2xl border border-slate-200/70 bg-white">
          <div className="p-4 text-[14.5px] font-bold">Revenue by category</div>
          <div className="space-y-2.5 px-4 pb-2">
            {rows.map((r) => (
              <div key={r.key}>
                <div className="mb-1 flex justify-between text-[12.5px]">
                  <span className="font-semibold text-slate-700">{r.key}</span>
                  <span className="num text-slate-500">{peso(r.revenue)} · {r.qty.toFixed(0)} sold</span>
                </div>
                <div className="h-[8px] overflow-hidden rounded bg-slate-100">
                  <div className="h-full rounded bg-blue-500" style={{ width: `${Math.max(3, (r.revenue / max) * 100)}%` }} />
                </div>
              </div>
            ))}
          </div>
          <table className="mt-2 w-full text-sm">
            <thead className="border-y border-slate-100 bg-slate-50 text-left text-slate-500">
              <tr>
                <th className="px-4 py-2 font-medium">Category</th>
                <th className="px-4 py-2 text-right font-medium">Qty</th>
                <th className="px-4 py-2 text-right font-medium">Revenue</th>
                <th className="px-4 py-2 text-right font-medium">Lines</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r) => (
                <tr key={r.key}>
                  <td className="px-4 py-2 font-medium text-slate-700">{r.key}</td>
                  <td className="num px-4 py-2 text-right">{r.qty.toFixed(0)}</td>
                  <td className="num px-4 py-2 text-right font-medium">{peso(r.revenue)}</td>
                  <td className="num px-4 py-2 text-right text-slate-400">{r.lines}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function DetailTab({ detail, from, to }: { detail: SalesDetailed | null; from: string; to: string }) {
  const [search, setSearch] = useState('')
  const rows = useMemo(() => {
    if (!detail) return []
    const q = search.trim().toLowerCase()
    if (!q) return detail.lines
    return detail.lines.filter((l) => l.salesNumber.toLowerCase().includes(q) || (l.item ?? '').toLowerCase().includes(q))
  }, [detail, search])

  return (
    <div className="rounded-2xl border border-slate-200/70 bg-white">
      <div className="flex flex-wrap items-center justify-between gap-2 p-4">
        <div className="text-[14.5px] font-bold">All sale lines</div>
        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input className={`${inputCls} pl-9`} placeholder="Search by sale # or item…" value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <ExportButtons report="sales-detailed" from={from} to={to} />
        </div>
      </div>
      {rows.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-slate-400">No matching rows.</p>
      ) : (
        <div className="max-h-[560px] overflow-auto">
          <table className="w-full text-sm">
            <thead className="sticky top-0 border-y border-slate-100 bg-slate-50 text-left text-slate-500">
              <tr>
                <th className="px-3 py-2 font-medium">Date</th>
                <th className="px-3 py-2 font-medium">Sale #</th>
                <th className="px-3 py-2 font-medium">Customer</th>
                <th className="px-3 py-2 font-medium">Item</th>
                <th className="px-3 py-2 text-right font-medium">Qty</th>
                <th className="px-3 py-2 text-right font-medium">Unit</th>
                <th className="px-3 py-2 text-right font-medium">Disc</th>
                <th className="px-3 py-2 text-right font-medium">Subtotal</th>
                <th className="px-3 py-2 text-right font-medium">Cost</th>
                <th className="px-3 py-2 text-right font-medium">Margin</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r, i) => (
                <tr key={`${r.salesNumber}-${i}`}>
                  <td className="px-3 py-2 text-xs text-slate-600">{r.date?.slice(0, 16).replace('T', ' ')}</td>
                  <td className="num px-3 py-2 text-xs">{r.salesNumber}</td>
                  <td className="px-3 py-2 text-xs">{r.customer || <span className="text-slate-400">walk-in</span>}</td>
                  <td className="px-3 py-2 font-medium">{r.item}</td>
                  <td className="num px-3 py-2 text-right">{Number(r.qty).toFixed(0)}</td>
                  <td className="num px-3 py-2 text-right">{peso(r.unitPrice)}</td>
                  <td className="num px-3 py-2 text-right text-slate-400">{Number(r.lineDiscount) > 0 ? `−${peso(r.lineDiscount)}` : ''}</td>
                  <td className="num px-3 py-2 text-right font-medium">{peso(r.lineTotal)}</td>
                  <td className="num px-3 py-2 text-right text-slate-400">{peso(r.lineCogs)}</td>
                  <td className={`num px-3 py-2 text-right font-medium ${Number(r.margin) < 0 ? 'text-red-600' : 'text-emerald-700'}`}>{peso(r.margin)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {detail && (
        <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2 text-xs text-slate-400">
          <span>{rows.length} of {detail.lineCount} line(s)</span>
          <span>
            Cost of goods <span className="font-medium text-slate-600">{peso(detail.totalCogs)}</span>
            {' · '}Gross margin <span className={`font-medium ${detail.totalMargin < 0 ? 'text-red-600' : 'text-emerald-700'}`}>{peso(detail.totalMargin)}</span>
          </span>
        </div>
      )}
    </div>
  )
}
