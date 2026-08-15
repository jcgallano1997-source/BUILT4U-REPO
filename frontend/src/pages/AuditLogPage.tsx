import { useEffect, useState } from 'react'
import { FileText, History, Sheet } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, inputCls } from '@/components/Modal'
import { auditErr, downloadAudit, listAudit, type AuditEntry, type AuditFilters } from '@/lib/audit'

const actionColor: Record<string, string> = {
  CREATE: 'text-emerald-600', UPDATE: 'text-amber-600', DELETE: 'text-red-600',
}

interface Change { field: string; old: string; new: string }

export default function AuditLogPage() {
  const [rows, setRows] = useState<AuditEntry[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<AuditEntry | null>(null)
  const [f, setF] = useState<AuditFilters>({ entity: '', action: '', q: '' })

  async function reload(p = page) {
    setLoading(true)
    try {
      const res = await listAudit({ ...f, page: p })
      setRows(res.content); setTotalPages(res.totalPages || 1); setPage(res.number)
    } catch (e) { toast.error(auditErr(e, 'Failed to load audit log')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload(0) // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [f.entity, f.action, f.q])

  async function download(format: 'pdf' | 'xlsx') {
    try { await downloadAudit(format, f) } catch (e) { toast.error(auditErr(e, 'Download failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <History size={18} className="text-blue-600" /> Audit log
        </h1>
        <div className="flex gap-2">
          <button className={btnGhost} onClick={() => download('pdf')}><FileText size={14} /> PDF</button>
          <button className={btnGhost} onClick={() => download('xlsx')}><Sheet size={14} /> Excel</button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <input className={`${inputCls} max-w-[12rem]`} placeholder="Search id / reference…" value={f.q ?? ''} onChange={(e) => setF((p) => ({ ...p, q: e.target.value }))} />
        <input className={`${inputCls} max-w-[10rem]`} placeholder="Entity (e.g. Item)" value={f.entity ?? ''} onChange={(e) => setF((p) => ({ ...p, entity: e.target.value }))} />
        <select className={`${inputCls} max-w-[9rem]`} value={f.action ?? ''} onChange={(e) => setF((p) => ({ ...p, action: e.target.value }))}>
          <option value="">All actions</option>
          <option value="CREATE">Create</option>
          <option value="UPDATE">Update</option>
          <option value="DELETE">Delete</option>
        </select>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">When</th>
              <th className="px-4 py-2 font-medium">User</th>
              <th className="px-4 py-2 font-medium">Action</th>
              <th className="px-4 py-2 font-medium">Entity</th>
              <th className="px-4 py-2 font-medium">Id / reference</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No changes</td></tr>
              : rows.map((r) => (
                <tr key={r.id} className="cursor-pointer hover:bg-slate-50" onClick={() => setDetail(r)}>
                  <td className="px-4 py-2 text-slate-500">{new Date(r.occurredAt).toLocaleString()}</td>
                  <td className="px-4 py-2 text-slate-700">{r.username}</td>
                  <td className={`px-4 py-2 font-medium ${actionColor[r.action] ?? 'text-slate-500'}`}>{r.action}</td>
                  <td className="px-4 py-2 text-slate-600">{r.entityName}</td>
                  <td className="px-4 py-2 text-slate-500">{r.reference ?? r.entityId ?? '—'}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-slate-500">
        <span>Page {page + 1} of {totalPages}</span>
        <div className="flex gap-2">
          <button className={btnGhost} disabled={page <= 0} onClick={() => reload(page - 1)}>Prev</button>
          <button className={btnGhost} disabled={page >= totalPages - 1} onClick={() => reload(page + 1)}>Next</button>
        </div>
      </div>

      {detail && <ChangeModal entry={detail} onClose={() => setDetail(null)} />}
    </div>
  )
}

function ChangeModal({ entry, onClose }: { entry: AuditEntry; onClose: () => void }) {
  let changes: Change[] = []
  try { if (entry.changes) changes = JSON.parse(entry.changes) } catch { /* leave empty */ }

  return (
    <Modal title={`${entry.action} · ${entry.entityName}`} onClose={onClose} width="max-w-xl">
      <div className="space-y-3 text-sm">
        <div className="text-slate-500">
          {new Date(entry.occurredAt).toLocaleString()} · by {entry.username}
          {entry.reference && <> · {entry.reference}</>}
        </div>
        {entry.entityId && <div className="text-xs text-slate-400">{entry.entityId}</div>}
        {changes.length === 0 ? <p className="text-slate-400">No field-level detail recorded.</p> : (
          <table className="w-full">
            <thead className="text-left text-slate-400"><tr><th className="py-1">Field</th><th className="py-1">Old</th><th className="py-1">New</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {changes.map((c, i) => (
                <tr key={i}>
                  <td className="py-1 pr-2 font-medium text-slate-700">{c.field}</td>
                  <td className="py-1 pr-2 text-slate-400">{c.old || '—'}</td>
                  <td className="py-1 text-slate-700">{c.new || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="flex justify-end"><button className={btnGhost} onClick={onClose}>Close</button></div>
      </div>
    </Modal>
  )
}
