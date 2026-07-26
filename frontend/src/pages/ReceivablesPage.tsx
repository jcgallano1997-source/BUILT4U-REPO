import { useEffect, useState } from 'react'
import { HandCoins, Wallet } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import {
  arapErr, collectReceivable, getReceivable, listReceivables,
  type LedgerStatus, type Receivable, type ReceivableDetail,
} from '@/lib/arap'

const STATUS_LABEL: Record<LedgerStatus, string> = {
  OPEN: 'Open', PARTIAL: 'Partial', PAID: 'Paid', CANCELLED: 'Cancelled',
}
const statusColor: Record<LedgerStatus, string> = {
  OPEN: 'text-indigo-600', PARTIAL: 'text-amber-600', PAID: 'text-emerald-600', CANCELLED: 'text-slate-400 line-through',
}

export default function ReceivablesPage() {
  const [rows, setRows] = useState<Receivable[]>([])
  const [status, setStatus] = useState<LedgerStatus | ''>('')
  const [overdue, setOverdue] = useState(false)
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<ReceivableDetail | null>(null)

  async function reload() {
    setLoading(true)
    try { setRows((await listReceivables({ status, overdue })).content) }
    catch (e) { toast.error(arapErr(e, 'Failed to load receivables')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, overdue])

  async function open(id: number) {
    try { setDetail(await getReceivable(id)) } catch (e) { toast.error(arapErr(e, 'Failed to load')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Wallet size={18} className="text-indigo-600" /> Accounts receivable
        </h1>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-500">
            <input type="checkbox" checked={overdue} onChange={(e) => setOverdue(e.target.checked)} /> Overdue only
          </label>
          <select className={`${inputCls} max-w-[11rem]`} value={status} onChange={(e) => setStatus(e.target.value as LedgerStatus | '')}>
            <option value="">All statuses</option>
            {(Object.keys(STATUS_LABEL) as LedgerStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
          </select>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Sale #</th>
              <th className="px-4 py-2 font-medium">Customer</th>
              <th className="px-4 py-2 font-medium">Due</th>
              <th className="px-4 py-2 font-medium text-right">Original</th>
              <th className="px-4 py-2 font-medium text-right">Balance</th>
              <th className="px-4 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No receivables</td></tr>
              : rows.map((r) => (
                <tr key={r.id} className="cursor-pointer hover:bg-slate-50" onClick={() => open(r.id)}>
                  <td className="px-4 py-2 font-medium text-indigo-700">{r.salesNumber}</td>
                  <td className="px-4 py-2 text-slate-600">{r.customerName}</td>
                  <td className={`px-4 py-2 ${r.overdue ? 'font-medium text-red-600' : 'text-slate-500'}`}>{r.dueDate}{r.overdue ? ' • overdue' : ''}</td>
                  <td className="px-4 py-2 text-right text-slate-500">{peso(r.originalAmount)}</td>
                  <td className="px-4 py-2 text-right font-medium">{peso(r.balance)}</td>
                  <td className={`px-4 py-2 ${statusColor[r.status]}`}>{STATUS_LABEL[r.status]}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && (
        <LedgerDetail
          title={`Receivable · ${detail.receivable.salesNumber}`}
          who={detail.receivable.customerName}
          d={detail.receivable}
          payments={detail.payments}
          collectVerb="Collect"
          onClose={() => setDetail(null)}
          onPay={async (amount, note) => {
            const updated = await collectReceivable(detail.receivable.id, amount, note)
            setDetail(updated); reload()
          }}
        />
      )}
    </div>
  )
}

/** Shared detail + record-payment modal, reused by AR and AP. */
export function LedgerDetail({
  title, who, d, payments, collectVerb, onClose, onPay,
}: {
  title: string
  who: string
  d: { originalAmount: number; amountPaid: number; balance: number; dueDate: string; status: LedgerStatus }
  payments: { id: number; amount: number; note: string | null; paidAt: string; createdBy: string | null }[]
  collectVerb: string
  onClose: () => void
  onPay: (amount: number, note?: string) => Promise<void>
}) {
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)
  const canPay = d.status === 'OPEN' || d.status === 'PARTIAL'

  async function submit() {
    const amt = Number(amount)
    if (!(amt > 0)) { toast.error('Enter an amount greater than zero'); return }
    if (amt > d.balance) { toast.error('Amount exceeds the outstanding balance'); return }
    setSaving(true)
    try { await onPay(amt, note.trim() || undefined); setAmount(''); setNote(''); toast.success('Recorded') }
    catch (e) { toast.error(arapErr(e, 'Failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={title} onClose={onClose} width="max-w-lg">
      <div className="space-y-3 text-sm">
        <div className="text-slate-500">{who} · due {d.dueDate} · <span className={statusColor[d.status]}>{STATUS_LABEL[d.status]}</span></div>
        <div className="grid grid-cols-3 gap-2 rounded-md bg-slate-50 p-3 text-center">
          <div><div className="text-xs text-slate-400">Original</div><div className="font-medium">{peso(d.originalAmount)}</div></div>
          <div><div className="text-xs text-slate-400">Paid</div><div className="font-medium text-emerald-600">{peso(d.amountPaid)}</div></div>
          <div><div className="text-xs text-slate-400">Balance</div><div className="font-medium">{peso(d.balance)}</div></div>
        </div>

        {canPay && (
          <div className="flex items-end gap-2 border-t border-slate-100 pt-3">
            <div className="flex-1"><label className="mb-1 block text-xs font-medium text-slate-600">Amount</label>
              <input className={inputCls} type="number" min={0} max={d.balance} value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
            <div className="flex-1"><label className="mb-1 block text-xs font-medium text-slate-600">Note</label>
              <input className={inputCls} value={note} onChange={(e) => setNote(e.target.value)} /></div>
            <button className={btnPrimary} disabled={saving} onClick={submit}><HandCoins size={14} /> {collectVerb}</button>
          </div>
        )}

        <div>
          <div className="mb-1 text-xs font-medium text-slate-500">History</div>
          {payments.length === 0 ? <p className="text-slate-400">No payments yet.</p> : (
            <table className="w-full">
              <tbody className="divide-y divide-slate-100">
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td className="py-1 text-slate-500">{new Date(p.paidAt).toLocaleString()}</td>
                    <td className="py-1 text-slate-500">{p.note ?? ''}</td>
                    <td className="py-1 text-right font-medium text-emerald-600">{peso(p.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="flex justify-end pt-1"><button className={btnGhost} onClick={onClose}>Close</button></div>
      </div>
    </Modal>
  )
}
