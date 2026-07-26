import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut, Store } from 'lucide-react'
import { toast } from 'sonner'
import { useAuthStore } from '@/store/authStore'
import { logout as apiLogout } from '@/lib/auth'

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
      </header>
      <main className="flex-1 mx-auto w-full max-w-5xl px-4 py-6">{children}</main>
    </div>
  )
}
