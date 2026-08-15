import { useEffect, useState } from 'react'
import { Plus, Ruler } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { createUom, listUoms, setUomActive, type Uom } from '@/lib/inventory'

function apiErr(e: unknown, f: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? f
}

export default function UomsPage() {
  const [rows, setRows] = useState<Uom[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [adding, setAdding] = useState(false)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listUoms(includeInactive)) }
    catch (e) { toast.error(apiErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  async function toggle(u: Uom) {
    try { await setUomActive(u.uom, !u.active); reload() }
    catch (e) { toast.error(apiErr(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Ruler size={18} className="text-blue-600" /> Units of measure
        </h1>
        <button className={btnPrimary} onClick={() => setAdding(true)}><Plus size={16} /> New unit</button>
      </div>

      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Unit</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={3} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={3} className="px-4 py-6 text-center text-slate-400">No units</td></tr>
              : rows.map((u) => (
                <tr key={u.uom}>
                  <td className="px-4 py-2 font-medium text-slate-700">{u.uom}</td>
                  <td className="px-4 py-2"><span className={u.active ? 'text-emerald-600' : 'text-slate-400'}>{u.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right">
                    <button className="text-blue-600 hover:underline" onClick={() => toggle(u)}>{u.active ? 'Deactivate' : 'Reactivate'}</button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {adding && <UomForm onClose={() => setAdding(false)} onSaved={() => { setAdding(false); reload() }} />}
    </div>
  )
}

function UomForm({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [uom, setUom] = useState('')
  const [saving, setSaving] = useState(false)
  async function save() {
    setSaving(true)
    try { await createUom(uom.trim()); toast.success('Created'); onSaved() }
    catch (e) { toast.error(apiErr(e, 'Save failed')) } finally { setSaving(false) }
  }
  return (
    <Modal title="New unit" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Unit (e.g. PCS, BOX, KG)</label>
          <input className={inputCls} value={uom} onChange={(e) => setUom(e.target.value)} autoFocus />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}
