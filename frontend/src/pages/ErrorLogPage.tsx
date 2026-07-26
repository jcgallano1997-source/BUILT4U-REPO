import { useEffect, useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost } from '@/components/Modal'
import { auditErr, getError, listErrors, type ErrorEntry } from '@/lib/audit'

export default function ErrorLogPage() {
  const [rows, setRows] = useState<ErrorEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<ErrorEntry | null>(null)

  async function reload() {
    setLoading(true)
    try { setRows(await listErrors(100)) }
    catch (e) { toast.error(auditErr(e, 'Failed to load error log')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function open(id: number) {
    try { setDetail(await getError(id)) } catch (e) { toast.error(auditErr(e, 'Failed to load')) }
  }

  return (
    <div className="space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <AlertTriangle size={18} className="text-amber-600" /> Error log
      </h1>
      <p className="text-sm text-slate-500">Unhandled server errors (5xx). Each carries a short reference a user can quote.</p>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">When</th>
              <th className="px-4 py-2 font-medium">Ref</th>
              <th className="px-4 py-2 font-medium">User</th>
              <th className="px-4 py-2 font-medium">Request</th>
              <th className="px-4 py-2 font-medium">Error</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No errors 🎉</td></tr>
              : rows.map((r) => (
                <tr key={r.id} className="cursor-pointer hover:bg-slate-50" onClick={() => open(r.id)}>
                  <td className="px-4 py-2 text-slate-500">{new Date(r.occurredAt).toLocaleString()}</td>
                  <td className="px-4 py-2 font-mono text-xs text-indigo-700">{r.ref}</td>
                  <td className="px-4 py-2 text-slate-600">{r.username ?? '—'}</td>
                  <td className="px-4 py-2 text-slate-500">{r.httpMethod} {r.requestPath}</td>
                  <td className="px-4 py-2 text-slate-600 truncate max-w-[18rem]">{r.message ?? r.exceptionClass}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && (
        <Modal title={`Error ${detail.ref}`} onClose={() => setDetail(null)} width="max-w-2xl">
          <div className="space-y-2 text-sm">
            <div className="text-slate-500">{new Date(detail.occurredAt).toLocaleString()} · {detail.username ?? 'anonymous'} · {detail.siteName ?? detail.siteCode ?? '—'}</div>
            <div className="text-slate-600">{detail.httpMethod} {detail.requestPath}</div>
            <div className="font-medium text-slate-800">{detail.exceptionClass}</div>
            <div className="text-slate-700">{detail.message}</div>
            {detail.stackTrace && (
              <pre className="max-h-80 overflow-auto rounded-md bg-slate-900 p-3 text-xs text-slate-100">{detail.stackTrace}</pre>
            )}
            <div className="flex justify-end pt-1"><button className={btnGhost} onClick={() => setDetail(null)}>Close</button></div>
          </div>
        </Modal>
      )}
    </div>
  )
}
