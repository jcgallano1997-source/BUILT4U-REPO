import { useEffect, useState } from 'react'
import { Ban, Printer, Receipt, RotateCcw } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import {
  getSale, listSales, peso, posErr, refundSale, voidSale,
  type Sale, type SaleStatus, type SaleSummary,
} from '@/lib/pos'
import { docErr, openReceipt, printSaleReceipt } from '@/lib/docsettings'

const statusColor: Record<SaleStatus, string> = {
  COMPLETED: 'text-emerald-600',
  VOIDED: 'text-slate-400 line-through',
  REFUNDED: 'text-amber-600',
}

export default function SalesPage() {
  const [rows, setRows] = useState<SaleSummary[]>([])
  const [status, setStatus] = useState<SaleStatus | ''>('')
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<Sale | null>(null)
  const [refunding, setRefunding] = useState<Sale | null>(null)

  async function reload() {
    setLoading(true)
    try { setRows(await listSales(status)) }
    catch (e) { toast.error(posErr(e, 'Failed to load sales')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status])

  async function open(num: string) {
    try { setDetail(await getSale(num)) } catch (e) { toast.error(posErr(e, 'Failed to load sale')) }
  }
  async function doVoid(num: string) {
    if (!confirm(`Void sale ${num}? This restores stock and cannot be undone.`)) return
    try { await voidSale(num); toast.success('Sale voided'); setDetail(null); reload() }
    catch (e) { toast.error(posErr(e, 'Void failed')) }
  }
  async function printReceipt(num: string) {
    try { await openReceipt(num) } catch (e) { toast.error(docErr(e, 'Could not open receipt')) }
  }
  async function thermalPrint(num: string) {
    try { await printSaleReceipt(num); toast.success('Sent to printer') }
    catch (e) { toast.error(docErr(e, 'Could not print')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Receipt size={18} className="text-blue-600" /> Sales
        </h1>
        <select className={`${inputCls} max-w-[12rem]`} value={status} onChange={(e) => setStatus(e.target.value as SaleStatus | '')}>
          <option value="">All statuses</option>
          <option value="COMPLETED">Completed</option>
          <option value="REFUNDED">Refunded</option>
          <option value="VOIDED">Voided</option>
        </select>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Sale #</th>
              <th className="px-4 py-2 font-medium">When</th>
              <th className="px-4 py-2 font-medium">Cashier</th>
              <th className="px-4 py-2 font-medium">Mode</th>
              <th className="px-4 py-2 font-medium text-right">Total</th>
              <th className="px-4 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No sales</td></tr>
              : rows.map((s) => (
                <tr key={s.salesNumber} className="cursor-pointer hover:bg-slate-50" onClick={() => open(s.salesNumber)}>
                  <td className="px-4 py-2 font-medium text-blue-700">{s.salesNumber}</td>
                  <td className="px-4 py-2 text-slate-500">{new Date(s.creationDate).toLocaleString()}</td>
                  <td className="px-4 py-2 text-slate-500">{s.createdBy}</td>
                  <td className="px-4 py-2 text-slate-500">{s.modeOfPayment}</td>
                  <td className="px-4 py-2 text-right">{peso(s.grandTotal)}</td>
                  <td className={`px-4 py-2 ${statusColor[s.status]}`}>{s.status}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && (
        <Modal title={`Sale ${detail.salesNumber}`} onClose={() => setDetail(null)} width="max-w-xl">
          <div className="space-y-3">
            <div className="flex items-center justify-between text-sm">
              <span className={statusColor[detail.status]}>{detail.status}</span>
              <span className="text-slate-400">{new Date(detail.creationDate).toLocaleString()} · {detail.modeOfPayment}</span>
            </div>
            {detail.customerName && <div className="text-sm text-slate-500">Customer: <span className="text-slate-700">{detail.customerName}</span></div>}
            <table className="w-full text-sm">
              <thead className="text-left text-slate-400">
                <tr><th className="py-1">Item</th><th className="py-1 text-right">Qty</th><th className="py-1 text-right">Price</th><th className="py-1 text-right">Subtotal</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {detail.lines.map((l) => (
                  <tr key={l.itemId}>
                    <td className="py-1 text-slate-700">{l.itemName}
                      {l.refundedQuantity > 0 && <span className="ml-1 text-xs text-amber-600">(-{l.refundedQuantity} refunded)</span>}
                    </td>
                    <td className="py-1 text-right">{l.quantity} {l.uom}</td>
                    <td className="py-1 text-right">{peso(l.unitCost)}</td>
                    <td className="py-1 text-right">{peso(l.subTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="space-y-1 border-t border-slate-100 pt-2 text-sm">
              <Row label="Total" value={peso(detail.total)} />
              {detail.totalDiscItem > 0 && <Row label="Line discounts" value={`- ${peso(detail.totalDiscItem)}`} />}
              {detail.discountAll > 0 && <Row label="Discount" value={`- ${peso(detail.discountAll)}`} />}
              <Row label="Grand total" value={peso(detail.grandTotal)} bold />
              <Row label="Paid" value={peso(detail.payment)} />
              <Row label="Change" value={peso(detail.change)} />
            </div>
            <div className="flex items-center justify-between pt-2">
              <div className="flex gap-2">
                <button className={btnGhost} onClick={() => printReceipt(detail.salesNumber)}><Receipt size={14} /> PDF</button>
                <button className={btnGhost} onClick={() => thermalPrint(detail.salesNumber)}><Printer size={14} /> Print</button>
              </div>
              {detail.status === 'COMPLETED' && (
                <div className="flex gap-2">
                  <button className={`${btnGhost} text-red-600`} onClick={() => doVoid(detail.salesNumber)}><Ban size={14} /> Void</button>
                  <button className={btnPrimary} onClick={() => { setRefunding(detail); setDetail(null) }}><RotateCcw size={14} /> Refund</button>
                </div>
              )}
            </div>
          </div>
        </Modal>
      )}

      {refunding && <RefundForm sale={refunding} onClose={() => setRefunding(null)} onDone={() => { setRefunding(null); reload() }} />}
    </div>
  )
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <div className={`flex justify-between ${bold ? 'font-semibold text-slate-800' : 'text-slate-500'}`}>
      <span>{label}</span><span>{value}</span>
    </div>
  )
}

function RefundForm({ sale, onClose, onDone }: { sale: Sale; onClose: () => void; onDone: () => void }) {
  const refundable = sale.lines.filter((l) => l.refundableQuantity > 0)
  const [qty, setQty] = useState<Record<number, string>>({})
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit() {
    const lines = refundable
      .map((l) => ({ itemId: l.itemId, quantity: Number(qty[l.itemId] || 0) }))
      .filter((l) => l.quantity > 0)
    if (lines.length === 0) { toast.error('Enter a quantity to refund'); return }
    setSaving(true)
    try {
      const r = await refundSale(sale.salesNumber, { reason: reason.trim() || undefined, lines })
      toast.success(`Refunded ${peso(r.totalRefunded)} (${r.returnNumber})`); onDone()
    } catch (e) { toast.error(posErr(e, 'Refund failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={`Refund ${sale.salesNumber}`} onClose={onClose}>
      <div className="space-y-4">
        {refundable.length === 0 ? <p className="text-sm text-slate-500">Nothing left to refund on this sale.</p> : (
          <div className="space-y-2">
            {refundable.map((l) => (
              <div key={l.itemId} className="flex items-center gap-2 text-sm">
                <div className="min-w-0 flex-1 truncate text-slate-700">{l.itemName}</div>
                <span className="text-xs text-slate-400">up to {l.refundableQuantity}</span>
                <input className="w-20 rounded border border-slate-300 px-2 py-1 text-right" type="number" min={0}
                  max={l.refundableQuantity} value={qty[l.itemId] ?? ''} onChange={(e) => setQty((p) => ({ ...p, [l.itemId]: e.target.value }))} />
              </div>
            ))}
          </div>
        )}
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Reason</label>
          <input className={inputCls} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Damaged / wrong item / …" />
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving || refundable.length === 0} onClick={submit}>{saving ? 'Refunding…' : 'Refund'}</button>
        </div>
      </div>
    </Modal>
  )
}
