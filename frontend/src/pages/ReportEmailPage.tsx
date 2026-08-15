import { useEffect, useState } from 'react'
import { Mail, AlertTriangle } from 'lucide-react'
import { toast } from 'sonner'
import { btnPrimary, inputCls } from '@/components/Modal'
import {
  emailErr, getReportEmailState, saveReportEmailConfig, REPORT_CODES, type ReportEmailConfig,
} from '@/lib/reportemail'

type Draft = { recipientEmail: string; subject: string; body: string }

/** Admin: per-report email recipients. Delivery is inert until a provider key is configured. */
export default function ReportEmailPage() {
  const [enabled, setEnabled] = useState(false)
  const [drafts, setDrafts] = useState<Record<string, Draft>>({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const state = await getReportEmailState()
      setEnabled(state.enabled)
      const byCode: Record<string, ReportEmailConfig> = {}
      state.configs.forEach((c) => { byCode[c.reportCode] = c })
      const d: Record<string, Draft> = {}
      REPORT_CODES.forEach(({ code }) => {
        const c = byCode[code]
        d[code] = { recipientEmail: c?.recipientEmail ?? '', subject: c?.subject ?? '', body: c?.body ?? '' }
      })
      setDrafts(d)
    } catch (e) { toast.error(emailErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  const edit = (code: string, k: keyof Draft, v: string) =>
    setDrafts((p) => ({ ...p, [code]: { ...p[code], [k]: v } }))

  async function save(code: string, label: string) {
    setSaving(code)
    try {
      const d = drafts[code]
      await saveReportEmailConfig(code, {
        label,
        recipientEmail: d.recipientEmail.trim() || undefined,
        subject: d.subject.trim() || undefined,
        body: d.body.trim() || undefined,
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
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="mb-1 block text-xs text-slate-500">Recipient email</label>
                    <input className={inputCls} type="email" placeholder="finance@example.com"
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
