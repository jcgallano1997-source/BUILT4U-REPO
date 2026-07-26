import { useEffect, useState, type ReactNode } from 'react'
import { Plus, Sparkles, Trash2, UserRound } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { createCustomer, deleteCustomer, listCustomers, updateCustomer, type Customer } from '@/lib/parties'
import { getLedger, listActiveRewards, redeemReward, promoErr, type LedgerView, type Reward } from '@/lib/promo'

const err = (e: unknown, f: string) => (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export default function CustomersPage() {
  const [rows, setRows] = useState<Customer[]>([])
  const [search, setSearch] = useState('')
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<Customer | 'new' | null>(null)
  const [loyaltyFor, setLoyaltyFor] = useState<Customer | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listCustomers(search, includeInactive)) }
    catch (e) { toast.error(err(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => {
    const t = setTimeout(reload, 200)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, includeInactive])

  async function remove(c: Customer) {
    if (!confirm(`Deactivate customer "${c.name}"?`)) return
    try { await deleteCustomer(c.id); toast.success('Deactivated'); reload() } catch (e) { toast.error(err(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <UserRound size={18} className="text-indigo-600" /> Customers
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New customer</button>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <input className={`${inputCls} max-w-xs`} placeholder="Search name / phone / email…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <label className="flex items-center gap-2 text-sm text-slate-500">
          <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
        </label>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Name</th><th className="px-4 py-2 font-medium">Contact</th><th className="px-4 py-2 font-medium">Email</th><th className="px-4 py-2 font-medium text-right">Points</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No customers</td></tr>
              : rows.map((c) => (
                <tr key={c.id}>
                  <td className="px-4 py-2 text-slate-700">{c.name}</td>
                  <td className="px-4 py-2 text-slate-500">{c.contact ?? '—'}</td>
                  <td className="px-4 py-2 text-slate-500">{c.email ?? '—'}</td>
                  <td className="px-4 py-2 text-right text-slate-500">{c.points}</td>
                  <td className="px-4 py-2"><span className={c.active ? 'text-emerald-600' : 'text-slate-400'}>{c.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="inline-flex items-center gap-1 text-amber-600 hover:underline" onClick={() => setLoyaltyFor(c)}><Sparkles size={13} /> Points</button>
                    <button className="ml-3 text-indigo-600 hover:underline" onClick={() => setEditing(c)}>Edit</button>
                    {c.active && <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(c)}><Trash2 size={13} /> Deactivate</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <CustForm cust={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
      {loyaltyFor && <LoyaltyPanel cust={loyaltyFor} onClose={() => setLoyaltyFor(null)} onChanged={reload} />}
    </div>
  )
}

function LoyaltyPanel({ cust, onClose, onChanged }: { cust: Customer; onClose: () => void; onChanged: () => void }) {
  const [view, setView] = useState<LedgerView | null>(null)
  const [rewards, setRewards] = useState<Reward[]>([])
  const [rewardId, setRewardId] = useState('')
  const [busy, setBusy] = useState(false)

  async function load() {
    try {
      const [v, rs] = await Promise.all([getLedger(cust.id, 0, 10), listActiveRewards()])
      setView(v); setRewards(rs)
    } catch (e) { toast.error(promoErr(e, 'Failed to load loyalty')) }
  }
  useEffect(() => { load() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function redeem() {
    if (!rewardId) { toast.error('Pick a reward'); return }
    setBusy(true)
    try {
      const r = await redeemReward(cust.id, Number(rewardId))
      toast.success(`Redeemed "${r.rewardName}" — balance ${r.newBalance}`)
      setRewardId(''); await load(); onChanged()
    } catch (e) { toast.error(promoErr(e, 'Redeem failed')) } finally { setBusy(false) }
  }

  return (
    <Modal title={`Loyalty · ${cust.name}`} onClose={onClose} width="max-w-lg">
      <div className="space-y-3">
        <div className="rounded-md bg-amber-50 px-3 py-2 text-center">
          <div className="text-xs text-amber-700">Points balance</div>
          <div className="text-2xl font-semibold text-amber-800">{view ? view.liveBalance : '…'}</div>
        </div>

        {rewards.length > 0 && (
          <div className="flex items-end gap-2 border-t border-slate-100 pt-3">
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-slate-600">Redeem a reward</label>
              <select className={inputCls} value={rewardId} onChange={(e) => setRewardId(e.target.value)}>
                <option value="">Select…</option>
                {rewards.map((r) => <option key={r.id} value={r.id} disabled={!!view && r.pointsCost > view.liveBalance}>{r.name} — {r.pointsCost} pts</option>)}
              </select>
            </div>
            <button className={btnPrimary} disabled={busy || !rewardId} onClick={redeem}>Redeem</button>
          </div>
        )}

        <div>
          <div className="mb-1 text-xs font-medium text-slate-500">Recent activity</div>
          {!view || view.content.length === 0 ? <p className="text-sm text-slate-400">No points activity yet.</p> : (
            <table className="w-full text-sm">
              <tbody className="divide-y divide-slate-100">
                {view.content.map((l) => (
                  <tr key={l.id}>
                    <td className="py-1 text-slate-500">{new Date(l.creationDate).toLocaleDateString()}</td>
                    <td className="py-1 text-slate-600">{l.entryType}{l.note ? <span className="text-slate-400"> · {l.note}</span> : ''}</td>
                    <td className={`py-1 text-right font-medium ${l.points < 0 ? 'text-red-600' : 'text-emerald-600'}`}>{l.points > 0 ? '+' : ''}{l.points}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="flex justify-end"><button className={btnGhost} onClick={onClose}>Close</button></div>
      </div>
    </Modal>
  )
}

function CustForm({ cust, onClose, onSaved }: { cust: Customer | null; onClose: () => void; onSaved: () => void }) {
  const isNew = cust === null
  const [f, setF] = useState({
    name: cust?.name ?? '', contact: cust?.contact ?? '', address: cust?.address ?? '',
    email: cust?.email ?? '', points: cust ? String(cust.points) : '0',
    creditLimit: cust ? String(cust.creditLimit) : '0', active: cust?.active ?? true,
  })
  const [saving, setSaving] = useState(false)
  const s = (k: keyof typeof f, v: string | boolean) => setF((p) => ({ ...p, [k]: v }))

  async function save() {
    setSaving(true)
    try {
      if (isNew) await createCustomer({ name: f.name.trim(), contact: f.contact.trim() || undefined, address: f.address.trim() || undefined, email: f.email.trim() || undefined, creditLimit: Number(f.creditLimit || 0) })
      else await updateCustomer(cust!.id, { name: f.name.trim(), contact: f.contact.trim() || undefined, address: f.address.trim() || undefined, email: f.email.trim() || undefined, points: Number(f.points || 0), creditLimit: Number(f.creditLimit || 0), active: f.active })
      toast.success('Saved'); onSaved()
    } catch (e) { toast.error(err(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={isNew ? 'New customer' : `Edit ${cust!.name}`} onClose={onClose}>
      <div className="space-y-3">
        <F label="Name"><input className={inputCls} value={f.name} onChange={(e) => s('name', e.target.value)} autoFocus /></F>
        <F label="Contact"><input className={inputCls} value={f.contact} onChange={(e) => s('contact', e.target.value)} /></F>
        <F label="Email"><input className={inputCls} value={f.email} onChange={(e) => s('email', e.target.value)} /></F>
        <F label="Address"><input className={inputCls} value={f.address} onChange={(e) => s('address', e.target.value)} /></F>
        <F label="Credit limit (₱ — 0 = no limit)"><input className={inputCls} type="number" min={0} value={f.creditLimit} onChange={(e) => s('creditLimit', e.target.value)} /></F>
        {!isNew && <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={f.active} onChange={(e) => s('active', e.target.checked)} /> Active</label>}
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}

function F({ label, children }: { label: string; children: ReactNode }) {
  return <div><label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>{children}</div>
}
