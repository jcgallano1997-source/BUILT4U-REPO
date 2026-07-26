import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { KeyRound } from 'lucide-react'
import type { AxiosError } from 'axios'
import { changePassword } from '@/lib/auth'
import { useAuthStore } from '@/store/authStore'

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
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: '', newPassword: '', confirm: '' },
  })

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
    <div className="max-w-sm mx-auto">
      <div className="mb-4 flex items-center gap-2 text-slate-800">
        <KeyRound size={18} className="text-indigo-600" />
        <h1 className="text-lg font-semibold">Change password</h1>
      </div>

      {user?.mustChangePassword && (
        <div className="mb-4 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-sm px-3 py-2">
          You must set a new password before continuing.
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="bg-white rounded-xl border border-slate-200 p-6 space-y-4">
        <Field label="Current password" error={errors.currentPassword?.message}>
          <input type="password" {...register('currentPassword')} className={inputCls} />
        </Field>
        <Field label="New password" error={errors.newPassword?.message}>
          <input type="password" {...register('newPassword')} className={inputCls} />
        </Field>
        <Field label="Confirm new password" error={errors.confirm?.message}>
          <input type="password" {...register('confirm')} className={inputCls} />
        </Field>
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
        >
          {isSubmitting ? 'Saving…' : 'Update password'}
        </button>
      </form>
    </div>
  )
}

const inputCls =
  'w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500'

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      {children}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  )
}
