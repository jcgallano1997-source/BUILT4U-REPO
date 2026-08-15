import { useEffect, useState } from 'react'
import { Landmark, Plus } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import {
  arapErr, createExpense, getPayable, listPayables, payPayable,
  type LedgerStatus, type Payable, type PayableDetail, type PayableSource,
} from '@/lib/arap'
import { LedgerDetail } from '@/pages/ReceivablesPage'

const STATUS_LABEL: Record<LedgerStatus, string> = {
  OPEN: 'Open', PARTIAL: 'Partial', PAID: 'Paid', CANCELLED: 'Cancelled',
}
const statusColor: Record<LedgerStatus, string> = {
  OPEN: 'text-blue-600', PARTIAL: 'text-amber-600', PAID: 'text-emerald-600', CANCELLED: 'text-slate-400 line-through',
}

export default function PayablesPage() {
  const [rows, setRows] = useState<Payable[]>([])
  const [status, setStatus] = useState<LedgerStatus | ''>('')
  const [source, setSource] = useState<PayableSource | ''>('')
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<PayableDetail | null>(null)
  const [expensing, setExpensing] = useState(false)

  async function reload() {
    setLoading(true)
    try { setRows((await listPayables({ status, source })).content) }
    catch (e) { toast.error(arapErr(e, 'Failed to load payables')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, source])

  async function open(id: number) {
    try { setDetail(await getPayable(id)) } catch (e) { toast.error(arapErr(e, 'Failed to load')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Landmark size={18} className="text-blue-600" /> Accounts payable
        </h1>
        <div className="flex items-center gap-2">
          <select className={`${inputCls} max-w-[9rem]`} value={source} onChange={(e) => setSource(e.target.value as PayableSource | '')}>
            <option value="">All sources</option>
            <option value="PURCHASE">Purchases</option>
            <option value="EXPENSE">Expenses</option>
          </select>
          <select className={`${inputCls} max-w-[9rem]`} value={status} onChange={(e) => setStatus(e.target.value as LedgerStatus | '')}>
            <option value="">All statuses</option>
            {(Object.keys(STATUS_LABEL) as LedgerStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
          </select>
          <button className={btnPrimary} onClick={() => setExpensing(true)}><Plus size={16} /> New expense</button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Payee</th>
              <th className="px-4 py-2 font-medium">Source</th>
              <th className="px-4 py-2 font-medium">Ref</th>
              <th className="px-4 py-2 font-medium">Due</th>
              <th className="px-4 py-2 font-medium text-right">Balance</th>
              <th className="px-4 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No payables</td></tr>
              : rows.map((p) => (
                <tr key={p.id} className="cursor-pointer hover:bg-slate-50" onClick={() => open(p.id)}>
                  <td className="px-4 py-2 font-medium text-slate-700">{p.payeeName}</td>
                  <td className="px-4 py-2 text-slate-500">{p.source === 'PURCHASE' ? 'Purchase' : (p.category ?? 'Expense')}</td>
                  <td className="px-4 py-2 text-slate-500">{p.grNumber ?? p.poNumber ?? '—'}</td>
                  <td className={`px-4 py-2 ${p.overdue ? 'font-medium text-red-600' : 'text-slate-500'}`}>{p.dueDate}{p.overdue ? ' • overdue' : ''}</td>
                  <td className="px-4 py-2 text-right font-medium">{peso(p.balance)}</td>
                  <td className={`px-4 py-2 ${statusColor[p.status]}`}>{STATUS_LABEL[p.status]}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && (
        <LedgerDetail
          title={`Payable · ${detail.payable.payeeName}`}
          who={detail.payable.source === 'PURCHASE'
            ? `Purchase${detail.payable.grNumber ? ' · ' + detail.payable.grNumber : ''}`
            : (detail.payable.category ?? 'Expense')}
          d={detail.payable}
          payments={detail.payments}
          collectVerb="Pay"
          onClose={() => setDetail(null)}
          onPay={async (amount, note) => {
            const updated = await payPayable(detail.payable.id, amount, note)
            setDetail(updated); reload()
          }}
        />
      )}

      {expensing && <ExpenseForm onClose={() => setExpensing(false)} onDone={() => { setExpensing(false); reload() }} />}
    </div>
  )
}

function ExpenseForm({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const today = new Date().toISOString().slice(0, 10)
  const [f, setF] = useState({ payeeName: '', category: '', description: '', amount: '', dueDate: today })
  const [saving, setSaving] = useState(false)
  const s = (k: keyof typeof f, v: string) => setF((p) => ({ ...p, [k]: v }))

  async function save() {
    if (!f.payeeName.trim()) { toast.error('Payee is required'); return }
    if (!(Number(f.amount) > 0)) { toast.error('Amount must be greater than zero'); return }
    setSaving(true)
    try {
      await createExpense({
        payeeName: f.payeeName.trim(), category: f.category.trim() || undefined,
        description: f.description.trim() || undefined, amount: Number(f.amount), dueDate: f.dueDate,
      })
      toast.success('Expense recorded'); onDone()
    } catch (e) { toast.error(arapErr(e, 'Failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title="New expense payable" onClose={onClose}>
      <div className="space-y-3">
        <div><label className="mb-1 block text-sm font-medium text-slate-700">Payee</label>
          <input className={inputCls} value={f.payeeName} onChange={(e) => s('payeeName', e.target.value)} autoFocus placeholder="e.g. Meralco" /></div>
        <div><label className="mb-1 block text-sm font-medium text-slate-700">Category</label>
          <input className={inputCls} value={f.category} onChange={(e) => s('category', e.target.value)} placeholder="utilities / rent / reimbursement" list="exp-cats" />
          <datalist id="exp-cats"><option value="utilities" /><option value="rent" /><option value="salaries" /><option value="reimbursement" /><option value="other" /></datalist>
        </div>
        <div><label className="mb-1 block text-sm font-medium text-slate-700">Description</label>
          <input className={inputCls} value={f.description} onChange={(e) => s('description', e.target.value)} /></div>
        <div className="grid grid-cols-2 gap-3">
          <div><label className="mb-1 block text-sm font-medium text-slate-700">Amount (₱)</label>
            <input className={inputCls} type="number" min={0} value={f.amount} onChange={(e) => s('amount', e.target.value)} /></div>
          <div><label className="mb-1 block text-sm font-medium text-slate-700">Due date</label>
            <input className={inputCls} type="date" value={f.dueDate} onChange={(e) => s('dueDate', e.target.value)} /></div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Create'}</button>
        </div>
      </div>
    </Modal>
  )
}
