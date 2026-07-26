import { useEffect, useState, type ReactNode } from 'react'
import { Plus, Trash2, Truck } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { createSupplier, deleteSupplier, listSuppliers, updateSupplier, type Supplier } from '@/lib/parties'

const err = (e: unknown, f: string) => (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export default function SuppliersPage() {
  const [rows, setRows] = useState<Supplier[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<Supplier | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listSuppliers(undefined, includeInactive)) }
    catch (e) { toast.error(err(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  async function remove(s: Supplier) {
    if (!confirm(`Deactivate supplier "${s.name}"?`)) return
    try { await deleteSupplier(s.id); toast.success('Deactivated'); reload() } catch (e) { toast.error(err(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Truck size={18} className="text-indigo-600" /> Suppliers
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New supplier</button>
      </div>
      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Code</th><th className="px-4 py-2 font-medium">Name</th><th className="px-4 py-2 font-medium">Agent</th><th className="px-4 py-2 font-medium">Contact</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No suppliers</td></tr>
              : rows.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">{s.code}</td>
                  <td className="px-4 py-2">{s.name}</td>
                  <td className="px-4 py-2 text-slate-500">{s.agentName ?? '—'}</td>
                  <td className="px-4 py-2 text-slate-500">{s.contact ?? '—'}</td>
                  <td className="px-4 py-2"><span className={s.active ? 'text-emerald-600' : 'text-slate-400'}>{s.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-indigo-600 hover:underline" onClick={() => setEditing(s)}>Edit</button>
                    {s.active && <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(s)}><Trash2 size={13} /> Deactivate</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <SupForm sup={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </div>
  )
}

function SupForm({ sup, onClose, onSaved }: { sup: Supplier | null; onClose: () => void; onSaved: () => void }) {
  const isNew = sup === null
  const [f, setF] = useState({
    code: sup?.code ?? '', name: sup?.name ?? '', address: sup?.address ?? '',
    agentName: sup?.agentName ?? '', contact: sup?.contact ?? '',
    apEnabled: sup?.apEnabled ?? false, payableDays: sup ? String(sup.payableDays) : '30',
    active: sup?.active ?? true,
  })
  const [saving, setSaving] = useState(false)
  const s = (k: keyof typeof f, v: string | boolean) => setF((p) => ({ ...p, [k]: v }))

  async function save() {
    setSaving(true)
    try {
      const base = { code: f.code.trim(), name: f.name.trim(), address: f.address.trim() || undefined, agentName: f.agentName.trim() || undefined, contact: f.contact.trim() || undefined, apEnabled: f.apEnabled, payableDays: Number(f.payableDays || 30) }
      if (isNew) await createSupplier(base)
      else await updateSupplier(sup!.id, { ...base, active: f.active })
      toast.success('Saved'); onSaved()
    } catch (e) { toast.error(err(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={isNew ? 'New supplier' : `Edit ${sup!.code}`} onClose={onClose}>
      <div className="space-y-3">
        <F label="Code"><input className={inputCls} value={f.code} onChange={(e) => s('code', e.target.value)} autoFocus /></F>
        <F label="Name"><input className={inputCls} value={f.name} onChange={(e) => s('name', e.target.value)} /></F>
        <F label="Agent"><input className={inputCls} value={f.agentName} onChange={(e) => s('agentName', e.target.value)} /></F>
        <F label="Contact"><input className={inputCls} value={f.contact} onChange={(e) => s('contact', e.target.value)} /></F>
        <F label="Address"><input className={inputCls} value={f.address} onChange={(e) => s('address', e.target.value)} /></F>
        <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={f.apEnabled} onChange={(e) => s('apEnabled', e.target.checked)} /> Track payables (auto-create AP on goods receipt)</label>
        {f.apEnabled && <F label="Payment terms (days)"><input className={inputCls} type="number" min={0} value={f.payableDays} onChange={(e) => s('payableDays', e.target.value)} /></F>}
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
