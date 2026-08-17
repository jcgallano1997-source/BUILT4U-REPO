import { useEffect, useRef, useState } from 'react'
import type { ComponentType, ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  AlertTriangle, ArrowLeftRight, BarChart3, Boxes, ClipboardCheck, ClipboardList, Clock,
  CreditCard, FileBadge, Gift, GitBranch, History, Keyboard, KeyRound, Landmark, LayoutDashboard, LogOut, Mail, MapPin, Menu,
  Package, PackagePlus, Percent, Plus, Receipt, Route, Ruler, Shield, ShoppingCart, Sparkles, Store,
  Tags, Ticket, TrendingUp, Truck, UserRound, Users, Wallet, X,
} from 'lucide-react'
import { toast } from 'sonner'
import { useAuthStore } from '@/store/authStore'
import { logout as apiLogout } from '@/lib/auth'

type NavItem = { to: string; label: string; icon: ComponentType<{ size?: number; className?: string }>; module: string | string[] | null; end?: boolean }
type NavGroup = { title: string | null; items: NavItem[] }

/** All 28 destinations, grouped into 7 tidy sections (was a 28-item overflow bar). */
const GROUPS: NavGroup[] = [
  { title: null, items: [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard, module: null, end: true },
  ] },
  { title: 'Sell', items: [
    { to: '/pos', label: 'Point of sale', icon: ShoppingCart, module: 'POS' },
    { to: '/sales', label: 'Sales', icon: Receipt, module: 'SALES' },
    { to: '/shifts', label: 'Shifts', icon: ClipboardList, module: 'SHIFTS' },
    { to: '/customers', label: 'Customers', icon: UserRound, module: 'CUSTOMERS' },
  ] },
  { title: 'Catalog', items: [
    { to: '/inventory', label: 'Inventory', icon: Package, module: 'INVENTORY' },
    { to: '/categories', label: 'Categories', icon: Tags, module: 'CATEGORIES' },
    { to: '/locations', label: 'Locations', icon: MapPin, module: 'LOCATIONS' },
    { to: '/units', label: 'Units', icon: Ruler, module: 'UOMS' },
  ] },
  { title: 'Procurement', items: [
    { to: '/suppliers', label: 'Suppliers', icon: Truck, module: 'SUPPLIERS' },
    { to: '/purchase-orders', label: 'Purchase orders', icon: ClipboardCheck, module: 'PURCHASE_ORDERS' },
    { to: '/goods-receipts', label: 'Receiving', icon: PackagePlus, module: 'GOODS_RECEIPTS' },
    { to: '/stock-transfers', label: 'Transfers', icon: ArrowLeftRight, module: 'STOCK_TRANSFER' },
  ] },
  { title: 'Finance', items: [
    { to: '/receivables', label: 'Receivables', icon: Wallet, module: 'RECEIVABLES' },
    { to: '/payables', label: 'Payables', icon: Landmark, module: 'PAYABLES' },
  ] },
  { title: 'Reports', items: [
    { to: '/reports', label: 'Sales report', icon: BarChart3, module: 'SALES_REPORTS', end: true },
    { to: '/reports/inventory-snapshot', label: 'Inventory snapshot', icon: Package, module: 'INVENTORY_SNAPSHOT' },
    { to: '/reports/inventory-valuation', label: 'Inventory valuation', icon: Tags, module: 'INVENTORY_VALUATION' },
    { to: '/reports/inventory-movement', label: 'Inventory movement', icon: ArrowLeftRight, module: 'INVENTORY_MOVEMENT' },
    { to: '/reports/reorder', label: 'Reorder suggestions', icon: AlertTriangle, module: 'REORDER_REPORT' },
    { to: '/reports/dead-stock', label: 'Dead stock', icon: Boxes, module: 'DEAD_STOCK_REPORT' },
    { to: '/reports/profit-margin', label: 'Profit & margin', icon: TrendingUp, module: 'PROFIT_REPORT' },
    { to: '/reports/sales-by-cashier', label: 'Sales by cashier', icon: UserRound, module: 'SALES_ANALYTICS' },
    { to: '/reports/sales-by-hour', label: 'Sales by hour', icon: Clock, module: 'SALES_ANALYTICS' },
    { to: '/reports/customer-purchases', label: 'Customer purchases', icon: Users, module: 'CUSTOMER_REPORT' },
    { to: '/reports/shift-history', label: 'Shift history', icon: History, module: 'SHIFT_HISTORY_REPORT' },
    { to: '/reports/discounts-overrides', label: 'Discounts & overrides', icon: Percent, module: 'DISCOUNTS_REPORT' },
    { to: '/reports/goods-receipts', label: 'Goods receive', icon: PackagePlus, module: 'GOODS_RECEIPTS_REPORT' },
    { to: '/reports/purchase-orders', label: 'Purchase order', icon: ClipboardCheck, module: 'PURCHASE_ORDERS_REPORT' },
    { to: '/reports/stock-transfers', label: 'Stock transfers', icon: Route, module: 'STOCK_TRANSFER_REPORT' },
    { to: '/reports/receivables', label: 'Accounts receivable', icon: Wallet, module: 'RECEIVABLES_REPORT' },
    { to: '/reports/payables', label: 'Accounts payable', icon: Landmark, module: 'PAYABLES_REPORT' },
  ] },
  { title: 'Promotions', items: [
    { to: '/admin/vouchers', label: 'Vouchers', icon: Ticket, module: 'VOUCHERS' },
    { to: '/admin/loyalty-config', label: 'Loyalty', icon: Sparkles, module: 'LOYALTY_CONFIG' },
    { to: '/admin/loyalty-rewards', label: 'Rewards', icon: Gift, module: 'LOYALTY_REWARDS' },
  ] },
  { title: 'Admin', items: [
    { to: '/admin/payment-modes', label: 'Payment modes', icon: CreditCard, module: 'PAYMENT_MODES' },
    { to: '/admin/po-approvers', label: 'PO approvers', icon: GitBranch, module: 'PO_APPROVERS' },
    { to: '/admin/stock-transfer-policy', label: 'Transfer policy', icon: Route, module: 'STOCK_TRANSFER_POLICY' },
    { to: '/admin/doc-settings', label: 'Documents', icon: FileBadge, module: ['DOC_SETTINGS', 'PDF_CONFIG', 'RECEIPT_CONFIG'] },
    { to: '/admin/report-email', label: 'Report email', icon: Mail, module: 'EMAIL_CONFIG' },
    { to: '/admin/sites', label: 'Sites', icon: Store, module: 'SITES' },
    { to: '/admin/users', label: 'Users', icon: Users, module: 'USERS' },
    { to: '/admin/roles', label: 'Roles', icon: Shield, module: 'ROLES' },
    { to: '/admin/audit-log', label: 'Audit log', icon: History, module: 'AUDIT_LOG' },
    { to: '/admin/error-log', label: 'Error log', icon: AlertTriangle, module: 'ERROR_LOG' },
  ] },
]

function initials(name: string | undefined): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || name[0].toUpperCase()
}

/** Authenticated shell: dark grouped sidebar + white topbar, per the redesign. */
export default function AppLayout({ children }: { children: ReactNode }) {
  const { user, site, refreshToken, clear } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const [open, setOpen] = useState(false) // mobile off-canvas sidebar
  const [showHelp, setShowHelp] = useState(false)

  async function handleLogout() {
    try {
      await apiLogout(refreshToken)
    } catch {
      // best-effort; clear locally regardless
    }
    clear()
    toast.success('Signed out')
    navigate('/login', { replace: true })
  }

  const modules = user?.modules ?? []
  const canSee = (m: string | string[] | null) =>
    m === null || (Array.isArray(m) ? m.some((x) => modules.includes(x)) : modules.includes(m))
  const groups = GROUPS
    .map((g) => ({ ...g, items: g.items.filter((i) => canSee(i.module)) }))
    .filter((g) => g.items.length > 0)

  // ── Keyboard shortcuts (efficiency) ───────────────────────────────────────
  // "g then <key>" jumps to a screen; "/" or Ctrl/Cmd+K focuses search;
  // "n" starts a new sale; "?" shows help. Ignored while typing in a field.
  const modulesRef = useRef(modules)
  modulesRef.current = modules
  useEffect(() => {
    // [key, path, required module (null = always)]
    const GOTO: Record<string, [string, string | null]> = {
      d: ['/', null], p: ['/pos', 'POS'], s: ['/sales', 'SALES'], i: ['/inventory', 'INVENTORY'],
      r: ['/reports', 'SALES_REPORTS'], h: ['/shifts', 'SHIFTS'], c: ['/customers', 'CUSTOMERS'],
    }
    const allowed = (m: string | null) => m === null || modulesRef.current.includes(m)
    let awaitingG = false
    let gTimer = 0
    const isTyping = (el: EventTarget | null) => {
      const n = el as HTMLElement | null
      return !!n && (n.tagName === 'INPUT' || n.tagName === 'TEXTAREA' || n.tagName === 'SELECT' || n.isContentEditable)
    }
    function onKey(e: KeyboardEvent) {
      if (isTyping(e.target)) { if (e.key === 'Escape') (e.target as HTMLElement).blur() ; return }
      if (e.ctrlKey || e.metaKey || e.altKey) return
      if (e.key === 'Escape') { setShowHelp(false); return }
      if (e.key === '?') { e.preventDefault(); setShowHelp((s) => !s); return }
      if (awaitingG) {
        awaitingG = false
        const hit = GOTO[e.key.toLowerCase()]
        if (hit && allowed(hit[1])) { e.preventDefault(); navigate(hit[0]) }
        return
      }
      if (e.key.toLowerCase() === 'g') { awaitingG = true; window.clearTimeout(gTimer); gTimer = window.setTimeout(() => { awaitingG = false }, 1200); return }
      if (e.key.toLowerCase() === 'n') {
        // Click this page's primary "New …" / "Add …" action, wherever it lives.
        const el = [...document.querySelectorAll('main button, main a')].find((node) => {
          const t = (node.textContent || '').trim()
          return /^(New|Add)\b/i.test(t) && !(node as HTMLButtonElement).disabled && (node as HTMLElement).offsetParent !== null
        }) as HTMLElement | undefined
        if (el) { e.preventDefault(); el.click() }
        else if (modulesRef.current.includes('POS')) { e.preventDefault(); navigate('/pos') }
      }
    }
    window.addEventListener('keydown', onKey)
    return () => { window.removeEventListener('keydown', onKey); window.clearTimeout(gTimer) }
  }, [navigate])

  // Stamp an "N" hint onto this page's primary New/Add button so the shortcut is
  // discoverable. Appends a trailing chip (React-safe) and keeps it applied as the
  // page re-renders. Skips buttons that already show a key hint (e.g. POS New sale).
  useEffect(() => {
    const HINT = /^(N|F\d|Enter|Esc)(\s*\/\s*(N|F\d))?$/i
    function decorate() {
      const el = [...document.querySelectorAll('main button, main a')].find((node) => {
        const t = (node.textContent || '').replace(/\s+/g, ' ').trim()
        return /^(New|Add)\b/i.test(t) && (node as HTMLElement).offsetParent !== null
      }) as HTMLElement | undefined
      if (!el) return
      if (el.querySelector('[data-nkey]')) return
      if ([...el.querySelectorAll('span')].some((s) => HINT.test((s.textContent || '').trim()))) return
      const chip = document.createElement('span')
      chip.setAttribute('data-nkey', '')
      chip.textContent = 'N'
      chip.style.cssText = 'margin-left:6px;border-radius:4px;background:rgba(255,255,255,.22);padding:0 5px;font-size:11px;font-weight:600;line-height:1.5;'
      el.appendChild(chip)
    }
    decorate()
    const main = document.querySelector('main')
    const obs = main ? new MutationObserver(() => decorate()) : null
    if (main && obs) obs.observe(main, { childList: true, subtree: true })
    return () => obs?.disconnect()
  }, [location.pathname])

  const sidebar = (
    <aside className="flex h-full w-[252px] flex-none flex-col bg-navy text-white">
      <div className="flex items-center gap-2.5 px-4 pb-4 pt-4">
        <img src="/favicon.svg" width={30} height={30} className="rounded-lg" alt="Built4U" />
        <div className="leading-none">
          <div className="text-[15px] font-extrabold">Built4U</div>
          <div className="text-[10.5px] tracking-wider text-slate-500">POS · {site?.code ?? '—'}</div>
        </div>
        <button
          onClick={() => setOpen(false)}
          className="ml-auto rounded-md p-1 text-slate-400 hover:bg-white/5 lg:hidden"
          aria-label="Close menu"
        >
          <X size={18} />
        </button>
      </div>

      <nav className="b4u-scroll flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 pb-3">
        {groups.map((g, gi) => (
          <div key={g.title ?? `g${gi}`}>
            {g.title && (
              <div className="px-3 pb-1.5 pt-3.5 text-[10px] font-bold uppercase tracking-[0.1em] text-slate-500">
                {g.title}
              </div>
            )}
            {g.items.map((n) => {
              const Icon = n.icon
              return (
                <NavLink
                  key={n.to}
                  to={n.to}
                  end={n.end}
                  onClick={() => setOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-lg px-3 py-2 text-[13.5px] transition-colors ${
                      isActive
                        ? 'bg-blue-500/15 font-semibold text-white'
                        : 'font-medium text-slate-400 hover:bg-white/5 hover:text-slate-200'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <Icon size={18} className={isActive ? 'text-blue-400' : 'text-current'} />
                      {n.label}
                    </>
                  )}
                </NavLink>
              )
            })}
          </div>
        ))}
      </nav>

      <div className="m-3 mt-auto flex items-center gap-2.5 rounded-xl bg-navy-800 p-2.5">
        <div className="flex h-[34px] w-[34px] flex-none items-center justify-center rounded-lg bg-blue-900 text-[13px] font-bold text-blue-200">
          {initials(user?.fullName)}
        </div>
        <div className="min-w-0 leading-tight">
          <div className="truncate text-[13px] font-semibold text-white">{user?.fullName ?? 'User'}</div>
          <div className="text-[11px] text-slate-500">{user?.roles?.[0] ?? 'Member'}</div>
        </div>
        <NavLink
          to="/change-password"
          onClick={() => setOpen(false)}
          className="ml-auto flex-none rounded-md p-1.5 text-slate-500 hover:bg-white/5 hover:text-white"
          title="Change password"
          aria-label="Change password"
        >
          <KeyRound size={16} />
        </NavLink>
        <button
          onClick={handleLogout}
          className="flex-none rounded-md p-1.5 text-slate-500 hover:bg-white/5 hover:text-white"
          title="Sign out"
          aria-label="Sign out"
        >
          <LogOut size={17} />
        </button>
      </div>
    </aside>
  )

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Desktop sidebar */}
      <div className="hidden lg:block">{sidebar}</div>

      {/* Mobile off-canvas sidebar */}
      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-navy/60 backdrop-blur-sm" onClick={() => setOpen(false)} />
          <div className="absolute inset-y-0 left-0 shadow-2xl">{sidebar}</div>
        </div>
      )}

      {/* Main column */}
      <div className="flex min-w-0 flex-1 flex-col bg-canvas">
        <header className="flex h-[62px] flex-none items-center gap-3 border-b border-slate-200 bg-white px-4 md:px-6">
          <button
            onClick={() => setOpen(true)}
            className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 lg:hidden"
            aria-label="Open menu"
          >
            <Menu size={20} />
          </button>

          {site && (
            <div className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-[7px] text-[13px] font-semibold text-slate-700">
              <Store size={16} className="text-blue-600" />
              <span className="hidden sm:inline">{site.code} — </span>{site.name}
            </div>
          )}

          <div className="ml-auto flex items-center gap-3">
            {canSee('POS') && (
              <NavLink
                to="/pos"
                className="hidden items-center gap-2 rounded-lg bg-blue-600 px-3.5 py-2 text-[13px] font-semibold text-white shadow-sm shadow-blue-600/30 hover:bg-blue-700 sm:inline-flex"
              >
                <Plus size={16} /> New sale
              </NavLink>
            )}
            <button
              onClick={() => setShowHelp(true)}
              className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-[12.5px] font-medium text-slate-500 hover:bg-slate-50"
              title="Keyboard shortcuts (press ?)"
              aria-label="Keyboard shortcuts"
            >
              <Keyboard size={16} /> <span className="hidden md:inline">Shortcuts</span>
            </button>
            <div className="flex h-[34px] w-[34px] items-center justify-center rounded-lg bg-blue-50 text-[13px] font-bold text-blue-700">
              {initials(user?.fullName)}
            </div>
          </div>
        </header>

        <main className="b4u-scroll flex-1 overflow-y-auto px-4 py-6 md:px-8">{children}</main>
      </div>

      {showHelp && <ShortcutsHelp onClose={() => setShowHelp(false)} />}
    </div>
  )
}

const SHORTCUT_GROUPS: { title: string; items: { keys: string[]; label: string }[] }[] = [
  { title: 'Anywhere', items: [
    { keys: ['G', 'D'], label: 'Go to Dashboard' },
    { keys: ['G', 'P'], label: 'Go to Point of sale' },
    { keys: ['G', 'S'], label: 'Go to Sales' },
    { keys: ['G', 'I'], label: 'Go to Inventory' },
    { keys: ['G', 'R'], label: 'Go to Reports' },
    { keys: ['G', 'H'], label: 'Go to Shifts' },
    { keys: ['G', 'C'], label: 'Go to Customers' },
    { keys: ['N'], label: 'New / add on this screen (new sale, new item, new user…)' },
    { keys: ['?'], label: 'Show this help' },
    { keys: ['Esc'], label: 'Close / leave field' },
  ] },
  { title: 'On the Point of sale screen', items: [
    { keys: ['F2'], label: 'Focus item search / scan' },
    { keys: ['F4'], label: 'Hold sale' },
    { keys: ['F8'], label: 'Recall a held sale' },
    { keys: ['F9'], label: 'Charge (checkout) — or start the next sale after one completes' },
    { keys: ['Enter'], label: 'Charge — from the Cash tendered box' },
    { keys: ['N'], label: 'New sale (after a sale completes)' },
  ] },
]

function ShortcutsHelp({ onClose }: { onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-navy/50 p-4 backdrop-blur-sm" onClick={onClose}>
      <div className="w-full max-w-md rounded-2xl bg-white p-5 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-[15px] font-bold text-slate-800">Keyboard shortcuts</h2>
          <button onClick={onClose} className="rounded-md p-1 text-slate-400 hover:bg-slate-100"><X size={18} /></button>
        </div>
        <div className="space-y-3">
          {SHORTCUT_GROUPS.map((g) => (
            <div key={g.title}>
              <div className="mb-1 text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">{g.title}</div>
              <div className="space-y-1.5">
                {g.items.map((s) => (
                  <div key={s.label + s.keys.join()} className="flex items-center justify-between text-[13px]">
                    <span className="text-slate-600">{s.label}</span>
                    <span className="flex gap-1">
                      {s.keys.map((k, i) => (
                        <kbd key={k + i} className="num rounded border border-slate-200 bg-slate-50 px-1.5 py-0.5 text-[11px] font-semibold text-slate-500">{k}</kbd>
                      ))}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
        <p className="mt-3 text-[11px] text-slate-400">Tip: “G then D” means press G, release, then D. Shortcuts pause while you’re typing in a field.</p>
      </div>
    </div>
  )
}
