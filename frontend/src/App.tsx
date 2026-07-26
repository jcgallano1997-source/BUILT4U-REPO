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
      <Route path="/admin/sites" element={<Protected><SitesPage /></Protected>} />
      <Route path="/admin/users" element={<Protected><UsersPage /></Protected>} />
      <Route path="/admin/roles" element={<Protected><RolesPage /></Protected>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
