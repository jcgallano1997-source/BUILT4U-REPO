import { useEffect, useState, type ReactNode } from 'react'
import { CreditCard, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import {
  createPaymentMode, deletePaymentMode, listPaymentModes, updatePaymentMode,
  type PaymentMode, type SavePaymentMode,
} from '@/lib/parties'

const err = (e: unknown, f: string) => (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export default function PaymentModesPage() {
  const [rows, setRows] = useState<PaymentMode[]>([])
  const [editing, setEditing] = useState<PaymentMode | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listPaymentModes()) }
    catch (e) { toast.error(err(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function remove(m: PaymentMode) {
    if (!confirm(`Delete payment mode "${m.code}"?`)) return
    try { await deletePaymentMode(m.id); toast.success('Deleted'); reload() } catch (e) { toast.error(err(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <CreditCard size={18} className="text-blue-600" /> Payment modes
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New mode</button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Code</th><th className="px-4 py-2 font-medium">Label</th>
              <th className="px-4 py-2 font-medium">Surcharge</th><th className="px-4 py-2 font-medium">Flags</th>
              <th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.map((m) => (
                <tr key={m.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">{m.code}</td>
                  <td className="px-4 py-2">{m.label}</td>
                  <td className="px-4 py-2 text-slate-500">{m.surchargeType === 'NONE' ? '—' : `${m.surchargeType} ${m.surchargeValue}`}</td>
                  <td className="px-4 py-2 text-xs text-slate-400">
                    {[m.isCash && 'cash', m.accountsReceivable && 'AR', m.customerRequired && 'cust-req'].filter(Boolean).join(' · ') || '—'}
                  </td>
                  <td className="px-4 py-2"><span className={m.active ? 'text-emerald-600' : 'text-slate-400'}>{m.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(m)}>Edit</button>
                    <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(m)}><Trash2 size={13} /> Delete</button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <ModeForm mode={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </div>
  )
}

function ModeForm({ mode, onClose, onSaved }: { mode: PaymentMode | null; onClose: () => void; onSaved: () => void }) {
  const isNew = mode === null
  const [f, setF] = useState<SavePaymentMode>(mode ?? {
    code: '', label: '', surchargeType: 'NONE', surchargeValue: 0,
    isCash: false, allowsPartial: false, customerRequired: false, accountsReceivable: false,
    arDueDays: 30, sortOrder: 100, active: true,
  })
  const [saving, setSaving] = useState(false)
  const set = <K extends keyof SavePaymentMode>(k: K, v: SavePaymentMode[K]) => setF((p) => ({ ...p, [k]: v }))

  async function save() {
    setSaving(true)
    try {
      const body: SavePaymentMode = { ...f, code: f.code.trim(), label: f.label.trim(), surchargeValue: Number(f.surchargeValue), arDueDays: Number(f.arDueDays), sortOrder: Number(f.sortOrder) }
      if (isNew) await createPaymentMode(body)
      else await updatePaymentMode(mode!.id, body)
      toast.success('Saved'); onSaved()
    } catch (e) { toast.error(err(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={isNew ? 'New payment mode' : `Edit ${mode!.code}`} onClose={onClose}>
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <F label="Code"><input className={inputCls} value={f.code} onChange={(e) => set('code', e.target.value.toUpperCase())} disabled={!isNew} /></F>
          <F label="Label"><input className={inputCls} value={f.label} onChange={(e) => set('label', e.target.value)} /></F>
          <F label="Surcharge type">
            <select className={inputCls} value={f.surchargeType} onChange={(e) => set('surchargeType', e.target.value as SavePaymentMode['surchargeType'])}>
              <option value="NONE">None</option><option value="PERCENT">Percent</option><option value="FIXED">Fixed</option>
            </select>
          </F>
          <F label="Surcharge value"><input className={inputCls} type="number" value={f.surchargeValue} onChange={(e) => set('surchargeValue', Number(e.target.value))} disabled={f.surchargeType === 'NONE'} /></F>
          <F label="Sort order"><input className={inputCls} type="number" value={f.sortOrder} onChange={(e) => set('sortOrder', Number(e.target.value))} /></F>
          <F label="AR due days"><input className={inputCls} type="number" value={f.arDueDays} onChange={(e) => set('arDueDays', Number(e.target.value))} /></F>
        </div>
        <div className="grid grid-cols-2 gap-1 text-sm text-slate-700">
          <Chk label="Reconciled as cash" v={f.isCash} on={(v) => set('isCash', v)} />
          <Chk label="Allows partial payment" v={f.allowsPartial} on={(v) => set('allowsPartial', v)} />
          <Chk label="Customer required" v={f.customerRequired} on={(v) => set('customerRequired', v)} />
          <Chk label="Accounts receivable" v={f.accountsReceivable} on={(v) => set('accountsReceivable', v)} />
          <Chk label="Active" v={f.active} on={(v) => set('active', v)} />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}

function Chk({ label, v, on }: { label: string; v: boolean; on: (v: boolean) => void }) {
  return <label className="flex items-center gap-2"><input type="checkbox" checked={v} onChange={(e) => on(e.target.checked)} /> {label}</label>
}
function F({ label, children }: { label: string; children: ReactNode }) {
  return <div><label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>{children}</div>
}
