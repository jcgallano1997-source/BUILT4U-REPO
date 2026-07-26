import { useState, type ReactNode } from 'react'
import { BarChart3, FileText, Sheet } from 'lucide-react'
import { toast } from 'sonner'
import { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import { useAuthStore } from '@/store/authStore'
import { downloadReport, getSalesOverview, reportErr, type SalesOverview } from '@/lib/reports'

interface ReportDef { report: string; title: string; module: string; desc: string; params?: () => Record<string, string> }

export default function ReportsPage() {
  const modules = useAuthStore((s) => s.user?.modules ?? [])
  const has = (m: string) => modules.includes(m)

  const catalog: ReportDef[] = [
    { report: 'inventory-snapshot', title: 'Inventory snapshot', module: 'MOD_INVENTORY_SNAPSHOT', desc: 'Every item with current stock, cost and value.' },
    { report: 'inventory-valuation', title: 'Inventory valuation', module: 'MOD_INVENTORY_VALUATION', desc: 'Stock value grouped by category.' },
    { report: 'receivables', title: 'Accounts receivable', module: 'MOD_RECEIVABLES_REPORT', desc: 'Outstanding customer balances.' },
    { report: 'payables', title: 'Accounts payable', module: 'MOD_PAYABLES_REPORT', desc: 'Outstanding supplier & expense balances.' },
    { report: 'purchase-orders', title: 'Purchase orders', module: 'MOD_PURCHASE_ORDERS_REPORT', desc: 'PO header list with totals & status.' },
    { report: 'goods-receipts', title: 'Goods receipts', module: 'MOD_GOODS_RECEIPTS_REPORT', desc: 'Received deliveries with totals.' },
    { report: 'stock-transfers', title: 'Stock transfers', module: 'MOD_STOCK_TRANSFER_REPORT', desc: 'Cross-site transfers with status.' },
  ]

  return (
    <div className="space-y-5">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <BarChart3 size={18} className="text-indigo-600" /> Reports
      </h1>

      {has('MOD_SALES_REPORTS') && <SalesOverviewCard />}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {catalog.filter((r) => has(r.module)).map((r) => (
          <SimpleReportCard key={r.report} def={r} />
        ))}
      </div>
    </div>
  )
}

function DownloadButtons({ report, params }: { report: string; params?: Record<string, string> }) {
  const [busy, setBusy] = useState<'pdf' | 'xlsx' | null>(null)
  async function dl(format: 'pdf' | 'xlsx') {
    setBusy(format)
    try { await downloadReport(report, format, params) }
    catch (e) { toast.error(reportErr(e, 'Download failed')) } finally { setBusy(null) }
  }
  return (
    <div className="flex gap-2">
      <button className={btnGhost} disabled={busy !== null} onClick={() => dl('pdf')}><FileText size={14} /> {busy === 'pdf' ? '…' : 'PDF'}</button>
      <button className={btnGhost} disabled={busy !== null} onClick={() => dl('xlsx')}><Sheet size={14} /> {busy === 'xlsx' ? '…' : 'Excel'}</button>
    </div>
  )
}

function Card({ title, desc, children }: { title: string; desc: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="font-medium text-slate-800">{title}</div>
      <p className="mt-0.5 mb-3 text-xs text-slate-400">{desc}</p>
      {children}
    </div>
  )
}

function SimpleReportCard({ def }: { def: ReportDef }) {
  return <Card title={def.title} desc={def.desc}><DownloadButtons report={def.report} /></Card>
}

function SalesOverviewCard() {
  const today = new Date().toISOString().slice(0, 10)
  const monthAgo = new Date(Date.now() - 30 * 864e5).toISOString().slice(0, 10)
  const [from, setFrom] = useState(monthAgo)
  const [to, setTo] = useState(today)
  const [data, setData] = useState<SalesOverview | null>(null)
  const [loading, setLoading] = useState(false)

  async function view() {
    setLoading(true)
    try { setData(await getSalesOverview(from, to)) }
    catch (e) { toast.error(reportErr(e, 'Failed to load')) } finally { setLoading(false) }
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <div className="font-medium text-slate-800">Sales overview</div>
          <p className="mt-0.5 text-xs text-slate-400">Totals, payment-mode & daily breakdown over a date range.</p>
        </div>
        <div className="flex items-end gap-2">
          <div><label className="mb-1 block text-xs text-slate-500">From</label><input className={inputCls} type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></div>
          <div><label className="mb-1 block text-xs text-slate-500">To</label><input className={inputCls} type="date" value={to} onChange={(e) => setTo(e.target.value)} /></div>
          <button className={btnPrimary} disabled={loading} onClick={view}>{loading ? '…' : 'View'}</button>
          <DownloadButtons report="sales-overview" params={{ from, to }} />
        </div>
      </div>

      {data && (
        <div className="mt-4 space-y-3">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {[['Sales', String(data.salesCount)], ['Gross', peso(data.gross)], ['Discounts', peso(data.lineDiscounts + data.orderDiscounts)], ['Net', peso(data.netSales)]].map(([k, v]) => (
              <div key={k} className="rounded-md bg-slate-50 p-2 text-center"><div className="text-xs text-slate-400">{k}</div><div className="font-semibold text-slate-800">{v}</div></div>
            ))}
          </div>
          {data.byMode.length > 0 && (
            <div>
              <div className="mb-1 text-xs font-medium text-slate-500">By payment mode</div>
              <table className="w-full text-sm">
                <tbody className="divide-y divide-slate-100">
                  {data.byMode.map((m) => (
                    <tr key={m.mode}><td className="py-1 text-slate-600">{m.mode}</td><td className="py-1 text-right text-slate-400">{m.count} sale(s)</td><td className="py-1 text-right font-medium">{peso(m.total)}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
