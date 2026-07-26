import { useEffect, useState } from 'react'
import { GitBranch } from 'lucide-react'
import { toast } from 'sonner'
import { listPoApprovers, procErr, setPoApprover, type PoApprover } from '@/lib/procurement'

/**
 * Per-user PO approver routing. A user with no approver auto-approves their own
 * POs on create; assigning an approver makes their POs start as DRAFT until the
 * designated approver (or an ADMIN) approves.
 */
export default function PoApproversPage() {
  const [rows, setRows] = useState<PoApprover[]>([])
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)

  async function reload() {
    setLoading(true)
    try { setRows(await listPoApprovers()) }
    catch (e) { toast.error(procErr(e, 'Failed to load')) }
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

  return (
    <div className="space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <GitBranch size={18} className="text-indigo-600" /> PO approvers
      </h1>
      <p className="text-sm text-slate-500">
        A user with no approver auto-approves their own purchase orders. Assign an approver to make their POs
        require sign-off before they can be received.
      </p>

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
                      className="w-full max-w-xs rounded-md border border-slate-300 px-2 py-1.5"
                      disabled={savingId === u.userId}
                      value={u.approverUserId ?? ''}
                      onChange={(e) => change(u.userId, e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">Auto-approve (no approver)</option>
                      {rows.filter((o) => o.userId !== u.userId).map((o) => (
                        <option key={o.userId} value={o.userId}>{o.fullName} ({o.username})</option>
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
