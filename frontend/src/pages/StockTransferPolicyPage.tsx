import { useEffect, useState } from 'react'
import { Plus, Route, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { btnPrimary, inputCls } from '@/components/Modal'
import { listSites, type SiteSummary } from '@/lib/admin'
import { addPolicyRule, deletePolicyRule, getPolicy, xferErr, type PolicyRule } from '@/lib/transfers'

/**
 * Cross-site transfer allow-list. No rules → OPEN (any active site may ship to
 * any other). Adding the first rule switches to ENFORCED — only listed
 * (source → destination) pairs are permitted.
 */
export default function StockTransferPolicyPage() {
  const [enforced, setEnforced] = useState(false)
  const [rules, setRules] = useState<PolicyRule[]>([])
  const [sites, setSites] = useState<SiteSummary[]>([])
  const [source, setSource] = useState('')
  const [dest, setDest] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function reload() {
    setLoading(true)
    try {
      const [p, s] = await Promise.all([getPolicy(), listSites()])
      setEnforced(p.enforced); setRules(p.rules); setSites(s)
    } catch (e) { toast.error(xferErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function add() {
    if (!source || !dest) { toast.error('Pick a source and destination'); return }
    if (source === dest) { toast.error('Source and destination must differ'); return }
    setSaving(true)
    try {
      await addPolicyRule(Number(source), Number(dest))
      toast.success('Rule added'); setSource(''); setDest(''); reload()
    } catch (e) { toast.error(xferErr(e, 'Failed to add rule')) } finally { setSaving(false) }
  }
  async function remove(r: PolicyRule) {
    if (!confirm(`Remove ${r.sourceSiteName} → ${r.destSiteName}?`)) return
    try { await deletePolicyRule(r.id); toast.success('Rule removed'); reload() }
    catch (e) { toast.error(xferErr(e, 'Failed to remove')) }
  }

  return (
    <div className="space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <Route size={18} className="text-indigo-600" /> Stock transfer policy
      </h1>
      <div className={`rounded-md border px-3 py-2 text-sm ${enforced ? 'border-amber-200 bg-amber-50 text-amber-800' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>
        {enforced
          ? 'ENFORCED — only the (source → destination) pairs listed below are permitted.'
          : 'OPEN — no rules configured, so any active site may ship to any other. Add a rule to start enforcing.'}
      </div>

      <div className="flex flex-wrap items-end gap-2 rounded-lg border border-slate-200 bg-white p-3">
        <div><label className="mb-1 block text-xs font-medium text-slate-600">Source site</label>
          <select className={`${inputCls} min-w-[12rem]`} value={source} onChange={(e) => setSource(e.target.value)}>
            <option value="">Select…</option>
            {sites.map((s) => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}
          </select></div>
        <span className="pb-2 text-slate-400">→</span>
        <div><label className="mb-1 block text-xs font-medium text-slate-600">Destination site</label>
          <select className={`${inputCls} min-w-[12rem]`} value={dest} onChange={(e) => setDest(e.target.value)}>
            <option value="">Select…</option>
            {sites.filter((s) => String(s.id) !== source).map((s) => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}
          </select></div>
        <button className={btnPrimary} disabled={saving} onClick={add}><Plus size={16} /> Allow pair</button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Source</th><th className="px-4 py-2 font-medium">Destination</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={3} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rules.length === 0 ? <tr><td colSpan={3} className="px-4 py-6 text-center text-slate-400">No rules — policy is open</td></tr>
              : rules.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-2 text-slate-700">{r.sourceSiteCode} · {r.sourceSiteName}</td>
                  <td className="px-4 py-2 text-slate-700">{r.destSiteCode} · {r.destSiteName}</td>
                  <td className="px-4 py-2 text-right">
                    <button className="inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(r)}><Trash2 size={13} /> Remove</button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
