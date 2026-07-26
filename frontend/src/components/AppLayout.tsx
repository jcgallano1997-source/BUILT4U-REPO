import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { AlertTriangle, ArrowLeftRight, BarChart3, ClipboardCheck, ClipboardList, CreditCard, FileBadge, Gift, GitBranch, History, Landmark, LayoutDashboard, LogOut, MapPin, Package, PackagePlus, Receipt, Route, Ruler, Shield, ShoppingCart, Sparkles, Store, Tags, Ticket, Truck, UserRound, Users, Wallet } from 'lucide-react'
import { toast } from 'sonner'
import { useAuthStore } from '@/store/authStore'
import { logout as apiLogout } from '@/lib/auth'

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, module: null },
  { to: '/pos', label: 'POS', icon: ShoppingCart, module: 'POS' },
  { to: '/sales', label: 'Sales', icon: Receipt, module: 'SALES' },
  { to: '/shifts', label: 'Shifts', icon: ClipboardList, module: 'SHIFTS' },
  { to: '/inventory', label: 'Inventory', icon: Package, module: 'INVENTORY' },
  { to: '/customers', label: 'Customers', icon: UserRound, module: 'CUSTOMERS' },
  { to: '/suppliers', label: 'Suppliers', icon: Truck, module: 'SUPPLIERS' },
  { to: '/purchase-orders', label: 'Purchase orders', icon: ClipboardCheck, module: 'PURCHASE_ORDERS' },
  { to: '/goods-receipts', label: 'Receiving', icon: PackagePlus, module: 'GOODS_RECEIPTS' },
  { to: '/receivables', label: 'Receivables', icon: Wallet, module: 'RECEIVABLES' },
  { to: '/payables', label: 'Payables', icon: Landmark, module: 'PAYABLES' },
  { to: '/stock-transfers', label: 'Transfers', icon: ArrowLeftRight, module: 'STOCK_TRANSFER' },
  { to: '/reports', label: 'Reports', icon: BarChart3, module: 'SALES_REPORTS' },
  { to: '/categories', label: 'Categories', icon: Tags, module: 'CATEGORIES' },
  { to: '/locations', label: 'Locations', icon: MapPin, module: 'LOCATIONS' },
  { to: '/units', label: 'Units', icon: Ruler, module: 'UOMS' },
  { to: '/admin/payment-modes', label: 'Payment modes', icon: CreditCard, module: 'PAYMENT_MODES' },
  { to: '/admin/po-approvers', label: 'PO approvers', icon: GitBranch, module: 'PO_APPROVERS' },
  { to: '/admin/stock-transfer-policy', label: 'Transfer policy', icon: Route, module: 'STOCK_TRANSFER_POLICY' },
  { to: '/admin/vouchers', label: 'Vouchers', icon: Ticket, module: 'VOUCHERS' },
  { to: '/admin/loyalty-config', label: 'Loyalty', icon: Sparkles, module: 'LOYALTY_CONFIG' },
  { to: '/admin/loyalty-rewards', label: 'Rewards', icon: Gift, module: 'LOYALTY_REWARDS' },
  { to: '/admin/doc-settings', label: 'Documents', icon: FileBadge, module: 'DOC_SETTINGS' },
  { to: '/admin/sites', label: 'Sites', icon: Store, module: 'SITES' },
  { to: '/admin/users', label: 'Users', icon: Users, module: 'USERS' },
  { to: '/admin/roles', label: 'Roles', icon: Shield, module: 'ROLES' },
  { to: '/admin/audit-log', label: 'Audit log', icon: History, module: 'AUDIT_LOG' },
  { to: '/admin/error-log', label: 'Error log', icon: AlertTriangle, module: 'AUDIT_LOG' },
] as const

/** Minimal authenticated shell: a header with the business/site + a logout button. */
export default function AppLayout({ children }: { children: ReactNode }) {
  const { user, site, refreshToken, clear } = useAuthStore()
  const navigate = useNavigate()

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
  const navItems = NAV.filter((n) => n.module === null || modules.includes(n.module))

  return (
    <div className="min-h-full flex flex-col">
      <header className="bg-white border-b border-slate-200">
        <div className="mx-auto max-w-5xl px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2 font-semibold text-slate-800">
            <Store size={18} className="text-indigo-600" />
            <span>Built4U POS</span>
            {site && <span className="text-slate-400 font-normal">· {site.name}</span>}
          </div>
          <div className="flex items-center gap-3 text-sm">
            {user && <span className="text-slate-500">{user.fullName}</span>}
            <button
              onClick={handleLogout}
              className="inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-slate-600 hover:bg-slate-100"
            >
              <LogOut size={15} /> Logout
            </button>
          </div>
        </div>
        <nav className="mx-auto max-w-5xl px-2 flex gap-1 overflow-x-auto">
          {navItems.map((n) => {
            const Icon = n.icon
            return (
              <NavLink
                key={n.to}
                to={n.to}
                end={n.to === '/'}
                className={({ isActive }) =>
                  `inline-flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm ${
                    isActive
                      ? 'border-indigo-600 text-indigo-700'
                      : 'border-transparent text-slate-500 hover:text-slate-700'
                  }`
                }
              >
                <Icon size={15} /> {n.label}
              </NavLink>
            )
          })}
        </nav>
      </header>
      <main className="flex-1 mx-auto w-full max-w-5xl px-4 py-6">{children}</main>
    </div>
  )
}
