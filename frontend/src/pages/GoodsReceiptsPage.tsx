import { useEffect, useState } from 'react'
import { PackagePlus, Trash2, Truck } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import { listItems, updateSellingPrice, type Item } from '@/lib/inventory'
import { listSuppliers, type Supplier } from '@/lib/parties'
import {
  createGoodsReceipt, getGoodsReceipt, getPurchaseOrder, listGoodsReceipts, listPurchaseOrders,
  procErr, type GoodsReceipt, type Po, type PoSummary, type RepriceSuggestion,
} from '@/lib/procurement'

export default function GoodsReceiptsPage() {
  const [rows, setRows] = useState<GoodsReceipt[]>([])
  const [source, setSource] = useState<'PO' | 'DIRECT' | ''>('')
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<GoodsReceipt | null>(null)
  const [receiving, setReceiving] = useState(false)

  async function reload() {
    setLoading(true)
    try { setRows(await listGoodsReceipts({ source })) }
    catch (e) { toast.error(procErr(e, 'Failed to load goods receipts')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [source])

  async function open(grNumber: string) {
    try { setDetail(await getGoodsReceipt(grNumber)) } catch (e) { toast.error(procErr(e, 'Failed to load receipt')) }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Truck size={18} className="text-blue-600" /> Goods receiving
        </h1>
        <div className="flex items-center gap-2">
          <select className={`${inputCls} max-w-[10rem]`} value={source} onChange={(e) => setSource(e.target.value as 'PO' | 'DIRECT' | '')}>
            <option value="">All receipts</option>
            <option value="PO">Against a PO</option>
            <option value="DIRECT">Direct</option>
          </select>
          <button className={btnPrimary} onClick={() => setReceiving(true)}><PackagePlus size={16} /> Receive stock</button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">GR #</th>
              <th className="px-4 py-2 font-medium">PO #</th>
              <th className="px-4 py-2 font-medium">Supplier</th>
              <th className="px-4 py-2 font-medium">When</th>
              <th className="px-4 py-2 font-medium text-right">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No goods receipts</td></tr>
              : rows.map((g) => (
                <tr key={g.grNumber} className="cursor-pointer hover:bg-slate-50" onClick={() => open(g.grNumber)}>
                  <td className="px-4 py-2 font-medium text-blue-700">{g.grNumber}</td>
                  <td className="px-4 py-2 text-slate-500">{g.poNumber ?? <span className="text-slate-400">direct</span>}</td>
                  <td className="px-4 py-2 text-slate-600">{g.supplier ?? '—'}</td>
                  <td className="px-4 py-2 text-slate-500">{new Date(g.creationDate).toLocaleString()}</td>
                  <td className="px-4 py-2 text-right">{peso(g.grandTotal)}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && (
        <Modal title={`Receipt ${detail.grNumber}`} onClose={() => setDetail(null)} width="max-w-xl">
          <div className="space-y-3">
            <div className="flex items-center justify-between text-sm text-slate-400">
              <span>{detail.poNumber ? `PO ${detail.poNumber}` : 'Direct receipt'}</span>
              <span>{detail.supplier ?? ''} · {new Date(detail.creationDate).toLocaleString()}</span>
            </div>
            {detail.reference && <div className="text-sm text-slate-500">Ref: <span className="text-slate-700">{detail.reference}</span></div>}
            <table className="w-full text-sm">
              <thead className="text-left text-slate-400">
                <tr><th className="py-1">Item</th><th className="py-1 text-right">Qty</th><th className="py-1 text-right">Unit cost</th><th className="py-1 text-right">Subtotal</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {detail.lines.map((l) => (
                  <tr key={l.itemId}>
                    <td className="py-1 text-slate-700">{l.itemName ?? `#${l.itemId}`} <span className="text-xs text-slate-400">{l.itemCode}</span></td>
                    <td className="py-1 text-right">{l.quantity} {l.uom}</td>
                    <td className="py-1 text-right">{peso(l.supPrice)}</td>
                    <td className="py-1 text-right">{peso(l.subTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-sm font-semibold text-slate-800">
              <span>Grand total</span><span>{peso(detail.grandTotal)}</span>
            </div>
          </div>
        </Modal>
      )}

      {receiving && <ReceiveForm onClose={() => setReceiving(false)} onDone={() => { setReceiving(false); reload() }} />}
    </div>
  )
}

interface DraftLine {
  itemId: number; label: string; uom: string | null; quantity: string; unitCost: string; max?: number
  purchaseUom?: string | null; packSize?: number | null; inPurchaseUnit?: boolean
}

function ReceiveForm({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [mode, setMode] = useState<'PO' | 'DIRECT'>('PO')
  const [openPos, setOpenPos] = useState<PoSummary[]>([])
  const [poNumber, setPoNumber] = useState('')
  const [po, setPo] = useState<Po | null>(null)
  const [supplier, setSupplier] = useState('')
  const [supplierResults, setSupplierResults] = useState<Supplier[]>([])
  const [showSupplierList, setShowSupplierList] = useState(false)
  const [reference, setReference] = useState('')
  const [remarks, setRemarks] = useState('')
  const [lines, setLines] = useState<DraftLine[]>([])
  const [search, setSearch] = useState('')
  const [results, setResults] = useState<Item[]>([])
  const [saving, setSaving] = useState(false)
  const [reprice, setReprice] = useState<RepriceSuggestion[] | null>(null)

  // Receivable POs (APPROVED / PARTIALLY_RECEIVED) for the picker.
  useEffect(() => {
    Promise.all([listPurchaseOrders('APPROVED'), listPurchaseOrders('PARTIALLY_RECEIVED')])
      .then(([a, b]) => setOpenPos([...a, ...b]))
      .catch(() => {})
  }, [])

  // Direct-mode supplier search (name-matched, like the item picker).
  useEffect(() => {
    if (mode !== 'DIRECT') return
    const q = supplier.trim()
    if (!q) { setSupplierResults([]); return }
    const t = setTimeout(() => {
      listSuppliers(q).then((r) => setSupplierResults(r.slice(0, 6))).catch(() => {})
    }, 200)
    return () => clearTimeout(t)
  }, [supplier, mode])

  // Direct-mode item search.
  useEffect(() => {
    if (mode !== 'DIRECT') return
    const t = setTimeout(() => {
      if (!search.trim()) { setResults([]); return }
      listItems({ search: search.trim() }).then((r) => setResults(r.slice(0, 8))).catch(() => {})
    }, 200)
    return () => clearTimeout(t)
  }, [search, mode])

  async function loadPo(num: string) {
    setPoNumber(num)
    if (!num) { setPo(null); setLines([]); return }
    try {
      const p = await getPurchaseOrder(num)
      setPo(p)
      setLines(p.lines.filter((l) => l.remainingQty > 0).map((l) => ({
        itemId: l.itemId, label: `${l.itemName ?? '#' + l.itemId}`, uom: l.uom,
        quantity: String(l.remainingQty), unitCost: String(l.unitPrice), max: l.remainingQty,
      })))
    } catch (e) { toast.error(procErr(e, 'Failed to load PO')) }
  }

  function addDirect(item: Item) {
    if (lines.some((l) => l.itemId === item.id)) { toast.error('Item already added'); return }
    setLines((p) => [...p, { itemId: item.id, label: item.name, uom: item.uom, quantity: '1',
      unitCost: String(item.costPrice ?? 0), purchaseUom: item.purchaseUom, packSize: item.packSize }])
    setSearch(''); setResults([])
  }
  const setLine = (id: number, k: 'quantity' | 'unitCost', v: string) =>
    setLines((p) => p.map((l) => (l.itemId === id ? { ...l, [k]: v } : l)))
  const toggleUnit = (id: number, inPurchaseUnit: boolean) =>
    setLines((p) => p.map((l) => (l.itemId === id ? { ...l, inPurchaseUnit } : l)))
  const total = lines.reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitCost) || 0), 0)

  async function save() {
    const active = lines.filter((l) => Number(l.quantity) > 0)
    if (active.length === 0) { toast.error('Enter a quantity to receive'); return }
    if (mode === 'PO' && !poNumber) { toast.error('Select a PO'); return }
    if (!reference.trim()) { toast.error('Reference (delivery / invoice #) is required'); return }
    if (active.some((l) => l.unitCost.trim() === '' || !(Number(l.unitCost) > 0))) {
      toast.error('Enter a unit cost greater than 0 for every item'); return
    }
    setSaving(true)
    try {
      // Direct receipts must name a REGISTERED supplier (create it in Suppliers first).
      if (mode === 'DIRECT') {
        const name = supplier.trim()
        if (!name) { toast.error('Supplier is required'); return }
        const matches = await listSuppliers(name)
        const exact = matches.find((s) => s.name.trim().toLowerCase() === name.toLowerCase())
        if (!exact) { toast.error(`Supplier "${name}" isn't registered — add it in Suppliers first`); return }
      }
      const payloadLines = active.map((l) => ({ itemId: l.itemId, quantity: Number(l.quantity), unitCost: Number(l.unitCost),
        ...(l.inPurchaseUnit ? { inPurchaseUnit: true } : {}) }))
      const body = mode === 'PO'
        ? { poNumber, reference: reference.trim(), remarks: remarks.trim() || undefined, lines: payloadLines }
        : { supplier: supplier.trim(), reference: reference.trim(), remarks: remarks.trim() || undefined, lines: payloadLines }
      const gr = await createGoodsReceipt(body)
      toast.success(`Received ${gr.grNumber}`)
      // If the average cost rose, offer to re-price affected items before closing.
      if (gr.repriceSuggestions && gr.repriceSuggestions.length > 0) setReprice(gr.repriceSuggestions)
      else onDone()
    } catch (e) { toast.error(procErr(e, 'Receiving failed')) } finally { setSaving(false) }
  }

  if (reprice) return <RepriceModal items={reprice} onDone={onDone} />

  return (
    <Modal title="Receive stock" onClose={onClose} width="max-w-2xl">
      <div className="space-y-3">
        <div className="flex gap-2">
          <button className={`flex-1 rounded-md border px-3 py-1.5 text-sm ${mode === 'PO' ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-slate-300 text-slate-500'}`}
            onClick={() => { setMode('PO'); setLines([]) }}>Against a PO</button>
          <button className={`flex-1 rounded-md border px-3 py-1.5 text-sm ${mode === 'DIRECT' ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-slate-300 text-slate-500'}`}
            onClick={() => { setMode('DIRECT'); setPo(null); setPoNumber(''); setLines([]) }}>Direct (no PO)</button>
        </div>

        {mode === 'PO' ? (
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Purchase order</label>
            <select className={inputCls} value={poNumber} onChange={(e) => loadPo(e.target.value)}>
              <option value="">Select a receivable PO…</option>
              {openPos.map((p) => <option key={p.poNumber} value={p.poNumber}>{p.poNumber} · {p.supplier}</option>)}
            </select>
            {po && <p className="mt-1 text-xs text-slate-400">Supplier: {po.supplier}</p>}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            <div className="relative">
              <label className="mb-1 block text-sm font-medium text-slate-700">Supplier <span className="text-red-500">*</span></label>
              <input className={inputCls} placeholder="Search supplier…" value={supplier}
                onChange={(e) => { setSupplier(e.target.value); setShowSupplierList(true) }}
                onFocus={() => setShowSupplierList(true)}
                onBlur={() => setTimeout(() => setShowSupplierList(false), 150)} />
              {showSupplierList && supplierResults.length > 0 && (
                <div className="absolute z-10 mt-1 w-full rounded-md border border-slate-200 bg-white shadow">
                  {supplierResults.map((s) => (
                    <button key={s.id} type="button" className="block w-full px-3 py-1.5 text-left text-sm hover:bg-slate-50"
                      onClick={() => { setSupplier(s.name); setShowSupplierList(false); setSupplierResults([]) }}>
                      {s.name} <span className="text-slate-400">{s.code}</span>
                    </button>
                  ))}
                </div>
              )}
              <p className="mt-1 text-xs text-slate-400">Must be a registered supplier — add new ones in Suppliers.</p>
            </div>
            <div className="relative">
              <label className="mb-1 block text-sm font-medium text-slate-700">Add item</label>
              <input className={inputCls} placeholder="Search item…" value={search} onChange={(e) => setSearch(e.target.value)} />
              {results.length > 0 && (
                <div className="absolute z-10 mt-1 w-full rounded-md border border-slate-200 bg-white shadow">
                  {results.map((it) => (
                    <button key={it.id} className="block w-full px-3 py-1.5 text-left text-sm hover:bg-slate-50" onClick={() => addDirect(it)}>
                      {it.name} <span className="text-slate-400">{it.code}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <div><label className="mb-1 block text-sm font-medium text-slate-700">Reference <span className="text-red-500">*</span></label>
            <input className={inputCls} value={reference} onChange={(e) => setReference(e.target.value)} placeholder="Delivery / invoice #" /></div>
          <div><label className="mb-1 block text-sm font-medium text-slate-700">Remarks</label>
            <input className={inputCls} value={remarks} onChange={(e) => setRemarks(e.target.value)} /></div>
        </div>

        {lines.length > 0 && (
          <table className="w-full text-sm">
            <thead className="text-left text-slate-400">
              <tr><th className="py-1">Item</th><th className="py-1 text-right">Qty</th><th className="py-1 text-right">Unit cost <span className="text-red-500">*</span></th><th className="py-1 text-right">Subtotal</th>{mode === 'DIRECT' && <th></th>}</tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {lines.map((l) => (
                <tr key={l.itemId}>
                  <td className="py-1 text-slate-700">
                    {l.label}{l.max != null && <span className="ml-1 text-xs text-amber-600">(≤{l.max})</span>}
                    {mode === 'DIRECT' && l.packSize != null && l.packSize > 0 && l.purchaseUom && (
                      <div className="mt-0.5 flex items-center gap-1 text-xs text-slate-500">
                        <span>Receive in</span>
                        <select className="rounded border border-slate-300 px-1 py-0.5 text-xs"
                          value={l.inPurchaseUnit ? 'P' : 'B'} onChange={(e) => toggleUnit(l.itemId, e.target.value === 'P')}>
                          <option value="B">{l.uom}</option>
                          <option value="P">{l.purchaseUom} (×{l.packSize})</option>
                        </select>
                        {l.inPurchaseUnit && <span className="text-slate-400">= {(Number(l.quantity) || 0) * l.packSize} {l.uom}</span>}
                      </div>
                    )}
                  </td>
                  <td className="py-1 text-right"><input className="w-16 rounded border border-slate-300 px-1 py-0.5 text-right" type="number" min={0} max={l.max} value={l.quantity} onChange={(e) => setLine(l.itemId, 'quantity', e.target.value)} /></td>
                  <td className="py-1 text-right"><input className="w-20 rounded border border-slate-300 px-1 py-0.5 text-right" type="number" min={0} value={l.unitCost} onChange={(e) => setLine(l.itemId, 'unitCost', e.target.value)} /></td>
                  <td className="py-1 text-right">{peso((Number(l.quantity) || 0) * (Number(l.unitCost) || 0))}</td>
                  {mode === 'DIRECT' && <td className="py-1 text-right"><button className="text-red-500" onClick={() => setLines((p) => p.filter((x) => x.itemId !== l.itemId))}><Trash2 size={13} /></button></td>}
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
          <button className={btnPrimary} disabled={saving || lines.length === 0} onClick={save}>{saving ? 'Receiving…' : 'Receive'}</button>
        </div>
      </div>
    </Modal>
  )
}

/** After a receipt raised the average cost, offer a markup-preserving new selling price per item. */
function RepriceModal({ items, onDone }: { items: RepriceSuggestion[]; onDone: () => void }) {
  const [rows, setRows] = useState(items.map((s) => ({ ...s, accept: true, price: String(s.suggestedPrice) })))
  const [saving, setSaving] = useState(false)
  const setRow = (id: number, k: 'accept' | 'price', v: boolean | string) =>
    setRows((p) => p.map((r) => (r.itemId === id ? { ...r, [k]: v } : r)))

  async function apply() {
    setSaving(true)
    try {
      const toApply = rows.filter((r) => r.accept && Number(r.price) > 0 && Number(r.price) !== r.sellingPrice)
      for (const r of toApply) await updateSellingPrice(r.itemId, Number(r.price))
      if (toApply.length) toast.success(`Updated ${toApply.length} price${toApply.length === 1 ? '' : 's'}`)
      onDone()
    } catch (e) { toast.error(procErr(e, 'Could not update prices')); setSaving(false) }
  }

  return (
    <Modal title="Update selling prices?" onClose={onDone} width="max-w-2xl">
      <div className="space-y-3">
        <p className="text-sm text-slate-500">
          These items' average cost went up on this receipt. Accept the suggested price (keeps your current markup) or edit it — untick to leave a price unchanged.
        </p>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="text-left text-slate-400">
              <tr>
                <th className="py-1">Item</th>
                <th className="py-1 text-right">Cost</th>
                <th className="py-1 text-right">Current price</th>
                <th className="py-1 text-right">New price</th>
                <th className="py-1 text-center">Apply</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r) => (
                <tr key={r.itemId}>
                  <td className="py-1.5 text-slate-700">{r.name} <span className="text-xs text-slate-400">{r.code}</span></td>
                  <td className="py-1.5 text-right text-slate-500">{peso(r.oldCost)} → <span className="text-slate-700">{peso(r.newCost)}</span></td>
                  <td className="py-1.5 text-right text-slate-500">{peso(r.sellingPrice)}</td>
                  <td className="py-1.5 text-right">
                    <input className="w-24 rounded border border-slate-300 px-1 py-0.5 text-right" type="number" min={0}
                      value={r.price} onChange={(e) => setRow(r.itemId, 'price', e.target.value)} />
                  </td>
                  <td className="py-1.5 text-center">
                    <input type="checkbox" checked={r.accept} onChange={(e) => setRow(r.itemId, 'accept', e.target.checked)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} disabled={saving} onClick={onDone}>Skip</button>
          <button className={btnPrimary} disabled={saving} onClick={apply}>{saving ? 'Updating…' : 'Update prices'}</button>
        </div>
      </div>
    </Modal>
  )
}
