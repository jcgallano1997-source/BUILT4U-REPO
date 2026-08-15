import { useEffect, useState } from 'react'
import { ClipboardList, LockKeyhole, PlayCircle, Plus } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import {
  closeShift, getCurrentShift, listCashMovements, listMyShifts, openShift, peso, posErr,
  recordCashMovement, PH_DENOMS, type CashMovement, type Shift, type ShiftSummary,
} from '@/lib/pos'

export default function ShiftsPage() {
  const [current, setCurrent] = useState<Shift | null | 'loading'>('loading')
  const [history, setHistory] = useState<ShiftSummary[]>([])
  const [opening, setOpening] = useState(false)
  const [closing, setClosing] = useState(false)
  const [float, setFloat] = useState('')

  async function reload() {
    const [c, h] = await Promise.all([getCurrentShift(), listMyShifts()])
    setCurrent(c); setHistory(h)
  }
  useEffect(() => { reload().catch(() => setCurrent(null)) }, [])

  async function doOpen() {
    setOpening(true)
    try {
      await openShift(Number(float || 0))
      toast.success('Shift opened'); setFloat(''); reload()
    } catch (e) { toast.error(posErr(e, 'Could not open shift')) } finally { setOpening(false) }
  }

  return (
    <div className="space-y-5">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <ClipboardList size={18} className="text-blue-600" /> Shifts
      </h1>

      {current === 'loading' ? <p className="text-slate-400">Loading…</p>
        : current === null ? (
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="mb-2 font-medium text-slate-700">Open a shift</h2>
            <div className="flex items-end gap-2">
              <div>
                <label className="mb-1 block text-sm text-slate-500">Opening cash float</label>
                <input className={inputCls} type="number" value={float} onChange={(e) => setFloat(e.target.value)} placeholder="1000.00" />
              </div>
              <button className={btnPrimary} disabled={opening} onClick={doOpen}><PlayCircle size={16} /> Open shift</button>
            </div>
          </div>
        ) : (
          <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-5">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-medium text-slate-800">Current shift · {current.shiftNumber}</h2>
              <button className={btnPrimary} onClick={() => setClosing(true)}><LockKeyhole size={15} /> Close shift</button>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
              <Stat label="Opening float" value={peso(current.openingFloat)} />
              <Stat label="Cash sales" value={peso(current.cashSalesTotal)} />
              <Stat label="Cash refunds" value={peso(current.cashRefundsTotal)} />
              <Stat label="Cash in" value={peso(current.cashIn)} />
              <Stat label="Cash out" value={peso(current.cashOut)} />
              <Stat label="Expected cash" value={peso(current.expectedCash)} accent />
              <Stat label="GCash" value={peso(current.gcashTotal)} />
              <Stat label="Sales" value={String(current.saleCount)} />
            </div>
            <CashDrawer shiftNumber={current.shiftNumber} onChange={reload} />
          </div>
        )}

      <div>
        <h2 className="mb-2 font-medium text-slate-700">My shift history</h2>
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-slate-500">
              <tr>
                <th className="px-4 py-2 font-medium">Shift</th>
                <th className="px-4 py-2 font-medium">Opened</th>
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium text-right">Expected</th>
                <th className="px-4 py-2 font-medium text-right">Counted</th>
                <th className="px-4 py-2 font-medium text-right">Variance</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {history.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No shifts yet</td></tr>
                : history.map((s) => (
                  <tr key={s.shiftNumber}>
                    <td className="px-4 py-2 font-medium text-slate-700">{s.shiftNumber}</td>
                    <td className="px-4 py-2 text-slate-500">{new Date(s.openedAt).toLocaleString()}</td>
                    <td className="px-4 py-2"><span className={s.status === 'OPEN' ? 'text-emerald-600' : 'text-slate-400'}>{s.status}</span></td>
                    <td className="px-4 py-2 text-right">{peso(s.expectedCash)}</td>
                    <td className="px-4 py-2 text-right">{s.countedCash == null ? '—' : peso(s.countedCash)}</td>
                    <td className={`px-4 py-2 text-right ${s.cashVariance == null ? '' : s.cashVariance < 0 ? 'text-red-600' : s.cashVariance > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
                      {s.cashVariance == null ? '—' : peso(s.cashVariance)}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {closing && current !== 'loading' && current && (
        <CloseForm shift={current} onClose={() => setClosing(false)} onDone={() => { setClosing(false); reload() }} />
      )}
    </div>
  )
}

function Stat({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="rounded-lg bg-white p-3 border border-slate-200">
      <div className="text-xs text-slate-400">{label}</div>
      <div className={`text-sm font-medium ${accent ? 'text-blue-700' : 'text-slate-700'}`}>{value}</div>
    </div>
  )
}

/** Record & list mid-shift cash drawer movements (pay-ins, paid-outs). */
function CashDrawer({ shiftNumber, onChange }: { shiftNumber: string; onChange: () => void }) {
  const [moves, setMoves] = useState<CashMovement[]>([])
  const [dir, setDir] = useState<'IN' | 'OUT'>('OUT')
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const load = () => listCashMovements(shiftNumber).then(setMoves).catch(() => {})
  useEffect(() => { load() }, [shiftNumber])

  async function add() {
    if (!(Number(amount) > 0)) { toast.error('Enter an amount'); return }
    setSaving(true)
    try {
      await recordCashMovement(shiftNumber, { direction: dir, amount: Number(amount), reason: reason.trim() || undefined })
      toast.success('Recorded'); setAmount(''); setReason(''); load(); onChange()
    } catch (e) { toast.error(posErr(e, 'Could not record')) } finally { setSaving(false) }
  }

  return (
    <div className="mt-4 rounded-lg border border-slate-200 bg-white p-3">
      <div className="mb-2 text-sm font-medium text-slate-700">Cash in / out</div>
      <div className="flex flex-wrap items-end gap-2">
        <select className={`${inputCls} w-32`} value={dir} onChange={(e) => setDir(e.target.value as 'IN' | 'OUT')}>
          <option value="OUT">Cash out</option>
          <option value="IN">Cash in</option>
        </select>
        <input className={`${inputCls} w-28`} type="number" placeholder="Amount" value={amount} onChange={(e) => setAmount(e.target.value)} />
        <input className={`${inputCls} min-w-40 flex-1`} placeholder="Reason (e.g. supplier payment)" value={reason} onChange={(e) => setReason(e.target.value)} />
        <button className={btnGhost} disabled={saving} onClick={add}><Plus size={14} /> Record</button>
      </div>
      {moves.length > 0 && (
        <ul className="mt-3 space-y-1 text-sm">
          {moves.map((m) => (
            <li key={m.movementId} className="flex items-center justify-between border-t border-slate-100 pt-1">
              <span className="text-slate-500">
                <span className={m.direction === 'IN' ? 'font-medium text-emerald-600' : 'font-medium text-amber-600'}>{m.direction === 'IN' ? '+ In' : '− Out'}</span>
                {m.reason ? ` · ${m.reason}` : ''}
              </span>
              <span className="font-medium text-slate-700">{peso(m.amount)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function CloseForm({
  shift, onClose, onDone,
}: { shift: Shift; onClose: () => void; onDone: () => void }) {
  const [qtys, setQtys] = useState<Record<number, string>>({})
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)
  const counted = PH_DENOMS.reduce((s, d) => s + d * (Number(qtys[d]) || 0), 0)
  const variance = counted - shift.expectedCash

  async function submit() {
    setSaving(true)
    try {
      const denominations = PH_DENOMS
        .filter((d) => Number(qtys[d]) > 0)
        .map((d) => ({ denom: d, qty: Number(qtys[d]) }))
      await closeShift(shift.shiftNumber, { countedCash: counted, closeNote: note.trim() || undefined, denominations })
      toast.success('Shift closed'); onDone()
    } catch (e) { toast.error(posErr(e, 'Could not close shift')) } finally { setSaving(false) }
  }

  return (
    <Modal title={`Close shift ${shift.shiftNumber}`} onClose={onClose}>
      <div className="space-y-4">
        <p className="text-sm text-slate-500">Expected cash in drawer: <span className="font-medium text-slate-700">{peso(shift.expectedCash)}</span></p>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Count the drawer</label>
          <div className="grid grid-cols-2 gap-2">
            {PH_DENOMS.map((d) => (
              <div key={d} className="flex items-center gap-2">
                <span className="w-14 text-right text-sm text-slate-500">{peso(d).replace('.00', '')}</span>
                <span className="text-slate-300">×</span>
                <input className={`${inputCls} w-20`} type="number" min={0} value={qtys[d] ?? ''}
                  onChange={(e) => setQtys((p) => ({ ...p, [d]: e.target.value }))} placeholder="0" />
                <span className="ml-auto text-sm text-slate-600">{peso(d * (Number(qtys[d]) || 0))}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between border-t border-slate-100 pt-2">
          <span className="text-sm font-medium text-slate-700">Counted cash</span>
          <span className="font-semibold text-slate-800">{peso(counted)}</span>
        </div>
        <p className={`text-sm ${variance < 0 ? 'text-red-600' : variance > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
          Variance: {peso(variance)}
        </p>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Note (optional)</label>
          <input className={inputCls} value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={submit}>{saving ? 'Closing…' : 'Close shift'}</button>
        </div>
      </div>
    </Modal>
  )
}
