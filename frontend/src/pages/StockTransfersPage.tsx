import { useEffect, useState } from 'react'
import { ArrowLeftRight, Ban, PackageCheck, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { peso } from '@/lib/pos'
import { useAuthStore } from '@/store/authStore'
import { listItems, type Item } from '@/lib/inventory'
import {
  cancelTransfer, getTransfer, listDestinations, listTransfers, receiveTransfer, shipTransfer,
  xferErr, type CreateTransfer, type SiteOption, type TransferDetail, type TransferStatus, type TransferSummary,
} from '@/lib/transfers'

const STATUS_LABEL: Record<TransferStatus, string> = {
  IN_TRANSIT: 'In transit', RECEIVED: 'Received', CANCELLED: 'Cancelled',
}
const statusColor: Record<TransferStatus, string> = {
  IN_TRANSIT: 'text-amber-600', RECEIVED: 'text-emerald-600', CANCELLED: 'text-slate-400 line-through',
}

export default function StockTransfersPage() {
  const currentSiteId = useAuthStore((s) => s.site?.id)
  const [rows, setRows] = useState<TransferSummary[]>([])
  const [status, setStatus] = useState<TransferStatus | ''>('')
  const [direction, setDirection] = useState<'OUTBOUND' | 'INBOUND' | ''>('')
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<TransferDetail | null>(null)
  const [shipping, setShipping] = useState(false)

  async function reload() {
    setLoading(true)
    try { setRows((await listTransfers({ status, direction })).content) }
    catch (e) { toast.error(xferErr(e, 'Failed to load transfers')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, direction])

  async function open(n: string) {
    try { setDetail(await getTransfer(n)) } catch (e) { toast.error(xferErr(e, 'Failed to load transfer')) }
  }
  async function doReceive(n: string) {
    try { await receiveTransfer(n); toast.success('Received — stock added here'); setDetail(null); reload() }
    catch (e) { toast.error(xferErr(e, 'Receive failed')) }
  }
  async function doCancel(n: string) {
    if (!confirm(`Cancel transfer ${n}? Source stock is restored.`)) return
    try { await cancelTransfer(n); toast.success('Cancelled — stock restored'); setDetail(null); reload() }
    catch (e) { toast.error(xferErr(e, 'Cancel failed')) }
  }

  const h = detail?.header
  const isDest = !!h && h.destSiteId === currentSiteId
  const isSource = !!h && h.sourceSiteId === currentSiteId

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <ArrowLeftRight size={18} className="text-indigo-600" /> Stock transfers
        </h1>
        <div className="flex items-center gap-2">
          <select className={`${inputCls} max-w-[9rem]`} value={direction} onChange={(e) => setDirection(e.target.value as 'OUTBOUND' | 'INBOUND' | '')}>
            <option value="">All directions</option>
            <option value="OUTBOUND">Outbound</option>
            <option value="INBOUND">Inbound</option>
          </select>
          <select className={`${inputCls} max-w-[9rem]`} value={status} onChange={(e) => setStatus(e.target.value as TransferStatus | '')}>
            <option value="">All statuses</option>
            {(Object.keys(STATUS_LABEL) as TransferStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
          </select>
          <button className={btnPrimary} onClick={() => setShipping(true)}><Plus size={16} /> New transfer</button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Transfer #</th>
              <th className="px-4 py-2 font-medium">From → To</th>
              <th className="px-4 py-2 font-medium">Shipped</th>
              <th className="px-4 py-2 font-medium text-right">Lines</th>
              <th className="px-4 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : rows.length === 0 ? <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No transfers</td></tr>
              : rows.map((t) => (
                <tr key={t.id} className="cursor-pointer hover:bg-slate-50" onClick={() => open(t.transferNumber)}>
                  <td className="px-4 py-2 font-medium text-indigo-700">{t.transferNumber}</td>
                  <td className="px-4 py-2 text-slate-600">
                    {t.sourceSiteName} <span className="text-slate-400">→</span> {t.destSiteName}
                    {t.destSiteId === currentSiteId && <span className="ml-1 text-xs text-indigo-500">(inbound)</span>}
                  </td>
                  <td className="px-4 py-2 text-slate-500">{new Date(t.shippedAt).toLocaleString()}</td>
                  <td className="px-4 py-2 text-right text-slate-500">{t.lineCount}</td>
                  <td className={`px-4 py-2 ${statusColor[t.status]}`}>{STATUS_LABEL[t.status]}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {detail && h && (
        <Modal title={`Transfer ${h.transferNumber}`} onClose={() => setDetail(null)} width="max-w-xl">
          <div className="space-y-3">
            <div className="flex items-center justify-between text-sm">
              <span className={statusColor[h.status]}>{STATUS_LABEL[h.status]}</span>
              <span className="text-slate-400">{h.sourceSiteName} → {h.destSiteName}</span>
            </div>
            <div className="text-xs text-slate-500">
              Shipped {new Date(h.shippedAt).toLocaleString()} by {h.sentBy}
              {h.receivedAt && ` · received ${new Date(h.receivedAt).toLocaleString()} by ${h.receivedBy}`}
              {h.cancelledAt && ` · cancelled ${new Date(h.cancelledAt).toLocaleString()} by ${h.cancelledBy}`}
            </div>
            {h.remarks && <div className="text-sm text-slate-500">Remarks: <span className="text-slate-700">{h.remarks}</span></div>}
            <table className="w-full text-sm">
              <thead className="text-left text-slate-400">
                <tr><th className="py-1">Item</th><th className="py-1 text-right">Qty</th><th className="py-1 text-right">Unit cost</th><th className="py-1 text-right">Total</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {detail.items.map((l) => (
                  <tr key={l.id}>
                    <td className="py-1 text-slate-700">{l.itemName ?? l.itemCode} <span className="text-xs text-slate-400">{l.itemCode}</span></td>
                    <td className="py-1 text-right">{l.quantity} {l.uom}</td>
                    <td className="py-1 text-right">{peso(l.unitCost)}</td>
                    <td className="py-1 text-right">{peso(l.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {h.status === 'IN_TRANSIT' && (
              <div className="flex justify-end gap-2 pt-2">
                {isSource && <button className={`${btnGhost} text-red-600`} onClick={() => doCancel(h.transferNumber)}><Ban size={14} /> Cancel (restore stock)</button>}
                {isDest && <button className={btnPrimary} onClick={() => doReceive(h.transferNumber)}><PackageCheck size={14} /> Receive here</button>}
                {!isDest && !isSource && <span className="text-xs text-slate-400">Log in to the source or destination site to act on this transfer.</span>}
              </div>
            )}
          </div>
        </Modal>
      )}

      {shipping && <ShipForm onClose={() => setShipping(false)} onDone={() => { setShipping(false); reload() }} />}
    </div>
  )
}

interface DraftLine { item: Item; quantity: string }

function ShipForm({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [dests, setDests] = useState<SiteOption[]>([])
  const [destSiteId, setDestSiteId] = useState('')
  const [remarks, setRemarks] = useState('')
  const [search, setSearch] = useState('')
  const [results, setResults] = useState<Item[]>([])
  const [lines, setLines] = useState<DraftLine[]>([])
  const [saving, setSaving] = useState(false)

  useEffect(() => { listDestinations().then(setDests).catch(() => {}) }, [])
  useEffect(() => {
    const t = setTimeout(() => {
      if (!search.trim()) { setResults([]); return }
      listItems({ search: search.trim() }).then((r) => setResults(r.slice(0, 8))).catch(() => {})
    }, 200)
    return () => clearTimeout(t)
  }, [search])

  function addLine(item: Item) {
    if (lines.some((l) => l.item.id === item.id)) { toast.error('Item already added'); return }
    setLines((p) => [...p, { item, quantity: '1' }]); setSearch(''); setResults([])
  }
  const setQty = (id: number, v: string) => setLines((p) => p.map((l) => (l.item.id === id ? { ...l, quantity: v } : l)))

  async function save() {
    if (!destSiteId) { toast.error('Choose a destination site'); return }
    const payloadLines = lines
      .map((l) => ({ itemId: l.item.id, quantity: Number(l.quantity) }))
      .filter((l) => l.quantity > 0)
    if (payloadLines.length === 0) { toast.error('Add at least one line'); return }
    setSaving(true)
    try {
      const body: CreateTransfer = { destSiteId: Number(destSiteId), remarks: remarks.trim() || undefined, lines: payloadLines }
      const t = await shipTransfer(body)
      toast.success(`Shipped ${t.header.transferNumber}`); onDone()
    } catch (e) { toast.error(xferErr(e, 'Ship failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title="New stock transfer" onClose={onClose} width="max-w-2xl">
      <div className="space-y-3">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Destination site</label>
          <select className={inputCls} value={destSiteId} onChange={(e) => setDestSiteId(e.target.value)}>
            <option value="">Select a destination…</option>
            {dests.map((d) => <option key={d.id} value={d.id}>{d.code} · {d.name}</option>)}
          </select>
          {dests.length === 0 && <p className="mt-1 text-xs text-amber-600">No eligible destinations (need another active site, and the transfer policy must permit it).</p>}
        </div>
        <div><label className="mb-1 block text-sm font-medium text-slate-700">Remarks</label>
          <input className={inputCls} value={remarks} onChange={(e) => setRemarks(e.target.value)} /></div>

        <div className="relative">
          <label className="mb-1 block text-sm font-medium text-slate-700">Add items (shipped from this site)</label>
          <input className={inputCls} placeholder="Search item by code or name…" value={search} onChange={(e) => setSearch(e.target.value)} />
          {results.length > 0 && (
            <div className="absolute z-10 mt-1 w-full rounded-md border border-slate-200 bg-white shadow">
              {results.map((it) => (
                <button key={it.id} className="block w-full px-3 py-1.5 text-left text-sm hover:bg-slate-50" onClick={() => addLine(it)}>
                  {it.name} <span className="text-slate-400">{it.code} · {it.quantity} on hand</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {lines.length > 0 && (
          <table className="w-full text-sm">
            <thead className="text-left text-slate-400"><tr><th className="py-1">Item</th><th className="py-1 text-right">On hand</th><th className="py-1 text-right">Ship qty</th><th></th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {lines.map((l) => (
                <tr key={l.item.id}>
                  <td className="py-1 text-slate-700">{l.item.name}</td>
                  <td className="py-1 text-right text-slate-400">{l.item.quantity}</td>
                  <td className="py-1 text-right"><input className="w-20 rounded border border-slate-300 px-1 py-0.5 text-right" type="number" min={0} max={l.item.quantity} value={l.quantity} onChange={(e) => setQty(l.item.id, e.target.value)} /></td>
                  <td className="py-1 text-right"><button className="text-red-500" onClick={() => setLines((p) => p.filter((x) => x.item.id !== l.item.id))}><Trash2 size={13} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Shipping…' : 'Ship transfer'}</button>
        </div>
      </div>
    </Modal>
  )
}
