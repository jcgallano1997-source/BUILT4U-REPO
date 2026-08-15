import { useEffect, useState } from 'react'
import { CheckCircle2, ClipboardCheck, Plus, Ban, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import { listItems, type Item } from '@/lib/inventory'
import {
  approvePurchaseOrder, cancelPurchaseOrder, createPurchaseOrder, getPurchaseOrder,
  listPurchaseOrders, procErr, type Po, type PoStatus, type PoSummary,
} from '@/lib/procurement'

const STATUS_LABEL: Record<PoStatus, string> = {
  DRAFT: 'Draft', APPROVED: 'Approved', PARTIALLY_RECEIVED: 'Partially received',
  RECEIVED: 'Received', CANCELLED: 'Cancelled',
}
const statusColor: Record<PoStatus, string> = {
  DRAFT: 'text-slate-500', APPROVED: 'text-blue-600', PARTIALLY_RECEIVED: 'text-amber-600',
  RECEIVED: 'text-emerald-600', CANCELLED: 'text-slate-400 line-through',
}

export default function PurchaseOrdersPage() {
  const [rows, setRows] = useState<PoSummary[]>([])
  const [status, setStatus] = useState<PoStatus | ''>('')
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<Po | null>(null)
  const [creating, setCreating] = useState(false)

  async function reload() {
    setLoading(true)
    try { setRows(await listPurchaseOrders(status)) }
    catch (e) { toast.error(procErr(e, 'Failed to load purchase orders')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status])

  async function open(poNumber: string) {
    try { setDetail(await getPurchaseOrder(poNumber)) } catch (e) { toast.error(procErr(e, 'Failed to load PO')) }
  }
  async function doApprove(poNumber: string) {
    try { await approvePurchaseOrder(poNumber); toast.success('PO approved'); setDetail(null); reload() }
    catch (e) { toast.error(procErr(e, 'Approve failed')) }
  }
  async function doCancel(poNumber: string) {
    if (!confirm(`Cancel PO ${poNumber}?`)) return
    try { await cancelPurchaseOrder(poNumber); toast.success('PO cancelled'); setDetail(null); reload() }
    catch (e) { toast.error(procErr(e, 'Cancel failed')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <ClipboardCheck size={18} className="text-blue-600" /> Purchase orders
        </h1>
        <div className="flex items-center gap-2">
          <select className={`${inputCls} max-w-[12rem]`} value={status} onChange={(e) => setStatus(e.target.value as PoStatus | '')}>
            <option value="">All statuses</option>
            {(Object.keys(STATUS_LABEL) as PoStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
          </select>
          <button className={btnPrimary} onClick={() => setCreating(true)}><Plus size={16} /> New PO</button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">PO #</th>
              <th className="px-4 py-2 font-medium">Supplier</th>
              <th className="px-4 py-2 font-medium">When</th>
              <th className="px-4 py-2 font-medium text-right">Lines</th>
              <th className="px-4 py-2 font-medium text-right">Total</th>
              <th className="px-4 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">No purchase orders</td></tr>
              : rows.map((p) => (
                <tr key={p.poNumber} className="cursor-pointer hover:bg-slate-50" onClick={() => open(p.poNumber)}>
                  <td className="px-4 py-2 font-medium text-blue-700">{p.poNumber}</td>
                  <td className="px-4 py-2 text-slate-600">{p.supplier}</td>
                  <td className="px-4 py-2 text-slate-500">{new Date(p.creationDate).toLocaleString()}</td>
                  <td className="px-4 py-2 text-right text-slate-500">{p.lineCount}</td>
                  <td className="px-4 py-2 text-right">{peso(p.grandTotal)}</td>
                  <td className={`px-4 py-2 ${statusColor[p.status]}`}>{STATUS_LABEL[p.status]}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && (
        <Modal title={`PO ${detail.poNumber}`} onClose={() => setDetail(null)} width="max-w-2xl">
          <div className="space-y-3">
            <div className="flex items-center justify-between text-sm">
              <span className={statusColor[detail.status]}>{STATUS_LABEL[detail.status]}</span>
              <span className="text-slate-400">{detail.supplier}{detail.deliveryDate ? ` · deliver ${detail.deliveryDate}` : ''}</span>
            </div>
            {detail.approvedBy && (
              <div className="text-xs text-slate-500">
                Approved by {detail.approvedBy}{detail.autoApproved ? ' (auto)' : ''}
                {detail.approvedAt ? ` · ${new Date(detail.approvedAt).toLocaleString()}` : ''}
              </div>
            )}
            {detail.remarks && <div className="text-sm text-slate-500">Remarks: <span className="text-slate-700">{detail.remarks}</span></div>}
            <table className="w-full text-sm">
              <thead className="text-left text-slate-400">
                <tr>
                  <th className="py-1">Item</th><th className="py-1 text-right">Ordered</th>
                  <th className="py-1 text-right">Received</th><th className="py-1 text-right">Remaining</th>
                  <th className="py-1 text-right">Unit</th><th className="py-1 text-right">Subtotal</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {detail.lines.map((l) => (
                  <tr key={l.itemId}>
                    <td className="py-1 text-slate-700">{l.itemName ?? `#${l.itemId}`} <span className="text-xs text-slate-400">{l.itemCode}</span></td>
                    <td className="py-1 text-right">{l.orderedQty} {l.uom}</td>
                    <td className="py-1 text-right text-emerald-600">{l.receivedQty}</td>
                    <td className="py-1 text-right text-amber-600">{l.remainingQty}</td>
                    <td className="py-1 text-right">{peso(l.unitPrice)}</td>
                    <td className="py-1 text-right">{peso(l.subTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-sm font-semibold text-slate-800">
              <span>Grand total</span><span>{peso(detail.grandTotal)}</span>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              {(detail.status === 'DRAFT' || detail.status === 'APPROVED') && (
                <button className={`${btnGhost} text-red-600`} onClick={() => doCancel(detail.poNumber)}><Ban size={14} /> Cancel PO</button>
              )}
              {detail.status === 'DRAFT' && detail.canCurrentUserApprove && (
                <button className={btnPrimary} onClick={() => doApprove(detail.poNumber)}><CheckCircle2 size={14} /> Approve</button>
              )}
            </div>
          </div>
        </Modal>
      )}

      {creating && <PoForm onClose={() => setCreating(false)} onDone={() => { setCreating(false); reload() }} />}
    </div>
  )
}

interface DraftLine { item: Item; quantity: string; unitPrice: string }

function PoForm({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [supplier, setSupplier] = useState('')
  const [deliveryDate, setDeliveryDate] = useState('')
  const [remarks, setRemarks] = useState('')
  const [search, setSearch] = useState('')
  const [results, setResults] = useState<Item[]>([])
  const [lines, setLines] = useState<DraftLine[]>([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const t = setTimeout(() => {
      if (!search.trim()) { setResults([]); return }
      listItems({ search: search.trim() }).then((r) => setResults(r.slice(0, 8))).catch(() => {})
    }, 200)
    return () => clearTimeout(t)
  }, [search])

  function addLine(item: Item) {
    if (lines.some((l) => l.item.id === item.id)) { toast.error('Item already added'); return }
    setLines((p) => [...p, { item, quantity: '1', unitPrice: String(item.costPrice ?? 0) }])
    setSearch(''); setResults([])
  }
  const setLine = (id: number, k: 'quantity' | 'unitPrice', v: string) =>
    setLines((p) => p.map((l) => (l.item.id === id ? { ...l, [k]: v } : l)))
  const total = lines.reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0)

  async function save() {
    if (!supplier.trim()) { toast.error('Supplier is required'); return }
    const payloadLines = lines
      .map((l) => ({ itemId: l.item.id, quantity: Number(l.quantity), unitPrice: Number(l.unitPrice) }))
      .filter((l) => l.quantity > 0)
    if (payloadLines.length === 0) { toast.error('Add at least one line'); return }
    setSaving(true)
    try {
      const po = await createPurchaseOrder({
        supplier: supplier.trim(),
        deliveryDate: deliveryDate.trim() || undefined,
        remarks: remarks.trim() || undefined,
        lines: payloadLines,
      })
      toast.success(`PO ${po.poNumber} created (${STATUS_LABEL[po.status]})`); onDone()
    } catch (e) { toast.error(procErr(e, 'Create failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title="New purchase order" onClose={onClose} width="max-w-2xl">
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <div><label className="mb-1 block text-sm font-medium text-slate-700">Supplier</label>
            <input className={inputCls} value={supplier} onChange={(e) => setSupplier(e.target.value)} autoFocus /></div>
          <div><label className="mb-1 block text-sm font-medium text-slate-700">Delivery date</label>
            <input className={inputCls} type="date" value={deliveryDate} onChange={(e) => setDeliveryDate(e.target.value)} /></div>
        </div>
        <div><label className="mb-1 block text-sm font-medium text-slate-700">Remarks</label>
          <input className={inputCls} value={remarks} onChange={(e) => setRemarks(e.target.value)} /></div>

        <div className="relative">
          <label className="mb-1 block text-sm font-medium text-slate-700">Add items</label>
          <input className={inputCls} placeholder="Search item by code or name…" value={search} onChange={(e) => setSearch(e.target.value)} />
          {results.length > 0 && (
            <div className="absolute z-10 mt-1 w-full rounded-md border border-slate-200 bg-white shadow">
              {results.map((it) => (
                <button key={it.id} className="block w-full px-3 py-1.5 text-left text-sm hover:bg-slate-50" onClick={() => addLine(it)}>
                  {it.name} <span className="text-slate-400">{it.code}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {lines.length > 0 && (
          <table className="w-full text-sm">
            <thead className="text-left text-slate-400">
              <tr><th className="py-1">Item</th><th className="py-1 text-right">Qty</th><th className="py-1 text-right">Unit price</th><th className="py-1 text-right">Subtotal</th><th></th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {lines.map((l) => (
                <tr key={l.item.id}>
                  <td className="py-1 text-slate-700">{l.item.name}</td>
                  <td className="py-1 text-right"><input className="w-16 rounded border border-slate-300 px-1 py-0.5 text-right" type="number" min={0} value={l.quantity} onChange={(e) => setLine(l.item.id, 'quantity', e.target.value)} /></td>
                  <td className="py-1 text-right"><input className="w-20 rounded border border-slate-300 px-1 py-0.5 text-right" type="number" min={0} value={l.unitPrice} onChange={(e) => setLine(l.item.id, 'unitPrice', e.target.value)} /></td>
                  <td className="py-1 text-right">{peso((Number(l.quantity) || 0) * (Number(l.unitPrice) || 0))}</td>
                  <td className="py-1 text-right"><button className="text-red-500" onClick={() => setLines((p) => p.filter((x) => x.item.id !== l.item.id))}><Trash2 size={13} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex justify-between border-t border-slate-100 pt-2 text-sm font-semibold text-slate-800">
          <span>Total</span><span>{peso(total)}</span>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Create PO'}</button>
        </div>
      </div>
    </Modal>
  )
}
