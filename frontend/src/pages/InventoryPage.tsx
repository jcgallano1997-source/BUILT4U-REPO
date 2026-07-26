import { useCallback, useEffect, useState, type ChangeEvent, type ReactNode } from 'react'
import { Boxes, Package, Plus, SlidersHorizontal } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import {
  adjustStock, createItem, listCategories, listItems, listLocations, listUoms, updateItem,
  type Category, type Item, type ItemPayload, type Location, type StockLevel, type Uom,
} from '@/lib/inventory'

function apiErr(e: unknown, f: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? f
}

const levelBadge: Record<StockLevel, string> = {
  OK: 'bg-emerald-50 text-emerald-700',
  WARNING: 'bg-amber-50 text-amber-700',
  CRITICAL: 'bg-red-50 text-red-700',
}

export default function InventoryPage() {
  const [items, setItems] = useState<Item[]>([])
  const [cats, setCats] = useState<Category[]>([])
  const [locs, setLocs] = useState<Location[]>([])
  const [uoms, setUoms] = useState<Uom[]>([])
  const [search, setSearch] = useState('')
  const [catId, setCatId] = useState<number | ''>('')
  const [level, setLevel] = useState<StockLevel | ''>('')
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Item | 'new' | null>(null)
  const [adjusting, setAdjusting] = useState<Item | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await listItems({
        search: search.trim() || undefined,
        catId: catId === '' ? undefined : catId,
        stockLevel: level || undefined,
      }))
    } catch (e) { toast.error(apiErr(e, 'Failed to load items')) }
    finally { setLoading(false) }
  }, [search, catId, level])

  useEffect(() => {
    Promise.all([listCategories(), listLocations(), listUoms()])
      .then(([c, l, u]) => { setCats(c); setLocs(l); setUoms(u) })
      .catch(() => {})
  }, [])
  useEffect(() => { reload() }, [reload])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Package size={18} className="text-indigo-600" /> Inventory
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New item</button>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <input className={`${inputCls} max-w-xs`} placeholder="Search code / name…" value={search}
          onChange={(e) => setSearch(e.target.value)} />
        <select className={`${inputCls} max-w-[12rem]`} value={catId} onChange={(e) => setCatId(e.target.value === '' ? '' : Number(e.target.value))}>
          <option value="">All categories</option>
          {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select className={`${inputCls} max-w-[11rem]`} value={level} onChange={(e) => setLevel(e.target.value as StockLevel | '')}>
          <option value="">All stock levels</option>
          <option value="OK">OK</option>
          <option value="WARNING">Warning</option>
          <option value="CRITICAL">Critical</option>
        </select>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Code</th>
              <th className="px-4 py-2 font-medium">Name</th>
              <th className="px-4 py-2 font-medium">Category</th>
              <th className="px-4 py-2 font-medium text-right">Qty</th>
              <th className="px-4 py-2 font-medium text-right">Price</th>
              <th className="px-4 py-2 font-medium">Stock</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? <tr><td colSpan={7} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
              : items.length === 0 ? <tr><td colSpan={7} className="px-4 py-6 text-center text-slate-400">No items</td></tr>
              : items.map((it) => (
                <tr key={it.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">{it.code}</td>
                  <td className="px-4 py-2">{it.name}</td>
                  <td className="px-4 py-2 text-slate-500">{it.categoryName ?? '—'}</td>
                  <td className="px-4 py-2 text-right">{it.quantity} <span className="text-slate-400">{it.uom}</span></td>
                  <td className="px-4 py-2 text-right">₱{Number(it.sellingPrice).toFixed(2)}</td>
                  <td className="px-4 py-2"><span className={`rounded px-1.5 py-0.5 text-xs ${levelBadge[it.stockLevel]}`}>{it.stockLevel}</span></td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="inline-flex items-center gap-1 text-slate-600 hover:underline" onClick={() => setAdjusting(it)}>
                      <SlidersHorizontal size={13} /> Adjust
                    </button>
                    <button className="ml-3 text-indigo-600 hover:underline" onClick={() => setEditing(it)}>Edit</button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing && (
        <ItemForm
          item={editing === 'new' ? null : editing}
          cats={cats} locs={locs} uoms={uoms}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); reload() }}
        />
      )}
      {adjusting && <AdjustForm item={adjusting} onClose={() => setAdjusting(null)} onSaved={() => { setAdjusting(null); reload() }} />}
    </div>
  )
}

const num = (v: string): number | undefined => (v.trim() === '' ? undefined : Number(v))

function ItemForm({
  item, cats, locs, uoms, onClose, onSaved,
}: {
  item: Item | null
  cats: Category[]; locs: Location[]; uoms: Uom[]
  onClose: () => void; onSaved: () => void
}) {
  const isNew = item === null
  const [f, setF] = useState({
    code: item?.code ?? '',
    name: item?.name ?? '',
    description: item?.description ?? '',
    catId: item ? String(item.catId) : '',
    locId: item ? String(item.locId) : '',
    uom: item?.uom ?? '',
    quantity: item ? String(item.quantity) : '0',
    sellingPrice: item ? String(item.sellingPrice) : '',
    costPrice: item?.costPrice != null ? String(item.costPrice) : '',
    warning: item?.warning != null ? String(item.warning) : '',
    critical: item?.critical != null ? String(item.critical) : '',
    barcodeId: item?.barcodeId != null ? String(item.barcodeId) : '',
    active: item?.active ?? true,
  })
  const [saving, setSaving] = useState(false)
  const set = (k: keyof typeof f) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setF((p) => ({ ...p, [k]: e.target.type === 'checkbox' ? (e.target as HTMLInputElement).checked : e.target.value }))

  async function save() {
    if (!f.catId || !f.locId || !f.uom) { toast.error('Category, location and unit are required'); return }
    const payload: ItemPayload = {
      code: f.code.trim(), name: f.name.trim(), description: f.description.trim() || undefined,
      catId: Number(f.catId), locId: Number(f.locId), uom: f.uom,
      quantity: Number(f.quantity || 0), sellingPrice: Number(f.sellingPrice || 0),
      costPrice: num(f.costPrice), warning: num(f.warning), critical: num(f.critical), barcodeId: num(f.barcodeId),
      active: f.active,
    }
    setSaving(true)
    try {
      if (isNew) { await createItem(payload); toast.success('Item created') }
      else { await updateItem(item!.id, payload); toast.success('Item updated') }
      onSaved()
    } catch (e) { toast.error(apiErr(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <Modal title={isNew ? 'New item' : `Edit ${item!.code}`} onClose={onClose} width="max-w-2xl">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <L label="Code"><input className={inputCls} value={f.code} onChange={set('code')} /></L>
        <L label="Name"><input className={inputCls} value={f.name} onChange={set('name')} /></L>
        <L label="Category">
          <select className={inputCls} value={f.catId} onChange={set('catId')}>
            <option value="">Select…</option>
            {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </L>
        <L label="Location">
          <select className={inputCls} value={f.locId} onChange={set('locId')}>
            <option value="">Select…</option>
            {locs.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
          </select>
        </L>
        <L label="Unit">
          <select className={inputCls} value={f.uom} onChange={set('uom')}>
            <option value="">Select…</option>
            {uoms.map((u) => <option key={u.uom} value={u.uom}>{u.uom}</option>)}
          </select>
        </L>
        <L label="Barcode (optional)"><input className={inputCls} type="number" value={f.barcodeId} onChange={set('barcodeId')} /></L>
        <L label="Quantity"><input className={inputCls} type="number" value={f.quantity} onChange={set('quantity')} disabled={!isNew} /></L>
        <L label="Selling price"><input className={inputCls} type="number" value={f.sellingPrice} onChange={set('sellingPrice')} /></L>
        <L label="Cost price (optional)"><input className={inputCls} type="number" value={f.costPrice} onChange={set('costPrice')} /></L>
        <L label="Description"><input className={inputCls} value={f.description} onChange={set('description')} /></L>
        <L label="Warning level"><input className={inputCls} type="number" value={f.warning} onChange={set('warning')} /></L>
        <L label="Critical level"><input className={inputCls} type="number" value={f.critical} onChange={set('critical')} /></L>
        {!isNew && (
          <label className="flex items-center gap-2 self-end pb-2 text-sm text-slate-700">
            <input type="checkbox" checked={f.active} onChange={set('active')} /> Active
          </label>
        )}
      </div>
      {isNew && <p className="mt-2 text-xs text-slate-400">Change quantity after creation via <Boxes size={12} className="inline" /> Adjust.</p>}
      <div className="mt-4 flex justify-end gap-2">
        <button className={btnGhost} onClick={onClose}>Cancel</button>
        <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
      </div>
    </Modal>
  )
}

function AdjustForm({ item, onClose, onSaved }: { item: Item; onClose: () => void; onSaved: () => void }) {
  const [delta, setDelta] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  async function save() {
    const d = Number(delta)
    if (!d) { toast.error('Enter a non-zero amount (use - to remove stock)'); return }
    if (!reason.trim()) { toast.error('Reason is required'); return }
    setSaving(true)
    try { await adjustStock(item.id, { delta: d, reason: reason.trim() }); toast.success('Stock adjusted'); onSaved() }
    catch (e) { toast.error(apiErr(e, 'Adjust failed')) } finally { setSaving(false) }
  }
  return (
    <Modal title={`Adjust stock · ${item.code}`} onClose={onClose}>
      <div className="space-y-4">
        <p className="text-sm text-slate-500">Current quantity: <span className="font-medium text-slate-700">{item.quantity} {item.uom}</span></p>
        <L label="Change (+ to add, - to remove)"><input className={inputCls} type="number" value={delta} onChange={(e) => setDelta(e.target.value)} autoFocus /></L>
        <L label="Reason"><input className={inputCls} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Stocktake correction / damage / …" /></L>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Apply'}</button>
        </div>
      </div>
    </Modal>
  )
}

function L({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>
      {children}
    </div>
  )
}
