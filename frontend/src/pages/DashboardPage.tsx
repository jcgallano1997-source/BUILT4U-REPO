import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, ArrowDownRight, ArrowUpRight, Download, Wallet } from 'lucide-react'
import { fetchMe } from '@/lib/auth'
import { useAuthStore } from '@/store/authStore'
import { getSalesOverview } from '@/lib/reports'
import type { SalesOverview } from '@/lib/reports'
import { listItems } from '@/lib/inventory'
import type { Item } from '@/lib/inventory'
import { listReceivables } from '@/lib/arap'
import type { Receivable } from '@/lib/arap'

const peso = (n: number) => '₱' + Math.round(n || 0).toLocaleString('en-US')
const has = (mods: string[], m: string) => mods.includes(m)

function greeting(): string {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 18) return 'Good afternoon'
  return 'Good evening'
}

/** Build a fixed 7-day scaffold (oldest→today) so the chart always has 7 bars. */
function last7(): { date: string; label: string; isToday: boolean }[] {
  const out: { date: string; label: string; isToday: boolean }[] = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    out.push({
      date: d.toISOString().slice(0, 10),
      label: d.toLocaleDateString('en-US', { weekday: 'short' }),
      isToday: i === 0,
    })
  }
  return out
}

function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-2xl border border-slate-200/70 bg-white p-5 ${className}`}>{children}</div>
}

function Kpi({ label, value, delta }: { label: string; value: string; delta?: { up: boolean; text: string } }) {
  return (
    <Card className="!p-[18px]">
      <div className="mb-2.5 text-[12.5px] font-semibold text-slate-500">{label}</div>
      <div className="num text-[28px] font-bold leading-none tracking-[-0.02em]">{value}</div>
      {delta && (
        <div
          className={`mt-3 inline-flex items-center gap-1 rounded-md px-2 py-[3px] text-xs font-semibold ${
            delta.up ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
          }`}
        >
          {delta.up ? <ArrowUpRight size={13} /> : <ArrowDownRight size={13} />} {delta.text}
        </div>
      )}
    </Card>
  )
}

export default function DashboardPage() {
  const storeUser = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const site = useAuthStore((s) => s.site)
  const modules = storeUser?.modules ?? []

  const [overview, setOverview] = useState<SalesOverview | null>(null)
  const [items, setItems] = useState<Item[] | null>(null)
  const [receivables, setReceivables] = useState<Receivable[] | null>(null)

  const showSales = has(modules, 'SALES_REPORTS')
  const showInv = has(modules, 'INVENTORY')
  const showAr = has(modules, 'RECEIVABLES')

  useEffect(() => {
    fetchMe().then(setUser).catch(() => {})
  }, [setUser])

  useEffect(() => {
    const days = last7()
    if (showSales) {
      getSalesOverview(days[0].date, days[6].date).then(setOverview).catch(() => setOverview(null))
    }
    if (showInv) {
      listItems({}).then(setItems).catch(() => setItems([]))
    }
    if (showAr) {
      listReceivables({}).then((p) => setReceivables(p.content)).catch(() => setReceivables([]))
    }
  }, [showSales, showInv, showAr])

  const days = useMemo(last7, [])

  // KPIs from the last 7 days; "today" = the final scaffold day.
  const byDate = useMemo(() => {
    const m = new Map<string, { net: number; count: number }>()
    overview?.byDay?.forEach((d) => m.set(d.date, { net: d.net, count: d.count }))
    return m
  }, [overview])
  const today = byDate.get(days[6].date) ?? { net: 0, count: 0 }
  const avgBasket = today.count > 0 ? today.net / today.count : 0
  const chartMax = Math.max(1, ...days.map((d) => byDate.get(d.date)?.net ?? 0))

  const modeMax = Math.max(1, ...(overview?.byMode?.map((m) => m.total) ?? [1]))

  const lowStock = (items ?? [])
    .filter((i) => i.active && i.stockLevel !== 'OK')
    .sort((a, b) => a.quantity - b.quantity)
    .slice(0, 6)

  const arOpen = (receivables ?? []).filter((r) => r.status === 'OPEN' || r.status === 'PARTIAL')
  const arOutstanding = arOpen.reduce((s, r) => s + r.balance, 0)
  const arOverdue = arOpen.filter((r) => r.overdue).reduce((s, r) => s + r.balance, 0)

  const firstName = (storeUser?.fullName ?? 'there').split(/\s+/)[0]
  const dateLabel = new Date().toLocaleDateString('en-US', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <div className="mx-auto max-w-[1200px] space-y-[18px]">
      {/* Header */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-[25px] font-extrabold tracking-[-0.02em]">
            {greeting()}, {firstName}
          </h1>
          <p className="mt-0.5 text-[13.5px] text-slate-500">
            {site?.name ?? 'Your branch'} · {dateLabel}
          </p>
        </div>
        {showSales && (
          <Link
            to="/reports"
            className="inline-flex items-center gap-2 rounded-[10px] bg-navy px-3.5 py-2 text-[13px] font-semibold text-white hover:opacity-90"
          >
            <Download size={16} /> Reports
          </Link>
        )}
      </div>

      {/* KPI row */}
      {showSales && (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <Kpi label="Net sales · today" value={peso(today.net)} />
          <Kpi label="Transactions · today" value={String(today.count)} />
          <Kpi label="Avg. basket · today" value={peso(avgBasket)} />
          <Kpi label="Net sales · 7 days" value={peso(overview?.netSales ?? 0)} />
        </div>
      )}

      {/* Chart + payment mix */}
      {showSales && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1.7fr_1fr]">
          <Card>
            <div className="mb-5 flex items-center justify-between">
              <div className="text-[14.5px] font-bold">Sales — last 7 days</div>
              <div className="num text-xs text-slate-500">{peso(overview?.netSales ?? 0)} total</div>
            </div>
            <div className="flex h-[180px] items-end gap-3">
              {days.map((d) => {
                const net = byDate.get(d.date)?.net ?? 0
                const h = Math.max(6, Math.round((net / chartMax) * 150))
                return (
                  <div key={d.date} className="flex h-full flex-1 flex-col items-center justify-end gap-2">
                    <div
                      className={`w-full max-w-[40px] rounded-t-[7px] ${d.isToday ? 'bg-blue-600 shadow-lg shadow-blue-600/40' : 'bg-blue-100'}`}
                      style={{ height: `${h}px` }}
                      title={`${d.label}: ${peso(net)}`}
                    />
                    <span className={`text-[11px] ${d.isToday ? 'font-bold text-blue-700' : 'text-slate-400'}`}>{d.label}</span>
                  </div>
                )
              })}
            </div>
          </Card>

          <Card>
            <div className="mb-4 text-[14.5px] font-bold">Payment mix · 7 days</div>
            <div className="flex flex-col gap-3">
              {(overview?.byMode ?? []).length === 0 && <div className="text-[13px] text-slate-400">No sales in range.</div>}
              {(overview?.byMode ?? []).slice(0, 6).map((m) => (
                <div key={m.mode}>
                  <div className="mb-1.5 flex justify-between text-[12.5px]">
                    <span className="font-semibold text-slate-700">{m.mode}</span>
                    <span className="num text-slate-500">{peso(m.total)}</span>
                  </div>
                  <div className="h-[7px] overflow-hidden rounded bg-slate-100">
                    <div className="h-full rounded bg-blue-500" style={{ width: `${Math.max(4, (m.total / modeMax) * 100)}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}

      {/* Low stock + receivables */}
      {(showInv || showAr) && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {showInv && (
            <Card>
              <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center gap-2 text-[14.5px] font-bold">
                  <AlertTriangle size={17} className="text-amber-600" /> Low-stock radar
                </div>
                <Link to="/inventory" className="text-[12.5px] font-semibold text-blue-600 hover:underline">View all</Link>
              </div>
              <div className="flex flex-col gap-2.5">
                {lowStock.length === 0 && <div className="text-[13px] text-slate-400">Everything's well stocked. 🎉</div>}
                {lowStock.map((it) => {
                  const crit = it.stockLevel === 'CRITICAL'
                  return (
                    <div key={it.id} className="flex items-center gap-3">
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-[13.5px] font-semibold">{it.name}</div>
                        <div className="text-[11.5px] text-slate-400">
                          {it.code} · reorder at {crit ? it.critical ?? '—' : it.warning ?? '—'}
                        </div>
                      </div>
                      <span className={`num text-[13px] font-semibold ${crit ? 'text-red-700' : 'text-amber-700'}`}>{it.quantity}</span>
                      <span
                        className={`rounded-md px-2 py-[3px] text-[11px] font-bold ${crit ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-700'}`}
                      >
                        {crit ? 'Critical' : 'Low'}
                      </span>
                    </div>
                  )
                })}
              </div>
            </Card>
          )}

          {showAr && (
            <Card>
              <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center gap-2 text-[14.5px] font-bold">
                  <Wallet size={17} className="text-blue-600" /> Receivables
                </div>
                <Link to="/receivables" className="text-[12.5px] font-semibold text-blue-600 hover:underline">Collect</Link>
              </div>
              <div className="num text-[28px] font-bold tracking-[-0.02em]">{peso(arOutstanding)}</div>
              <div className="mt-1 flex flex-wrap items-center gap-2.5">
                <span className="text-[12px] text-slate-400">outstanding · {arOpen.length} accounts</span>
                {arOverdue > 0 && (
                  <span className="num rounded-md bg-red-50 px-2 py-0.5 text-[11px] font-bold text-red-700">
                    {peso(arOverdue)} overdue
                  </span>
                )}
              </div>
              {arOpen.length > 0 && (
                <div className="mt-4 flex flex-col gap-2.5">
                  {arOpen.slice(0, 4).map((r) => (
                    <div key={r.id} className="flex items-center justify-between text-[12.5px]">
                      <span className="truncate text-slate-600">{r.customerName}</span>
                      <span className={`num font-semibold ${r.overdue ? 'text-red-700' : 'text-slate-700'}`}>{peso(r.balance)}</span>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          )}
        </div>
      )}

    </div>
  )
}
