import { useEffect, useState } from 'react'
import { Sparkles } from 'lucide-react'
import { toast } from 'sonner'
import { btnPrimary, inputCls } from '@/components/Modal'
import { getLoyaltyConfig, promoErr, saveLoyaltyConfig } from '@/lib/promo'

/** Admin: the % of the grand total customers earn as loyalty points. */
export default function LoyaltyConfigPage() {
  const [pointsRate, setPointsRate] = useState('5')
  const [redeemValue, setRedeemValue] = useState('1')
  const [usingDefault, setUsingDefault] = useState(true)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  async function reload() {
    setLoading(true)
    try {
      const c = await getLoyaltyConfig()
      setPointsRate(String(c.pointsRate)); setRedeemValue(String(c.redeemValue)); setUsingDefault(c.usingDefault)
    } catch (e) { toast.error(promoErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function save() {
    setSaving(true)
    try {
      await saveLoyaltyConfig(Number(pointsRate), Number(redeemValue))
      toast.success('Saved'); reload()
    } catch (e) { toast.error(promoErr(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <div className="max-w-lg space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <Sparkles size={18} className="text-indigo-600" /> Loyalty points
      </h1>
      {loading ? <p className="text-slate-400">Loading…</p> : (
        <div className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
          {usingDefault && <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">Using the default until you save a value for this site.</div>}
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Earn rate (% of grand total)</label>
            <input className={inputCls} type="number" min={0} max={100} step="0.01" value={pointsRate} onChange={(e) => setPointsRate(e.target.value)} />
            <p className="mt-1 text-xs text-slate-400">A customer attached to a ₱1,000 sale at {pointsRate || 0}% earns {Math.floor(1000 * (Number(pointsRate) || 0) / 100)} points.</p>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Point value (₱ per point, informational)</label>
            <input className={inputCls} type="number" min={0} step="0.01" value={redeemValue} onChange={(e) => setRedeemValue(e.target.value)} />
          </div>
          <div className="flex justify-end">
            <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </div>
      )}
    </div>
  )
}
