import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore } from '@/store/authStore'

/** Guards authed routes. Redirects to /login when there's no session, and to
 *  /change-password when the user must rotate their password first. */
export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { accessToken, user } = useAuthStore()
  const location = useLocation()

  if (!accessToken) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (user?.mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />
  }

  return <>{children}</>
}
