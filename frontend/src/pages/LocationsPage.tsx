import { useEffect, useState } from 'react'
import { MapPin, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { createLocation, deleteLocation, listLocations, updateLocation, type Location } from '@/lib/inventory'

function apiErr(e: unknown, f: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? f
}

export default function LocationsPage() {
  const [rows, setRows] = useState<Location[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<Location | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listLocations(includeInactive)) }
    catch (e) { toast.error(apiErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  async function remove(l: Location) {
    if (!confirm(`Deactivate location "${l.name}"?`)) return
    try { await deleteLocation(l.id); toast.success('Deactivated'); reload() }
    catch (e) { toast.error(apiErr(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <MapPin size={18} className="text-blue-600" /> Locations
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New location</button>
      </div>

      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Name</th><th className="px-4 py-2 font-medium">Capacity</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">No locations</td></tr>
              : rows.map((l) => (
                <tr key={l.id}>
                  <td className="px-4 py-2 text-slate-700">{l.name}</td>
                  <td className="px-4 py-2 text-slate-500">{l.capacity ?? '—'}</td>
                  <td className="px-4 py-2"><span className={l.active ? 'text-emerald-600' : 'text-slate-400'}>{l.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(l)}>Edit</button>
                    {l.active && <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(l)}><Trash2 size={13} /> Deactivate</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <LocForm loc={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </div>
  )
}

function LocForm({ loc, onClose, onSaved }: { loc: Location | null; onClose: () => void; onSaved: () => void }) {
  const isNew = loc === null
  const [name, setName] = useState(loc?.name ?? '')
  const [capacity, setCapacity] = useState(loc?.capacity != null ? String(loc.capacity) : '')
  const [active, setActive] = useState(loc?.active ?? true)
  const [saving, setSaving] = useState(false)
  async function save() {
    setSaving(true)
    try {
      const cap = capacity.trim() === '' ? undefined : Number(capacity)
      if (isNew) { await createLocation({ name: name.trim(), capacity: cap }); toast.success('Created') }
      else { await updateLocation(loc!.id, { name: name.trim(), capacity: cap, active }); toast.success('Updated') }
      onSaved()
    } catch (e) { toast.error(apiErr(e, 'Save failed')) } finally { setSaving(false) }
  }
  return (
    <Modal title={isNew ? 'New location' : `Edit ${loc!.name}`} onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Name</label>
          <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Capacity (optional)</label>
          <input className={inputCls} type="number" value={capacity} onChange={(e) => setCapacity(e.target.value)} />
        </div>
        {!isNew && <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} /> Active</label>}
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}
