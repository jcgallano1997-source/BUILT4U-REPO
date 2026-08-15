import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { ChevronDown, LogIn } from 'lucide-react'
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
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '', siteCode: '' },
  })

  const lastLoaded = useRef('')

  async function loadSites(name?: string) {
    const username = (name ?? getValues('username')).trim()
    if (!username) return
    lastLoaded.current = username
    setLoadingSites(true)
    try {
      const list = await fetchSites(username)
      setSites(list)
      setValue('siteCode', list.length === 1 ? list[0].code : '')
    } catch {
      setSites([])
    } finally {
      setLoadingSites(false)
    }
  }

  // Auto-load branches ~as the user finishes typing the username, so the Branch
  // dropdown is already populated by the time they reach it (blur/focus below
  // remain as immediate fallbacks).
  const username = watch('username')
  useEffect(() => {
    const name = username.trim()
    if (!name) { setSites([]); lastLoaded.current = ''; return }
    const t = setTimeout(() => { if (lastLoaded.current !== name) loadSites(name) }, 400)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [username])

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

  const field = 'w-full h-[46px] px-3.5 text-[14.5px] rounded-xl border-[1.5px] border-slate-200 bg-white outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-600/12'
  const labelCls = 'block text-[13px] font-semibold text-slate-700 mb-1.5'

  return (
    <div className="flex min-h-screen">
      {/* Brand panel */}
      <div className="relative hidden w-[46%] max-w-[620px] flex-col justify-between overflow-hidden bg-navy p-14 text-white lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-100"
          style={{
            backgroundImage:
              'linear-gradient(rgba(255,255,255,.045) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.045) 1px,transparent 1px)',
            backgroundSize: '38px 38px',
          }}
        />
        <div
          className="pointer-events-none absolute -right-32 -top-40 h-[520px] w-[520px] rounded-full"
          style={{ background: 'radial-gradient(circle,rgba(37,99,235,.55),transparent 62%)' }}
        />
        <div className="relative flex items-center gap-3">
          <img src="/favicon.svg" width={42} height={42} className="rounded-xl" alt="Built4U" />
          <span className="text-[19px] font-extrabold tracking-tight">Built4U POS</span>
        </div>
        <div className="relative">
          <div className="num mb-4 text-[11.5px] font-semibold uppercase tracking-[0.16em] text-blue-400">
            Point of sale for hardware
          </div>
          <h2 className="m-0 mb-4 max-w-[14ch] text-[40px] font-extrabold leading-[1.12] tracking-[-0.02em]">
            Run every branch from one clean counter.
          </h2>
          <p className="m-0 max-w-[40ch] text-[15px] leading-relaxed text-slate-400">
            Sell, restock, and reconcile across all your stores — with the reports and audit trail your accountant
            actually wants.
          </p>
          <div className="mt-9 flex gap-7">
            <div>
              <div className="num text-[26px] font-bold">45</div>
              <div className="mt-0.5 text-xs text-slate-500">modules</div>
            </div>
            <div className="w-px bg-white/10" />
            <div>
              <div className="num text-[26px] font-bold">Multi</div>
              <div className="mt-0.5 text-xs text-slate-500">branch ready</div>
            </div>
            <div className="w-px bg-white/10" />
            <div>
              <div className="num text-[26px] font-bold">₱</div>
              <div className="mt-0.5 text-xs text-slate-500">peso native</div>
            </div>
          </div>
        </div>
        <div className="relative text-[12.5px] text-slate-600">Single-business · site-scoped access · v2.0</div>
      </div>

      {/* Form panel */}
      <div className="flex flex-1 items-center justify-center bg-white px-6 py-10">
        <div className="w-[380px] max-w-full">
          <div className="mb-7 flex items-center gap-2.5 lg:hidden">
            <img src="/favicon.svg" width={36} height={36} className="rounded-lg" alt="Built4U" />
            <span className="text-lg font-extrabold">Built4U POS</span>
          </div>

          <h3 className="m-0 mb-1.5 text-[24px] font-extrabold tracking-[-0.02em]">Sign in to your register</h3>
          <p className="m-0 mb-7 text-[14px] text-slate-500">Enter your credentials to open the counter.</p>

          <form onSubmit={handleSubmit(onSubmit)}>
            <label className={labelCls}>Username</label>
            <input autoFocus {...register('username')} onBlur={() => loadSites()} className={`${field} mb-1`} placeholder="admin" />
            {errors.username && <p className="mb-2 text-xs text-red-600">{errors.username.message}</p>}

            <label className={`${labelCls} mt-4`}>
              Branch {loadingSites && <span className="font-normal text-slate-400">(loading…)</span>}
            </label>
            <div className="relative mb-1">
              <select
                {...register('siteCode')}
                onFocus={() => { if (sites.length === 0) loadSites() }}
                className={`${field} appearance-none pr-10`}
              >
                <option value="">{sites.length ? 'Select a branch…' : 'Enter username first'}</option>
                {sites.map((s) => (
                  <option key={s.id} value={s.code}>{s.name} ({s.code})</option>
                ))}
              </select>
              <ChevronDown size={18} className="pointer-events-none absolute right-3.5 top-3.5 text-slate-400" />
            </div>
            {errors.siteCode && <p className="mb-2 text-xs text-red-600">{errors.siteCode.message}</p>}

            <label className={`${labelCls} mt-4`}>Password</label>
            <input type="password" {...register('password')} className={`${field} mb-1`} placeholder="••••••••" />
            {errors.password && <p className="mb-2 text-xs text-red-600">{errors.password.message}</p>}

            <button
              type="submit"
              disabled={isSubmitting}
              className="mt-6 inline-flex h-12 w-full items-center justify-center gap-2.5 rounded-xl bg-blue-600 text-[15px] font-bold text-white shadow-lg shadow-blue-600/40 transition hover:bg-blue-700 disabled:opacity-60"
            >
              <LogIn size={18} /> {isSubmitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="mt-6 text-center text-[12.5px] text-slate-400">
            Protected by rate-limiting &amp; 5-attempt lockout.
          </p>
        </div>
      </div>
    </div>
  )
}
