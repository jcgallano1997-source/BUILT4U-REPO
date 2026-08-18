import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { ChevronDown, Lock, LogIn, ShieldCheck, User, Warehouse } from 'lucide-react'
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

  const field =
    'w-full h-[47px] pl-[42px] pr-3.5 text-[14.5px] rounded-xl border-[1.5px] border-slate-200 bg-white outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-600/12'
  const labelCls = 'block text-[13px] font-semibold text-slate-700 mb-1.5'
  const iconCls = 'pointer-events-none absolute left-[14px] top-[15px] text-slate-400'

  return (
    <div className="flex min-h-screen bg-white font-sans text-slate-900">
      {/* ── Brand panel ─────────────────────────────────────────────── */}
      <div className="relative hidden w-[53%] max-w-[760px] flex-none flex-col justify-between overflow-hidden bg-navy py-14 pl-24 pr-14 text-white lg:flex">
        {/* Blueprint grid + radial glow */}
        <div className="blueprint-grid pointer-events-none absolute inset-0" />
        <div
          className="pointer-events-none absolute -right-36 -top-40 h-[540px] w-[540px] rounded-full"
          style={{ background: 'radial-gradient(circle,rgba(37,99,235,.5),transparent 62%)' }}
        />

        {/* Measurement ruler */}
        <div className="pointer-events-none absolute inset-y-0 left-0 w-[46px] border-r border-white/10 bg-white/[0.02]">
          <div
            className="absolute inset-y-0 right-0 w-3"
            style={{ background: 'repeating-linear-gradient(to bottom, rgba(255,255,255,.28) 0 1px, transparent 1px 18px)' }}
          />
          <div
            className="absolute inset-y-0 right-0 w-[22px]"
            style={{ background: 'repeating-linear-gradient(to bottom, var(--color-accent) 0 2px, transparent 2px 90px)' }}
          />
          {['05', '15', '25', '35'].map((n, i) => (
            <div key={n} className="num absolute left-[7px] text-[9px] text-slate-400/70" style={{ top: 84 + i * 180 }}>{n}</div>
          ))}
        </div>

        {/* Crop marks */}
        <div className="pointer-events-none absolute right-5 top-5 h-4 w-4 border-r-[1.5px] border-t-[1.5px] border-white/25" />
        <div className="pointer-events-none absolute bottom-5 right-5 h-4 w-4 border-b-[1.5px] border-r-[1.5px] border-white/25" />

        {/* Header */}
        <div className="relative z-10 flex items-center gap-3">
          <img src="/favicon.svg" width={44} height={44} className="rounded-xl" alt="Built4U" />
          <div className="leading-none">
            <div className="text-[20px] font-extrabold tracking-tight">Built4U</div>
            <div className="num mt-1.5 text-[10px] tracking-[0.22em] text-slate-500">POS SYSTEM</div>
          </div>
          <div className="num ml-auto rounded-md border border-accent/40 px-2.5 py-1.5 text-[10px] font-semibold tracking-[0.12em] text-accent">
            HARDWARE&nbsp;ED.
          </div>
        </div>

        {/* Middle: copy + receipt tape */}
        <div className="relative z-10 flex items-center gap-10">
          <div className="flex-1">
            <div className="num mb-5 text-[11px] font-semibold tracking-[0.16em] text-blue-400">
              POINT OF SALE · HARDWARE &amp; BUILDING SUPPLY
            </div>
            <h2 className="m-0 mb-[18px] max-w-[14ch] text-[40px] font-extrabold leading-[1.1] tracking-[-0.025em]">
              Every sale — <span className="text-accent">on paper &amp; in the cloud.</span>
            </h2>
            <p className="m-0 max-w-[34ch] text-[15.5px] leading-relaxed text-slate-400">
              Printed receipts, live inventory, and a clean audit trail — from one register, across every branch.
            </p>
          </div>

          {/* Receipt tape */}
          <div
            className="num w-[214px] flex-none rounded bg-[#fbfbf9] px-[18px] pb-6 pt-[18px] text-[11px] leading-[1.75] text-[#1a1a1a]"
            style={{
              transform: 'rotate(2.5deg)',
              boxShadow: '0 24px 46px -12px rgba(0,0,0,.55)',
              clipPath:
                'polygon(0 0,100% 0,100% calc(100% - 6px),94% 100%,88% calc(100% - 6px),82% 100%,76% calc(100% - 6px),70% 100%,64% calc(100% - 6px),58% 100%,52% calc(100% - 6px),46% 100%,40% calc(100% - 6px),34% 100%,28% calc(100% - 6px),22% 100%,16% calc(100% - 6px),10% 100%,4% calc(100% - 6px),0 100%)',
            }}
          >
            <div className="text-center text-[12px] font-bold tracking-[0.05em]">BUILT4U HARDWARE</div>
            <div className="mb-2 text-center text-[9.5px] text-[#777]">WH-01 · Main Warehouse</div>
            <div className="my-[7px] border-t border-dashed border-[#bbb]" />
            {[['Galv Bolt M10', '96.00'], ['PVC Pipe 1"', '145.00'], ['Paint Roller', '178.00'], ['Wood Screw x50', '62.50']].map(([a, b]) => (
              <div key={a} className="flex justify-between"><span>{a}</span><span>{b}</span></div>
            ))}
            <div className="my-[7px] border-t border-dashed border-[#bbb]" />
            <div className="flex justify-between font-bold"><span>TOTAL</span><span>481.50</span></div>
            <div className="flex justify-between text-[#555]"><span>CASH</span><span>500.00</span></div>
            <div className="flex justify-between text-[#555]"><span>CHANGE</span><span>18.50</span></div>
            <div className="mt-2 text-center text-[9.5px] text-[#777]">SALE #000842 · SALAMAT PO</div>
          </div>
        </div>

        {/* Footer */}
        <div className="num relative z-10 flex items-center justify-between text-[11px] text-slate-600">
          <span>SINGLE-BUSINESS · SITE-SCOPED ACCESS</span>
          <span>v2.0</span>
        </div>
      </div>

      {/* ── Form panel ──────────────────────────────────────────────── */}
      <div className="relative flex flex-1 items-center justify-center bg-canvas px-6 py-10">
        <div className="safety-stripe absolute inset-x-0 top-0 h-1 opacity-90" />

        <div className="w-[396px] max-w-full">
          <div className="mb-7 flex items-center gap-2.5 lg:hidden">
            <img src="/favicon.svg" width={36} height={36} className="rounded-lg" alt="Built4U" />
            <span className="text-lg font-extrabold">Built4U POS</span>
          </div>

          <div className="num mb-3.5 text-[11px] font-semibold tracking-[0.18em] text-accent">COUNTER ACCESS</div>
          <h3 className="m-0 mb-2 text-[27px] font-extrabold tracking-[-0.02em] text-slate-900">Sign in to your register</h3>
          <p className="m-0 mb-[30px] text-[14px] text-slate-500">Enter your credentials to open the counter.</p>

          <form onSubmit={handleSubmit(onSubmit)}>
            <label className={labelCls}>Username</label>
            <div className="relative mb-1">
              <User size={17} className={iconCls} />
              <input autoFocus {...register('username')} onBlur={() => loadSites()} className={field} placeholder="admin" />
            </div>
            {errors.username && <p className="mb-2 text-xs text-red-600">{errors.username.message}</p>}

            <label className={`${labelCls} mt-4`}>
              Branch {loadingSites && <span className="font-normal text-slate-400">(loading…)</span>}
            </label>
            <div className="relative mb-1">
              <Warehouse size={17} className={iconCls} />
              <select
                {...register('siteCode')}
                onFocus={() => { if (sites.length === 0) loadSites() }}
                className={`${field} appearance-none pr-10`}
              >
                <option value="">{sites.length ? 'Select a branch…' : 'Enter username first'}</option>
                {sites.map((s) => (
                  <option key={s.id} value={s.code}>{s.name} — {s.code}</option>
                ))}
              </select>
              <ChevronDown size={18} className="pointer-events-none absolute right-3.5 top-[15px] text-slate-400" />
            </div>
            {errors.siteCode && <p className="mb-2 text-xs text-red-600">{errors.siteCode.message}</p>}

            <label className={`${labelCls} mt-4`}>Password</label>
            <div className="relative mb-1">
              <Lock size={17} className={iconCls} />
              <input type="password" {...register('password')} className={field} placeholder="••••••••" />
            </div>
            {errors.password && <p className="mb-2 text-xs text-red-600">{errors.password.message}</p>}

            <button
              type="submit"
              disabled={isSubmitting}
              className="mt-[26px] inline-flex h-[50px] w-full items-center justify-center gap-2.5 rounded-xl bg-blue-600 text-[15px] font-bold text-white shadow-lg shadow-blue-600/40 transition hover:bg-blue-700 disabled:opacity-60"
            >
              <LogIn size={18} /> {isSubmitting ? 'Signing in…' : 'Sign in & open register'}
            </button>
          </form>

          <div className="mt-[22px] flex items-center justify-center gap-1.5 text-[12.5px] text-slate-400">
            <ShieldCheck size={14} /> Rate-limited · 5-attempt lockout
          </div>
        </div>

        <div className="num absolute bottom-[22px] right-[26px] text-[10.5px] text-slate-300">BUILD 2.0.4</div>
      </div>
    </div>
  )
}
