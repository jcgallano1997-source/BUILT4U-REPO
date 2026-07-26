import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import ProtectedRoute from '@/components/ProtectedRoute'
import AppLayout from '@/components/AppLayout'
import LoginPage from '@/pages/LoginPage'
import DashboardPage from '@/pages/DashboardPage'
import ChangePasswordPage from '@/pages/ChangePasswordPage'
import SitesPage from '@/pages/SitesPage'
import UsersPage from '@/pages/UsersPage'
import RolesPage from '@/pages/RolesPage'
import InventoryPage from '@/pages/InventoryPage'
import CategoriesPage from '@/pages/CategoriesPage'
import LocationsPage from '@/pages/LocationsPage'
import UomsPage from '@/pages/UomsPage'
import PosPage from '@/pages/PosPage'
import SalesPage from '@/pages/SalesPage'
import ShiftsPage from '@/pages/ShiftsPage'
import CustomersPage from '@/pages/CustomersPage'
import SuppliersPage from '@/pages/SuppliersPage'
import PaymentModesPage from '@/pages/PaymentModesPage'
import PurchaseOrdersPage from '@/pages/PurchaseOrdersPage'
import GoodsReceiptsPage from '@/pages/GoodsReceiptsPage'
import PoApproversPage from '@/pages/PoApproversPage'
import ReceivablesPage from '@/pages/ReceivablesPage'
import PayablesPage from '@/pages/PayablesPage'
import StockTransfersPage from '@/pages/StockTransfersPage'
import StockTransferPolicyPage from '@/pages/StockTransferPolicyPage'
import VouchersPage from '@/pages/VouchersPage'
import LoyaltyConfigPage from '@/pages/LoyaltyConfigPage'
import LoyaltyRewardsPage from '@/pages/LoyaltyRewardsPage'
import ReportsPage from '@/pages/ReportsPage'

function Protected({ children }: { children: ReactNode }) {
  return (
    <ProtectedRoute>
      <AppLayout>{children}</AppLayout>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route path="/change-password" element={<Protected><ChangePasswordPage /></Protected>} />
      <Route path="/" element={<Protected><DashboardPage /></Protected>} />
      <Route path="/pos" element={<Protected><PosPage /></Protected>} />
      <Route path="/sales" element={<Protected><SalesPage /></Protected>} />
      <Route path="/shifts" element={<Protected><ShiftsPage /></Protected>} />
      <Route path="/inventory" element={<Protected><InventoryPage /></Protected>} />
      <Route path="/categories" element={<Protected><CategoriesPage /></Protected>} />
      <Route path="/locations" element={<Protected><LocationsPage /></Protected>} />
      <Route path="/units" element={<Protected><UomsPage /></Protected>} />
      <Route path="/customers" element={<Protected><CustomersPage /></Protected>} />
      <Route path="/suppliers" element={<Protected><SuppliersPage /></Protected>} />
      <Route path="/purchase-orders" element={<Protected><PurchaseOrdersPage /></Protected>} />
      <Route path="/goods-receipts" element={<Protected><GoodsReceiptsPage /></Protected>} />
      <Route path="/receivables" element={<Protected><ReceivablesPage /></Protected>} />
      <Route path="/payables" element={<Protected><PayablesPage /></Protected>} />
      <Route path="/stock-transfers" element={<Protected><StockTransfersPage /></Protected>} />
      <Route path="/admin/stock-transfer-policy" element={<Protected><StockTransferPolicyPage /></Protected>} />
      <Route path="/admin/vouchers" element={<Protected><VouchersPage /></Protected>} />
      <Route path="/admin/loyalty-config" element={<Protected><LoyaltyConfigPage /></Protected>} />
      <Route path="/admin/loyalty-rewards" element={<Protected><LoyaltyRewardsPage /></Protected>} />
      <Route path="/reports" element={<Protected><ReportsPage /></Protected>} />
      <Route path="/admin/po-approvers" element={<Protected><PoApproversPage /></Protected>} />
      <Route path="/admin/payment-modes" element={<Protected><PaymentModesPage /></Protected>} />
      <Route path="/admin/sites" element={<Protected><SitesPage /></Protected>} />
      <Route path="/admin/users" element={<Protected><UsersPage /></Protected>} />
      <Route path="/admin/roles" element={<Protected><RolesPage /></Protected>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
