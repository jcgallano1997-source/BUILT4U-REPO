import { useEffect, useState } from 'react'
import { GitBranch, Lock, Plus, X } from 'lucide-react'
import { toast } from 'sonner'
import { btnGhost, inputCls } from '@/components/Modal'
import {
  addApprover, listApprovers, listPoApprovers, procErr, removeApprover, setPoApprover,
  type Approver, type PoApprover,
} from '@/lib/procurement'

/**
 * PO approver admin, in two parts: who is allowed to approve (the pool — the
 * business owner is built-in, the rest are added/removed here), and which
 * approver each user's POs route to. A user with no approver auto-approves.
 * The IT/system administrator account is excluded server-side.
 */
export default function PoApproversPage() {
  const [rows, setRows] = useState<PoApprover[]>([])
  const [approvers, setApprovers] = useState<Approver[]>([])
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [busyPool, setBusyPool] = useState(false)
  const [toAdd, setToAdd] = useState('')

  async function reload() {
    setLoading(true)
    try {
      const [r, a] = await Promise.all([listPoApprovers(), listApprovers()])
      setRows(r)
      setApprovers(a)
    } catch (e) { toast.error(procErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function change(userId: number, approverUserId: number | null) {
    setSavingId(userId)
    try {
      await setPoApprover(userId, approverUserId)
      toast.success(approverUserId ? 'Approver set' : 'Reverted to auto-approve')
      await reload()
    } catch (e) { toast.error(procErr(e, 'Update failed')) } finally { setSavingId(null) }
  }

  async function add() {
    if (!toAdd) return
    setBusyPool(true)
    try {
      await addApprover(Number(toAdd))
      toast.success('Approver added')
      setToAdd('')
      await reload()
    } catch (e) { toast.error(procErr(e, 'Could not add approver')) } finally { setBusyPool(false) }
  }

  async function drop(a: Approver) {
    setBusyPool(true)
    try {
      await removeApprover(a.userId)
      toast.success(`${a.fullName} is no longer an approver`)
      await reload()
    } catch (e) { toast.error(procErr(e, 'Could not remove approver')) } finally { setBusyPool(false) }
  }

  const approverIds = new Set(approvers.map((a) => a.userId))
  const candidates = rows.filter((u) => !approverIds.has(u.userId))

  return (
    <div className="space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <GitBranch size={18} className="text-blue-600" /> PO approvers
      </h1>
      <p className="text-sm text-slate-500">
        A user with no approver auto-approves their own purchase orders. Assign an approver to make their POs
        require sign-off before they can be received.
      </p>

      {/* Who may approve */}
      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-1 text-sm font-semibold text-slate-700">Who can approve</div>
        <p className="mb-3 text-xs text-slate-500">
          The business owner is always an approver. Add anyone else who should be able to sign off POs.
        </p>

        <div className="mb-3 flex flex-wrap gap-2">
          {approvers.length === 0 && <span className="text-sm text-slate-400">No approvers yet.</span>}
          {approvers.map((a) => (
            <span key={a.userId}
              className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 py-1 pl-3 pr-2 text-sm">
              <span className="font-medium text-slate-700">{a.fullName}</span>
              <span className="text-xs text-slate-400">{a.username}</span>
              {a.builtIn ? (
                <span className="ml-0.5 inline-flex items-center gap-1 rounded-full bg-blue-50 px-2 py-0.5 text-[11px] font-semibold text-blue-700"
                  title="The business owner is a built-in approver and cannot be removed">
                  <Lock size={10} /> Built-in
                </span>
              ) : (
                <button type="button" disabled={busyPool} onClick={() => drop(a)}
                  className="ml-0.5 rounded-full p-0.5 text-slate-400 hover:bg-slate-200 hover:text-slate-600 disabled:opacity-50"
                  aria-label={`Remove ${a.fullName} as an approver`} title="Remove as approver">
                  <X size={13} />
                </button>
              )}
            </span>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <select className={`${inputCls} max-w-xs`} value={toAdd} disabled={busyPool || candidates.length === 0}
            onChange={(e) => setToAdd(e.target.value)}>
            <option value="">{candidates.length ? 'Add an approver…' : 'Everyone is already an approver'}</option>
            {candidates.map((u) => (
              <option key={u.userId} value={u.userId}>{u.fullName} ({u.username})</option>
            ))}
          </select>
          <button className={btnGhost} disabled={!toAdd || busyPool} onClick={add}>
            <Plus size={14} /> Add
          </button>
        </div>
      </div>

      {/* Routing */}
      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">User</th>
              <th className="px-4 py-2 font-medium">Routes to approver</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={2} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={2} className="px-4 py-6 text-center text-slate-400">No users</td></tr>
              : rows.map((u) => (
                <tr key={u.userId}>
                  <td className="px-4 py-2">
                    <div className="font-medium text-slate-700">{u.fullName}</div>
                    <div className="text-xs text-slate-400">{u.username}</div>
                  </td>
                  <td className="px-4 py-2">
                    <select
                      className={`${inputCls} max-w-xs`}
                      disabled={savingId === u.userId}
                      value={u.approverUserId ?? ''}
                      onChange={(e) => change(u.userId, e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">Auto-approve (no approver)</option>
                      {/* Only approvers may be routed to — and never yourself. */}
                      {approvers.filter((a) => a.userId !== u.userId).map((a) => (
                        <option key={a.userId} value={a.userId}>{a.fullName} ({a.username})</option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
