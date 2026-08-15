import { useEffect, useState, type ReactNode } from 'react'
import { Gift, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { listItems, type Item } from '@/lib/inventory'
import { createReward, deleteReward, listRewardsAdmin, promoErr, updateReward, type Reward, type RewardType } from '@/lib/promo'

export default function LoyaltyRewardsPage() {
  const [rows, setRows] = useState<Reward[]>([])
  const [editing, setEditing] = useState<Reward | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listRewardsAdmin()) }
    catch (e) { toast.error(promoErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function remove(r: Reward) {
    if (!confirm(`Delete reward "${r.name}"?`)) return
    try { await deleteReward(r.id); toast.success('Deleted'); reload() } catch (e) { toast.error(promoErr(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Gift size={18} className="text-blue-600" /> Loyalty rewards
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New reward</button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Name</th><th className="px-4 py-2 font-medium">Type</th><th className="px-4 py-2 font-medium text-right">Points</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No rewards</td></tr>
              : rows.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-2 text-slate-700">{r.name}{r.description && <div className="text-xs text-slate-400">{r.description}</div>}</td>
                  <td className="px-4 py-2 text-slate-500">{r.rewardType === 'ITEM' ? `Item · ${r.itemName ?? '#' + r.itemId}` : 'Free-text'}</td>
                  <td className="px-4 py-2 text-right text-slate-600">{r.pointsCost}</td>
                  <td className="px-4 py-2"><span className={r.active ? 'text-emerald-600' : 'text-slate-400'}>{r.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(r)}>Edit</button>
                    <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(r)}><Trash2 size={13} /> Delete</button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <RForm r={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </div>
  )
}

function RForm({ r, onClose, onSaved }: { r: Reward | null; onClose: () => void; onSaved: () => void }) {
  const isNew = r === null
  const [f, setF] = useState({
    name: r?.name ?? '', description: r?.description ?? '',
    pointsCost: r ? String(r.pointsCost) : '10',
    rewardType: (r?.rewardType ?? 'FREETEXT') as RewardType,
    itemId: r?.itemId ?? null as number | null, itemLabel: r?.itemName ?? '',
    sortOrder: r ? String(r.sortOrder) : '100', active: r?.active ?? true,
  })
  const [search, setSearch] = useState('')
  const [results, setResults] = useState<Item[]>([])
  const [saving, setSaving] = useState(false)
  const s = (k: keyof typeof f, val: string | boolean | number | null) => setF((p) => ({ ...p, [k]: val }))

  useEffect(() => {
    if (f.rewardType !== 'ITEM') return
    const t = setTimeout(() => {
      if (!search.trim()) { setResults([]); return }
      listItems({ search: search.trim() }).then((x) => setResults(x.slice(0, 6))).catch(() => {})
    }, 200)
    return () => clearTimeout(t)
  }, [search, f.rewardType])

  async function save() {
    if (!f.name.trim()) { toast.error('Name is required'); return }
    if (f.rewardType === 'ITEM' && !f.itemId) { toast.error('Pick an inventory item'); return }
    setSaving(true)
    try {
      const body = {
        name: f.name.trim(), description: f.description.trim() || undefined,
        pointsCost: Number(f.pointsCost), rewardType: f.rewardType,
        itemId: f.rewardType === 'ITEM' ? f.itemId ?? undefined : undefined,
        sortOrder: Number(f.sortOrder || 100), active: f.active,
      }
      if (isNew) await createReward(body); else await updateReward(r!.id, body)
      toast.success('Saved'); onSaved()
    } catch (e) { toast.error(promoErr(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={isNew ? 'New reward' : `Edit ${r!.name}`} onClose={onClose}>
      <div className="space-y-3">
        <Fld label="Name"><input className={inputCls} value={f.name} onChange={(e) => s('name', e.target.value)} autoFocus /></Fld>
        <Fld label="Description"><input className={inputCls} value={f.description} onChange={(e) => s('description', e.target.value)} /></Fld>
        <div className="grid grid-cols-2 gap-3">
          <Fld label="Points cost"><input className={inputCls} type="number" min={1} value={f.pointsCost} onChange={(e) => s('pointsCost', e.target.value)} /></Fld>
          <Fld label="Type"><select className={inputCls} value={f.rewardType} onChange={(e) => { s('rewardType', e.target.value); s('itemId', null); s('itemLabel', '') }}><option value="FREETEXT">Free-text</option><option value="ITEM">Inventory item</option></select></Fld>
        </div>
        {f.rewardType === 'ITEM' && (
          <div className="relative">
            <label className="mb-1 block text-sm font-medium text-slate-700">Linked item {f.itemLabel && <span className="text-blue-600">· {f.itemLabel}</span>}</label>
            <input className={inputCls} placeholder="Search item…" value={search} onChange={(e) => setSearch(e.target.value)} />
            {results.length > 0 && (
              <div className="absolute z-10 mt-1 w-full rounded-md border border-slate-200 bg-white shadow">
                {results.map((it) => (
                  <button key={it.id} className="block w-full px-3 py-1.5 text-left text-sm hover:bg-slate-50" onClick={() => { s('itemId', it.id); s('itemLabel', it.name); setSearch(''); setResults([]) }}>
                    {it.name} <span className="text-slate-400">{it.code}</span>
                  </button>
                ))}
              </div>
            )}
            <p className="mt-1 text-xs text-slate-400">Redeeming decrements this item's stock by 1.</p>
          </div>
        )}
        <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={f.active} onChange={(e) => s('active', e.target.checked)} /> Active</label>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}

function Fld({ label, children }: { label: string; children: ReactNode }) {
  return <div><label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>{children}</div>
}
