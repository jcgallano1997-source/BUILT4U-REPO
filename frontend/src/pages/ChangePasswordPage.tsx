import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { Check, KeyRound, ShieldCheck } from 'lucide-react'
import type { AxiosError } from 'axios'
import { changePassword } from '@/lib/auth'
import { useAuthStore } from '@/store/authStore'
import { inputCls } from '@/components/Modal'

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z
      .string()
      .min(8, 'At least 8 characters')
      .regex(/[A-Za-z]/, 'Must contain a letter')
      .regex(/\d/, 'Must contain a digit'),
    confirm: z.string().min(1, 'Confirm your new password'),
  })
  .refine((v) => v.newPassword === v.confirm, {
    path: ['confirm'],
    message: 'Passwords do not match',
  })
type FormValues = z.infer<typeof schema>

export default function ChangePasswordPage() {
  const navigate = useNavigate()
  const { user, setUser } = useAuthStore()

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: '', newPassword: '', confirm: '' },
  })

  const pw = watch('newPassword') ?? ''
  const confirm = watch('confirm') ?? ''
  const rules = [
    { ok: pw.length >= 8, label: '8+ characters' },
    { ok: /[A-Za-z]/.test(pw), label: 'a letter' },
    { ok: /\d/.test(pw), label: 'a digit' },
  ]
  const matches = pw.length > 0 && pw === confirm

  async function onSubmit(values: FormValues) {
    try {
      await changePassword(values.currentPassword, values.newPassword)
      if (user) setUser({ ...user, mustChangePassword: false })
      toast.success('Password changed')
      navigate('/', { replace: true })
    } catch (e) {
      const ax = e as AxiosError<{ message?: string }>
      toast.error(ax.response?.data?.message ?? 'Could not change password')
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <div className="num mb-3.5 text-[11px] font-semibold tracking-[0.18em] text-accent">ACCOUNT SECURITY</div>
      <h1 className="flex items-center gap-2 text-[22px] font-extrabold tracking-[-0.02em] text-slate-900">
        <KeyRound size={20} className="text-blue-600" /> Change password
      </h1>
      <p className="mt-1 mb-5 text-[13.5px] text-slate-500">Keep your register access secure.</p>

      {user?.mustChangePassword && (
        <div className="mb-4 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          <ShieldCheck size={16} /> You must set a new password before continuing.
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="overflow-hidden rounded-[18px] border border-slate-200 bg-white shadow-sm">
        <div className="safety-stripe h-1" />
        <div className="space-y-4 p-6">
          <Field label="Current password" error={errors.currentPassword?.message}>
            <input type="password" {...register('currentPassword')} className={inputCls} autoFocus />
          </Field>
          <Field label="New password" error={errors.newPassword?.message}>
            <input type="password" {...register('newPassword')} className={inputCls} />
          </Field>

          {/* Live rule chips — turn green as each rule is met. */}
          <div className="flex flex-wrap gap-2">
            {rules.map((r) => (
              <span
                key={r.label}
                className={`inline-flex items-center gap-1 rounded-md px-2 py-1 text-[11.5px] font-semibold transition ${
                  r.ok ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-400'
                }`}
              >
                <Check size={12} className={r.ok ? '' : 'opacity-40'} /> {r.label}
              </span>
            ))}
          </div>

          <Field label="Confirm new password" error={errors.confirm?.message}>
            <div className="relative">
              <input type="password" {...register('confirm')} className={inputCls} />
              {matches && <Check size={17} className="absolute right-3 top-2.5 text-emerald-600" />}
            </div>
          </Field>

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-1 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-blue-600 text-sm font-semibold text-white shadow-sm shadow-blue-600/30 transition hover:bg-blue-700 disabled:opacity-60"
          >
            {isSubmitting ? 'Saving…' : 'Update password'}
          </button>
        </div>
      </form>
    </div>
  )
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div>
      <label className="mb-1.5 block text-[13px] font-semibold text-slate-700">{label}</label>
      {children}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  )
}
