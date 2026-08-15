import { useEffect, useState } from 'react'
import { Plus, Store } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { createSite, listSites, updateSite, type SiteSummary } from '@/lib/admin'

function apiErr(e: unknown, fallback: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? fallback
}

export default function SitesPage() {
  const [sites, setSites] = useState<SiteSummary[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<SiteSummary | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try {
      setSites(await listSites(includeInactive))
    } catch (e) {
      toast.error(apiErr(e, 'Failed to load sites'))
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Store size={18} className="text-blue-600" /> Sites
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}>
          <Plus size={16} /> New site
        </button>
      </div>

      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} />
        Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Code</th>
              <th className="px-4 py-2 font-medium">Name</th>
              <th className="px-4 py-2 font-medium">Address</th>
              <th className="px-4 py-2 font-medium">Users</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
            ) : sites.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No sites</td></tr>
            ) : (
              sites.map((s) => (
                <tr key={s.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">{s.code}</td>
                  <td className="px-4 py-2">{s.name}</td>
                  <td className="px-4 py-2 text-slate-500">{s.address ?? '—'}</td>
                  <td className="px-4 py-2 text-slate-500">{s.userCount}</td>
                  <td className="px-4 py-2">
                    <span className={s.active ? 'text-emerald-600' : 'text-slate-400'}>
                      {s.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-right">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(s)}>Edit</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <SiteForm
          site={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); reload() }}
        />
      )}
    </div>
  )
}

function SiteForm({ site, onClose, onSaved }: { site: SiteSummary | null; onClose: () => void; onSaved: () => void }) {
  const isNew = site === null
  const [code, setCode] = useState(site?.code ?? '')
  const [name, setName] = useState(site?.name ?? '')
  const [address, setAddress] = useState(site?.address ?? '')
  const [active, setActive] = useState(site?.active ?? true)
  const [saving, setSaving] = useState(false)

  async function save() {
    setSaving(true)
    try {
      if (isNew) {
        await createSite({ code: code.trim().toUpperCase(), name: name.trim(), address: address.trim() || undefined })
        toast.success('Site created')
      } else {
        await updateSite(site!.id, { name: name.trim(), address: address.trim() || undefined, active })
        toast.success('Site updated')
      }
      onSaved()
    } catch (e) {
      toast.error(apiErr(e, 'Save failed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isNew ? 'New site' : `Edit ${site!.code}`} onClose={onClose}>
      <div className="space-y-4">
        {isNew && (
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Code</label>
            <input className={inputCls} value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="BR2" />
            <p className="mt-1 text-xs text-slate-400">Uppercase letters, digits, dash/underscore. Immutable after creation.</p>
          </div>
        )}
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Name</label>
          <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} placeholder="Branch 2" />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Address</label>
          <input className={inputCls} value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
        {!isNew && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} /> Active
          </label>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}
