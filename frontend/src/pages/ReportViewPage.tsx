import { useEffect, useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { BarChart3, FileText, Mail, Search, Sheet } from 'lucide-react'
import { toast } from 'sonner'
import { btnGhost, inputCls } from '@/components/Modal'
import { useAuthStore } from '@/store/authStore'
import { downloadReport, fetchReportJson, reportErr } from '@/lib/reports'
import { emailReport, emailErr } from '@/lib/reportemail'

const todayIso = () => new Date().toISOString().slice(0, 10)
const startOfMonthIso = () => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10) }

interface ReportDef { title: string; desc: string; report: string; module: string; dated: boolean; asOf?: boolean; dataPath?: string }

const DEFS: Record<string, ReportDef> = {
  'inventory-snapshot': { title: 'Inventory Snapshot Report', desc: 'Every item with stock, cost and value — as of any date.', report: 'inventory-snapshot', module: 'INVENTORY_SNAPSHOT', dated: false, asOf: true },
  'inventory-valuation': { title: 'Inventory Valuation Report', desc: 'Stock value grouped by category — as of any date.', report: 'inventory-valuation', module: 'INVENTORY_VALUATION', dated: false, asOf: true, dataPath: 'categories' },
  'inventory-movement': { title: 'Inventory Movement Report', desc: 'Stock in / out / adjust / transfer per item.', report: 'inventory-movement', module: 'INVENTORY_MOVEMENT', dated: true, dataPath: 'rows' },
  'reorder': { title: 'Reorder Suggestions', desc: 'Low-stock items with suggested reorder quantities and estimated cost.', report: 'reorder', module: 'REORDER_REPORT', dated: false, dataPath: 'rows' },
  'profit-margin': { title: 'Profit & Margin Report', desc: 'Revenue, cost of goods and margin per item — from the cost captured at sale time.', report: 'profit-margin', module: 'PROFIT_REPORT', dated: true, dataPath: 'rows' },
  'sales-by-cashier': { title: 'Sales by Cashier', desc: 'Sales, discounts and net per cashier over the period.', report: 'sales-by-cashier', module: 'SALES_ANALYTICS', dated: true, dataPath: 'rows' },
  'sales-by-hour': { title: 'Sales by Hour', desc: 'Sales and net revenue by hour of day — spot your peak hours.', report: 'sales-by-hour', module: 'SALES_ANALYTICS', dated: true, dataPath: 'rows' },
  'dead-stock': { title: 'Dead Stock Report', desc: 'Slow / non-moving items (idle 30+ days) with the cash tied up in them.', report: 'dead-stock', module: 'DEAD_STOCK_REPORT', dated: false, dataPath: 'rows' },
  'customer-purchases': { title: 'Customer Purchases', desc: 'Per-customer spend, frequency and recency over the period.', report: 'customer-purchases', module: 'CUSTOMER_REPORT', dated: true, dataPath: 'rows' },
  'shift-history': { title: 'Shift History Report', desc: 'Per-shift cash reconciliation, with the sales rung in each shift (in PDF/Excel).', report: 'shift-history', module: 'SHIFT_HISTORY_REPORT', dated: true },
  'discounts-overrides': { title: 'Discounts & Overrides Report', desc: 'Every price override and line discount, with reason and approving manager.', report: 'discounts-overrides', module: 'DISCOUNTS_REPORT', dated: true, dataPath: 'rows' },
  'goods-receipts': { title: 'Goods Receive Report', desc: 'Received deliveries with totals.', report: 'goods-receipts', module: 'GOODS_RECEIPTS_REPORT', dated: true },
  'purchase-orders': { title: 'Purchase Order Report', desc: 'Purchase orders with totals & status.', report: 'purchase-orders', module: 'PURCHASE_ORDERS_REPORT', dated: true },
  'stock-transfers': { title: 'Stock Transfers Report', desc: 'Cross-site transfers with status.', report: 'stock-transfers', module: 'STOCK_TRANSFER_REPORT', dated: false },
  'receivables': { title: 'Accounts Receivable Report', desc: 'Outstanding customer balances.', report: 'receivables', module: 'RECEIVABLES_REPORT', dated: false },
  'payables': { title: 'Accounts Payable Report', desc: 'Outstanding supplier & expense balances.', report: 'payables', module: 'PAYABLES_REPORT', dated: false },
}

function prettify(key: string): string {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase()).replace(/\bId\b/, 'ID').trim()
}

function formatCell(v: unknown): string {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'boolean') return v ? 'Yes' : 'No'
  if (typeof v === 'number') {
    return Number.isInteger(v) ? v.toLocaleString('en-US') : v.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  if (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(v)) return v.slice(0, 16).replace('T', ' ')
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

export default function ReportViewPage() {
  const { key } = useParams<{ key: string }>()
  const def = key ? DEFS[key] : undefined
  const modules = useAuthStore((s) => s.user?.modules ?? [])

  const [from, setFrom] = useState(startOfMonthIso())
  const [to, setTo] = useState(todayIso())
  const [asOf, setAsOf] = useState(todayIso())
  const [search, setSearch] = useState('')
  const [rows, setRows] = useState<Record<string, unknown>[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!def) return
    let cancelled = false
    setLoading(true)
    const params: Record<string, string> = def.dated ? { from, to } : def.asOf ? { asOf } : {}
    fetchReportJson<unknown>(def.report, params)
      .then((data) => {
        if (cancelled) return
        const arr = def.dataPath ? ((data as Record<string, unknown>)?.[def.dataPath] as unknown[]) : (data as unknown[])
        setRows(Array.isArray(arr) ? (arr as Record<string, unknown>[]) : [])
      })
      .catch((e) => { if (!cancelled) toast.error(reportErr(e, 'Failed to load report')) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [def, from, to, asOf])

  if (!def) return <Navigate to="/reports" replace />
  if (!modules.includes(def.module)) {
    return <p className="rounded-2xl border border-slate-200/70 bg-white p-6 text-sm text-slate-500">You don't have access to this report.</p>
  }

  // Hide internal id columns (id, catId, locId, barcodeId, customerId…) from the
  // on-screen preview; the PDF/Excel exports use their own curated columns.
  const columns = rows.length > 0
    ? Object.keys(rows[0]).filter((k) => k !== 'id' && k !== 'createdAt' && k !== 'sales' && !/Id$/.test(k))
    : []

  // Client-side search across every field of each row.
  const q = search.trim().toLowerCase()
  const filtered = q
    ? rows.filter((r) => Object.values(r).some((v) => String(v ?? '').toLowerCase().includes(q)))
    : rows

  function currentParams(): Record<string, string> {
    return def!.dated ? { from, to } : def!.asOf ? { asOf } : {}
  }

  async function dl(fmt: 'pdf' | 'xlsx') {
    try { await downloadReport(def!.report, fmt, currentParams()) }
    catch (e) { toast.error(reportErr(e, 'Download failed')) }
  }

  async function email() {
    try {
      await emailReport(def!.report, 'pdf', currentParams())
      toast.success('Report emailed to the configured recipient')
    } catch (e) { toast.error(emailErr(e, 'Email failed')) }
  }

  return (
    <div className="mx-auto max-w-[1100px] space-y-4">
      <div>
        <h1 className="flex items-center gap-2 text-[22px] font-extrabold tracking-tight text-slate-900">
          <BarChart3 className="text-blue-600" size={22} /> {def.title}
        </h1>
        <p className="mt-0.5 text-[13px] text-slate-500">{def.desc}</p>
      </div>

      <div className="rounded-2xl border border-slate-200/70 bg-white p-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          {def.dated ? (
            <div className="flex flex-wrap items-end gap-3">
              <div><label className="mb-1 block text-xs text-slate-500">From</label><input className={`${inputCls} w-40`} type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} /></div>
              <div><label className="mb-1 block text-xs text-slate-500">To</label><input className={`${inputCls} w-40`} type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} /></div>
              <div className="flex gap-1.5">
                <button onClick={() => { setFrom(startOfMonthIso()); setTo(todayIso()) }} className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-[12.5px] font-medium text-slate-600 hover:bg-slate-50">This month</button>
              </div>
            </div>
          ) : def.asOf ? (
            <div className="flex flex-wrap items-end gap-3">
              <div><label className="mb-1 block text-xs text-slate-500">As of date</label><input className={`${inputCls} w-44`} type="date" value={asOf} max={todayIso()} onChange={(e) => setAsOf(e.target.value)} /></div>
              <button onClick={() => setAsOf(todayIso())} className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-[12.5px] font-medium text-slate-600 hover:bg-slate-50">Today</button>
              {asOf < todayIso() && (
                <span className="pb-1.5 text-[12px] text-amber-600">Historical — reconstructed from stock movements (current cost)</span>
              )}
            </div>
          ) : <div className="text-xs text-slate-400">Current snapshot</div>}
          <div className="flex gap-2">
            <button className={btnGhost} onClick={() => dl('pdf')}><FileText size={14} /> PDF</button>
            <button className={btnGhost} onClick={() => dl('xlsx')}><Sheet size={14} /> Excel</button>
            <button className={btnGhost} onClick={email} title="Email this report to its configured recipient"><Mail size={14} /> Email</button>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200/70 bg-white">
        {rows.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 p-3">
            <div className="text-[13px] font-semibold text-slate-600">Results</div>
            <div className="relative w-72 max-w-full">
              <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input className={`${inputCls} pl-9`} placeholder="Search this report…" value={search} onChange={(e) => setSearch(e.target.value)} />
            </div>
          </div>
        )}
        {loading ? (
          <p className="px-4 py-10 text-center text-sm text-slate-400">Loading…</p>
        ) : rows.length === 0 ? (
          <p className="px-4 py-10 text-center text-sm text-slate-400">No data for this report.</p>
        ) : filtered.length === 0 ? (
          <p className="px-4 py-10 text-center text-sm text-slate-400">No rows match “{search}”.</p>
        ) : (
          <div className="max-h-[620px] overflow-auto">
            <table className="w-full text-sm">
              <thead className="sticky top-0 border-b border-slate-100 bg-slate-50 text-left text-slate-500">
                <tr>
                  {columns.map((c) => (
                    <th key={c} className={`px-3 py-2 font-medium ${typeof rows[0][c] === 'number' ? 'text-right' : ''}`}>{prettify(c)}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((r, i) => (
                  <tr key={i}>
                    {columns.map((c) => (
                      <td key={c} className={`px-3 py-2 ${typeof r[c] === 'number' ? 'num text-right' : 'text-slate-700'}`}>{formatCell(r[c])}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {rows.length > 0 && <p className="px-4 py-2 text-xs text-slate-400">{filtered.length} of {rows.length} row(s)</p>}
      </div>
    </div>
  )
}
