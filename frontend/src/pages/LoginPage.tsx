import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { LogIn, Store } from 'lucide-react'
import type { AxiosError } from 'axios'
import { fetchSites, login } from '@/lib/auth'
import { useAuthStore } from '@/store/authStore'
import type { Site } from '@/store/authStore'

const schema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
  siteCode: z.string().min(1, 'Select a site'),
})
type FormValues = z.infer<typeof schema>

function errMessage(e: unknown, fallback: string): string {
  const ax = e as AxiosError<{ message?: string }>
  return ax.response?.data?.message ?? fallback
}

export default function LoginPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const [sites, setSites] = useState<Site[]>([])
  const [loadingSites, setLoadingSites] = useState(false)

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '', siteCode: '' },
  })

  async function loadSites() {
    const username = getValues('username').trim()
    if (!username) return
    setLoadingSites(true)
    try {
      const list = await fetchSites(username)
      setSites(list)
      // Auto-select when there's exactly one site.
      setValue('siteCode', list.length === 1 ? list[0].code : '')
    } catch {
      setSites([])
    } finally {
      setLoadingSites(false)
    }
  }

  async function onSubmit(values: FormValues) {
    try {
      const session = await login(values)
      setSession(session)
      toast.success(`Welcome, ${session.user.fullName}`)
      navigate(session.user.mustChangePassword ? '/change-password' : '/', { replace: true })
    } catch (e) {
      toast.error(errMessage(e, 'Login failed'))
    }
  }

  return (
    <div className="min-h-full grid place-items-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex items-center justify-center gap-2 text-slate-800">
          <Store className="text-indigo-600" />
          <span className="text-lg font-semibold">Built4U POS</span>
        </div>

        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Username</label>
            <input
              autoFocus
              {...register('username')}
              onBlur={loadSites}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              placeholder="admin"
            />
            {errors.username && <p className="mt-1 text-xs text-red-600">{errors.username.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Site {loadingSites && <span className="text-slate-400">(loading…)</span>}
            </label>
            <select
              {...register('siteCode')}
              onFocus={() => { if (sites.length === 0) loadSites() }}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm bg-white outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">{sites.length ? 'Select a site…' : 'Enter username first'}</option>
              {sites.map((s) => (
                <option key={s.id} value={s.code}>{s.name} ({s.code})</option>
              ))}
            </select>
            {errors.siteCode && <p className="mt-1 text-xs text-red-600">{errors.siteCode.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
            <input
              type="password"
              {...register('password')}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              placeholder="••••••••"
            />
            {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full inline-flex items-center justify-center gap-2 rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
          >
            <LogIn size={16} /> {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="mt-4 text-center text-xs text-slate-400">Single-business POS · site-scoped access</p>
      </div>
    </div>
  )
}
