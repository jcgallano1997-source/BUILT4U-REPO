import { useEffect, useState } from 'react'
import { KeyRound, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { fetchMe } from '@/lib/auth'
import { useAuthStore } from '@/store/authStore'
import type { User } from '@/store/authStore'

/** Protected landing page. Reads /api/auth/me to prove the session works. */
export default function DashboardPage() {
  const storeUser = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const [me, setMe] = useState<User | null>(storeUser)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchMe()
      .then((u) => { setMe(u); setUser(u) })
      .catch(() => setError('Could not load your profile.'))
  }, [setUser])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Dashboard</h1>
        <p className="text-sm text-slate-500">You are signed in. This confirms the auth slice end-to-end.</p>
      </div>

      {error && <div className="rounded-md bg-red-50 border border-red-200 text-red-700 text-sm px-3 py-2">{error}</div>}

      {me && (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="bg-white rounded-xl border border-slate-200 p-4">
            <div className="flex items-center gap-2 text-slate-700 font-medium mb-2">
              <ShieldCheck size={16} className="text-indigo-600" /> Profile
            </div>
            <dl className="text-sm text-slate-600 space-y-1">
              <div className="flex justify-between"><dt className="text-slate-400">Username</dt><dd>{me.username}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-400">Full name</dt><dd>{me.fullName}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-400">Email</dt><dd>{me.email ?? '—'}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-400">Roles</dt><dd>{me.roles.join(', ')}</dd></div>
            </dl>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-4">
            <div className="flex items-center gap-2 text-slate-700 font-medium mb-2">Modules ({me.modules.length})</div>
            <div className="flex flex-wrap gap-1.5">
              {me.modules.map((m) => (
                <span key={m} className="rounded bg-slate-100 text-slate-600 text-xs px-1.5 py-0.5">{m}</span>
              ))}
            </div>
          </div>
        </div>
      )}

      <Link
        to="/change-password"
        className="inline-flex items-center gap-1.5 text-sm text-indigo-600 hover:underline"
      >
        <KeyRound size={15} /> Change password
      </Link>
    </div>
  )
}
