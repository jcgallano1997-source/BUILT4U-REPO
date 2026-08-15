import { useEffect, useState, type ReactNode } from 'react'
import { Plus, Ticket, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import { createVoucher, deleteVoucher, listVouchers, promoErr, updateVoucher, type DiscountType, type Voucher } from '@/lib/promo'

export default function VouchersPage() {
  const [rows, setRows] = useState<Voucher[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<Voucher | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listVouchers(undefined, includeInactive)) }
    catch (e) { toast.error(promoErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  async function remove(v: Voucher) {
    if (!confirm(`Deactivate voucher "${v.code}"?`)) return
    try { await deleteVoucher(v.id); toast.success('Deactivated'); reload() } catch (e) { toast.error(promoErr(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Ticket size={18} className="text-blue-600" /> Vouchers
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New voucher</button>
      </div>
      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Code</th><th className="px-4 py-2 font-medium">Discount</th><th className="px-4 py-2 font-medium">Valid</th><th className="px-4 py-2 font-medium text-right">Used</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No vouchers</td></tr>
              : rows.map((v) => (
                <tr key={v.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">{v.code}{v.description && <div className="text-xs text-slate-400">{v.description}</div>}</td>
                  <td className="px-4 py-2 text-slate-600">{v.discountType === 'PERCENT' ? `${v.discountValue}%${v.maxDiscount ? ` (max ${peso(v.maxDiscount)})` : ''}` : peso(v.discountValue)}{v.minSpend ? <span className="text-xs text-slate-400"> · min {peso(v.minSpend)}</span> : null}</td>
                  <td className="px-4 py-2 text-slate-500">{v.validFrom ?? '—'} → {v.validTo ?? '—'}</td>
                  <td className="px-4 py-2 text-right text-slate-500">{v.usedCount}{v.usageLimit ? ` / ${v.usageLimit}` : ''}</td>
                  <td className="px-4 py-2"><span className={v.active ? 'text-emerald-600' : 'text-slate-400'}>{v.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(v)}>Edit</button>
                    {v.active && <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(v)}><Trash2 size={13} /> Deactivate</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <VForm v={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </div>
  )
}

function VForm({ v, onClose, onSaved }: { v: Voucher | null; onClose: () => void; onSaved: () => void }) {
  const isNew = v === null
  const [f, setF] = useState({
    code: v?.code ?? '', description: v?.description ?? '',
    discountType: (v?.discountType ?? 'PERCENT') as DiscountType,
    discountValue: v ? String(v.discountValue) : '10',
    maxDiscount: v?.maxDiscount != null ? String(v.maxDiscount) : '',
    minSpend: v?.minSpend != null ? String(v.minSpend) : '',
    validFrom: v?.validFrom ?? '', validTo: v?.validTo ?? '',
    usageLimit: v?.usageLimit != null ? String(v.usageLimit) : '',
    active: v?.active ?? true,
  })
  const [saving, setSaving] = useState(false)
  const s = (k: keyof typeof f, val: string | boolean) => setF((p) => ({ ...p, [k]: val }))

  async function save() {
    if (!f.code.trim()) { toast.error('Code is required'); return }
    setSaving(true)
    try {
      const body = {
        code: f.code.trim(), description: f.description.trim() || undefined,
        discountType: f.discountType, discountValue: Number(f.discountValue),
        maxDiscount: f.discountType === 'PERCENT' && f.maxDiscount ? Number(f.maxDiscount) : undefined,
        minSpend: f.minSpend ? Number(f.minSpend) : undefined,
        validFrom: f.validFrom || undefined, validTo: f.validTo || undefined,
        usageLimit: f.usageLimit ? Number(f.usageLimit) : undefined, active: f.active,
      }
      if (isNew) await createVoucher(body); else await updateVoucher(v!.id, body)
      toast.success('Saved'); onSaved()
    } catch (e) { toast.error(promoErr(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={isNew ? 'New voucher' : `Edit ${v!.code}`} onClose={onClose} width="max-w-lg">
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <F label="Code"><input className={inputCls} value={f.code} onChange={(e) => s('code', e.target.value.toUpperCase())} autoFocus /></F>
          <F label="Type"><select className={inputCls} value={f.discountType} onChange={(e) => s('discountType', e.target.value)}><option value="PERCENT">Percent</option><option value="FIXED">Fixed ₱</option></select></F>
        </div>
        <F label="Description"><input className={inputCls} value={f.description} onChange={(e) => s('description', e.target.value)} /></F>
        <div className="grid grid-cols-2 gap-3">
          <F label={f.discountType === 'PERCENT' ? 'Percent off' : 'Amount off (₱)'}><input className={inputCls} type="number" min={0} value={f.discountValue} onChange={(e) => s('discountValue', e.target.value)} /></F>
          {f.discountType === 'PERCENT'
            ? <F label="Max discount (₱, optional)"><input className={inputCls} type="number" min={0} value={f.maxDiscount} onChange={(e) => s('maxDiscount', e.target.value)} /></F>
            : <div />}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <F label="Min spend (₱, optional)"><input className={inputCls} type="number" min={0} value={f.minSpend} onChange={(e) => s('minSpend', e.target.value)} /></F>
          <F label="Usage limit (optional)"><input className={inputCls} type="number" min={1} value={f.usageLimit} onChange={(e) => s('usageLimit', e.target.value)} /></F>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <F label="Valid from"><input className={inputCls} type="date" value={f.validFrom} onChange={(e) => s('validFrom', e.target.value)} /></F>
          <F label="Valid to"><input className={inputCls} type="date" value={f.validTo} onChange={(e) => s('validTo', e.target.value)} /></F>
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={f.active} onChange={(e) => s('active', e.target.checked)} /> Active</label>
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
