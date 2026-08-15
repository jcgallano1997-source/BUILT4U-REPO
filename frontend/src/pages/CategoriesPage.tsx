import { useEffect, useState } from 'react'
import { Plus, Tags, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { createCategory, deleteCategory, listCategories, updateCategory, type Category } from '@/lib/inventory'

function apiErr(e: unknown, f: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? f
}

export default function CategoriesPage() {
  const [rows, setRows] = useState<Category[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<Category | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try { setRows(await listCategories(includeInactive)) }
    catch (e) { toast.error(apiErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  async function remove(c: Category) {
    if (!confirm(`Deactivate category "${c.name}"?`)) return
    try { await deleteCategory(c.id); toast.success('Deactivated'); reload() }
    catch (e) { toast.error(apiErr(e, 'Failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Tags size={18} className="text-blue-600" /> Categories
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New category</button>
      </div>

      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr><th className="px-4 py-2 font-medium">Name</th><th className="px-4 py-2 font-medium">Status</th><th className="px-4 py-2"></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={3} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={3} className="px-4 py-6 text-center text-slate-400">No categories</td></tr>
              : rows.map((c) => (
                <tr key={c.id}>
                  <td className="px-4 py-2 text-slate-700">{c.name}</td>
                  <td className="px-4 py-2"><span className={c.active ? 'text-emerald-600' : 'text-slate-400'}>{c.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(c)}>Edit</button>
                    {c.active && <button className="ml-3 inline-flex items-center gap-1 text-red-600 hover:underline" onClick={() => remove(c)}><Trash2 size={13} /> Deactivate</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && <CatForm cat={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />}
    </div>
  )
}

function CatForm({ cat, onClose, onSaved }: { cat: Category | null; onClose: () => void; onSaved: () => void }) {
  const isNew = cat === null
  const [name, setName] = useState(cat?.name ?? '')
  const [active, setActive] = useState(cat?.active ?? true)
  const [saving, setSaving] = useState(false)
  async function save() {
    setSaving(true)
    try {
      if (isNew) { await createCategory(name.trim()); toast.success('Created') }
      else { await updateCategory(cat!.id, { name: name.trim(), active }); toast.success('Updated') }
      onSaved()
    } catch (e) { toast.error(apiErr(e, 'Save failed')) } finally { setSaving(false) }
  }
  return (
    <Modal title={isNew ? 'New category' : `Edit ${cat!.name}`} onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Name</label>
          <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
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
