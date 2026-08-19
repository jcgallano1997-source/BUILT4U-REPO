import { useEffect, useState } from 'react'
import { Mail, AlertTriangle } from 'lucide-react'
import { toast } from 'sonner'
import { btnPrimary, inputCls } from '@/components/Modal'
import {
  emailErr, getReportEmailState, listRecipientUsers, saveReportEmailConfig, REPORT_CODES,
  type RecipientUser, type ReportEmailConfig,
} from '@/lib/reportemail'

type Draft = { recipientEmail: string; subject: string; body: string; userIds: number[] }

/** Admin: per-report email recipients. Delivery is inert until a provider key is configured. */
export default function ReportEmailPage() {
  const [enabled, setEnabled] = useState(false)
  const [drafts, setDrafts] = useState<Record<string, Draft>>({})
  const [users, setUsers] = useState<RecipientUser[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const [state, u] = await Promise.all([getReportEmailState(), listRecipientUsers()])
      setEnabled(state.enabled)
      setUsers(u)
      const byCode: Record<string, ReportEmailConfig> = {}
      state.configs.forEach((c) => { byCode[c.reportCode] = c })
      const d: Record<string, Draft> = {}
      REPORT_CODES.forEach(({ code }) => {
        const c = byCode[code]
        d[code] = {
          recipientEmail: c?.recipientEmail ?? '',
          subject: c?.subject ?? '',
          body: c?.body ?? '',
          userIds: (c?.recipients ?? []).map((r) => r.userId),
        }
      })
      setDrafts(d)
    } catch (e) { toast.error(emailErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  const edit = (code: string, k: 'recipientEmail' | 'subject' | 'body', v: string) =>
    setDrafts((p) => ({ ...p, [code]: { ...p[code], [k]: v } }))

  const toggleUser = (code: string, userId: number) =>
    setDrafts((p) => {
      const cur = p[code].userIds
      const next = cur.includes(userId) ? cur.filter((id) => id !== userId) : [...cur, userId]
      return { ...p, [code]: { ...p[code], userIds: next } }
    })

  async function save(code: string, label: string) {
    setSaving(code)
    try {
      const d = drafts[code]
      await saveReportEmailConfig(code, {
        label,
        recipientEmail: d.recipientEmail.trim() || undefined,
        subject: d.subject.trim() || undefined,
        body: d.body.trim() || undefined,
        userIds: d.userIds,
      })
      toast.success('Saved')
    } catch (e) { toast.error(emailErr(e, 'Save failed')) } finally { setSaving(null) }
  }

  return (
    <div className="max-w-3xl space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <Mail size={18} className="text-blue-600" /> Report email
      </h1>

      {!enabled && (
        <div className="flex items-start gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700">
          <AlertTriangle size={15} className="mt-0.5 flex-none" />
          <div>Email delivery is <b>disabled</b> — no mail provider key is configured. You can still set recipients here;
            reports won’t actually send until an administrator sets <code>app.mail.resend-api-key</code> on the server.</div>
        </div>
      )}

      {loading ? <p className="text-slate-400">Loading…</p> : (
        <div className="space-y-3">
          {REPORT_CODES.map(({ code, label }) => {
            const d = drafts[code]
            if (!d) return null
            return (
              <section key={code} className="space-y-2 rounded-lg border border-slate-200 bg-white p-4">
                <div className="text-sm font-semibold text-slate-700">{label}</div>

                {/* Recipients are users, so a changed address or a deactivated
                    account takes effect everywhere at once. */}
                <div>
                  <label className="mb-1 block text-xs text-slate-500">
                    Send to {d.userIds.length > 0 && <span className="text-slate-400">· {d.userIds.length} selected</span>}
                  </label>
                  <div className="grid grid-cols-2 gap-1 rounded-md border border-slate-200 p-2">
                    {users.length === 0 && <span className="text-xs text-slate-400">No users.</span>}
                    {users.map((u) => {
                      const noEmail = !u.email
                      return (
                        <label key={u.userId}
                          title={noEmail ? 'This user has no email address on their account' : u.email!}
                          className={`flex items-start gap-2 rounded px-2 py-1 text-sm ${noEmail ? 'cursor-not-allowed opacity-50' : 'hover:bg-slate-50'}`}>
                          <input type="checkbox" className="mt-0.5" disabled={noEmail}
                            checked={d.userIds.includes(u.userId)} onChange={() => toggleUser(code, u.userId)} />
                          <span className="min-w-0">
                            <span className="text-slate-700">{u.fullName}</span>{' '}
                            <span className="text-xs text-slate-400">{u.email ?? 'no email set'}</span>
                          </span>
                        </label>
                      )
                    })}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="mb-1 block text-xs text-slate-500">Also send to (optional)</label>
                    <input className={inputCls} type="email" placeholder="accountant@example.com"
                      value={d.recipientEmail} onChange={(e) => edit(code, 'recipientEmail', e.target.value)} />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs text-slate-500">Subject (optional)</label>
                    <input className={inputCls} placeholder={`Built4U report — ${label}`}
                      value={d.subject} onChange={(e) => edit(code, 'subject', e.target.value)} />
                  </div>
                </div>
                <div>
                  <label className="mb-1 block text-xs text-slate-500">Body (optional)</label>
                  <textarea className={`${inputCls} h-16 resize-y`} placeholder="Custom message for this report…"
                    value={d.body} onChange={(e) => edit(code, 'body', e.target.value)} />
                </div>
                <div className="flex justify-end">
                  <button className={btnPrimary} disabled={saving === code} onClick={() => save(code, label)}>
                    {saving === code ? 'Saving…' : 'Save'}
                  </button>
                </div>
              </section>
            )
          })}
        </div>
      )}
    </div>
  )
}
