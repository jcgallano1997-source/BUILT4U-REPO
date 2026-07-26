import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Minus, Plus, ScanLine, ShoppingCart, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { X } from 'lucide-react'
import { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { listItems, type Item } from '@/lib/inventory'
import { checkout, getCurrentShift, PAYMENT_MODES, peso, posErr, type Shift } from '@/lib/pos'
import { listActivePaymentModes, listCustomers, type Customer } from '@/lib/parties'

interface CartLine { item: Item; qty: number }

export default function PosPage() {
  const [shift, setShift] = useState<Shift | null | 'loading'>('loading')
  const [search, setSearch] = useState('')
  const [results, setResults] = useState<Item[]>([])
  const [cart, setCart] = useState<CartLine[]>([])
  const [mode, setMode] = useState<string>('CASH')
  const [paid, setPaid] = useState('')
  const [placing, setPlacing] = useState(false)
  const [lastSale, setLastSale] = useState<{ num: string; change: number } | null>(null)
  const [modes, setModes] = useState<{ code: string; label: string; accountsReceivable: boolean; customerRequired: boolean }[]>(
    PAYMENT_MODES.map((c) => ({ code: c, label: c, accountsReceivable: false, customerRequired: false })))
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [custQuery, setCustQuery] = useState('')
  const [custResults, setCustResults] = useState<Customer[]>([])

  useEffect(() => { getCurrentShift().then(setShift).catch(() => setShift(null)) }, [])
  useEffect(() => {
    listActivePaymentModes()
      .then((ms) => { if (ms.length) { setModes(ms.map((m) => ({ code: m.code, label: m.label, accountsReceivable: m.accountsReceivable, customerRequired: m.customerRequired }))); setMode(ms[0].code) } })
      .catch(() => {})
  }, [])
  useEffect(() => {
    if (!custQuery.trim()) { setCustResults([]); return }
    const t = setTimeout(() => { listCustomers(custQuery.trim()).then((r) => setCustResults(r.slice(0, 6))).catch(() => {}) }, 200)
    return () => clearTimeout(t)
  }, [custQuery])

  useEffect(() => {
    const t = setTimeout(() => {
      listItems({ search: search.trim() || undefined }).then((r) => setResults(r.slice(0, 20))).catch(() => {})
    }, 200)
    return () => clearTimeout(t)
  }, [search])

  const grandTotal = useMemo(
    () => cart.reduce((s, l) => s + Number(l.item.sellingPrice) * l.qty, 0),
    [cart],
  )
  const selMode = modes.find((m) => m.code === mode)
  const isAr = !!selMode?.accountsReceivable
  const change = Math.max(0, (Number(paid) || 0) - grandTotal)
  const onAccount = isAr ? Math.max(0, grandTotal - (Number(paid) || 0)) : 0

  function addToCart(item: Item) {
    setCart((prev) => {
      const found = prev.find((l) => l.item.id === item.id)
      if (found) return prev.map((l) => (l.item.id === item.id ? { ...l, qty: l.qty + 1 } : l))
      return [...prev, { item, qty: 1 }]
    })
  }
  function setQty(id: number, qty: number) {
    if (qty <= 0) { setCart((prev) => prev.filter((l) => l.item.id !== id)); return }
    setCart((prev) => prev.map((l) => (l.item.id === id ? { ...l, qty } : l)))
  }

  async function pay() {
    if (cart.length === 0) { toast.error('Cart is empty'); return }
    let payment: number
    if (isAr) {
      if (!customer) { toast.error('A customer is required for a credit sale'); return }
      payment = Number(paid) || 0   // down-payment (optional); remainder goes on account
    } else if (mode === 'CASH') {
      payment = Number(paid)
      if (payment < grandTotal) { toast.error('Cash tendered is less than the total'); return }
    } else {
      payment = grandTotal
    }
    if (selMode?.customerRequired && !customer) { toast.error('This payment mode requires a customer'); return }
    setPlacing(true)
    try {
      const sale = await checkout({
        modeOfPayment: mode,
        payment,
        customerId: customer?.id,
        lines: cart.map((l) => ({ itemId: l.item.id, quantity: l.qty })),
      })
      setLastSale({ num: sale.salesNumber, change: sale.change })
      toast.success(`Sale ${sale.salesNumber} completed`)
      setCart([]); setPaid('')
    } catch (e) { toast.error(posErr(e, 'Checkout failed')) } finally { setPlacing(false) }
  }

  if (shift === 'loading') return <p className="text-slate-400">Loading…</p>
  if (shift === null) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-center">
        <ShoppingCart className="mx-auto mb-2 text-amber-600" />
        <p className="text-amber-800">You need an open shift before you can sell.</p>
        <Link to="/shifts" className={`${btnPrimary} mt-3`}>Go to Shifts</Link>
      </div>
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
      {/* Item search + results */}
      <div className="space-y-3">
        <div className="flex items-center gap-2">
          <ScanLine size={18} className="text-indigo-600" />
          <input className={inputCls} placeholder="Search items by code or name…" value={search}
            onChange={(e) => setSearch(e.target.value)} autoFocus />
        </div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {results.map((it) => (
            <button key={it.id} onClick={() => addToCart(it)}
              className="rounded-lg border border-slate-200 bg-white p-3 text-left hover:border-indigo-400">
              <div className="truncate text-sm font-medium text-slate-700">{it.name}</div>
              <div className="text-xs text-slate-400">{it.code} · {it.quantity} {it.uom}</div>
              <div className="mt-1 text-sm text-indigo-700">{peso(it.sellingPrice)}</div>
            </button>
          ))}
          {results.length === 0 && <p className="col-span-full text-sm text-slate-400">No items.</p>}
        </div>
      </div>

      {/* Cart + payment */}
      <div className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex items-center gap-2 font-semibold text-slate-800">
          <ShoppingCart size={16} className="text-indigo-600" /> Cart
          <span className="ml-auto text-xs font-normal text-slate-400">Shift {shift.shiftNumber}</span>
        </div>

        <div className="max-h-72 space-y-1 overflow-y-auto">
          {cart.length === 0 ? <p className="py-6 text-center text-sm text-slate-400">Tap items to add</p>
            : cart.map((l) => (
              <div key={l.item.id} className="flex items-center gap-2 text-sm">
                <div className="min-w-0 flex-1">
                  <div className="truncate text-slate-700">{l.item.name}</div>
                  <div className="text-xs text-slate-400">{peso(l.item.sellingPrice)} each</div>
                </div>
                <button className="rounded border border-slate-300 p-1 text-slate-500" onClick={() => setQty(l.item.id, l.qty - 1)}><Minus size={12} /></button>
                <input className="w-12 rounded border border-slate-300 px-1 py-0.5 text-center" type="number"
                  value={l.qty} onChange={(e) => setQty(l.item.id, Number(e.target.value))} />
                <button className="rounded border border-slate-300 p-1 text-slate-500" onClick={() => setQty(l.item.id, l.qty + 1)}><Plus size={12} /></button>
                <div className="w-16 text-right text-slate-700">{peso(Number(l.item.sellingPrice) * l.qty)}</div>
                <button className="text-red-500" onClick={() => setQty(l.item.id, 0)}><Trash2 size={13} /></button>
              </div>
            ))}
        </div>

        <div className="flex items-center justify-between border-t border-slate-100 pt-2 text-lg font-semibold text-slate-800">
          <span>Total</span><span>{peso(grandTotal)}</span>
        </div>

        <div className="space-y-2">
          {/* Customer (optional) */}
          {customer ? (
            <div className="flex items-center justify-between rounded-md bg-slate-50 px-2 py-1.5 text-sm">
              <span className="text-slate-700">{customer.name}</span>
              <button className="text-slate-400 hover:text-slate-600" onClick={() => setCustomer(null)}><X size={14} /></button>
            </div>
          ) : (
            <div className="relative">
              <input className={inputCls} placeholder="Attach customer (optional)…" value={custQuery}
                onChange={(e) => setCustQuery(e.target.value)} />
              {custResults.length > 0 && (
                <div className="absolute z-10 mt-1 w-full rounded-md border border-slate-200 bg-white shadow">
                  {custResults.map((c) => (
                    <button key={c.id} className="block w-full px-3 py-1.5 text-left text-sm hover:bg-slate-50"
                      onClick={() => { setCustomer(c); setCustQuery(''); setCustResults([]) }}>
                      {c.name} <span className="text-slate-400">{c.contact ?? ''}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          <select className={inputCls} value={mode} onChange={(e) => setMode(e.target.value)}>
            {modes.map((m) => <option key={m.code} value={m.code}>{m.label}</option>)}
          </select>
          {mode === 'CASH' && (
            <>
              <input className={inputCls} type="number" placeholder="Cash tendered" value={paid}
                onChange={(e) => setPaid(e.target.value)} />
              <div className="flex justify-between text-sm text-slate-500">
                <span>Change</span><span className="font-medium text-slate-700">{peso(change)}</span>
              </div>
            </>
          )}
          {isAr && (
            <>
              <input className={inputCls} type="number" placeholder="Amount paid now (optional)" value={paid}
                onChange={(e) => setPaid(e.target.value)} />
              <div className="flex justify-between text-sm text-amber-700">
                <span>On account</span><span className="font-medium">{peso(onAccount)}</span>
              </div>
              {!customer && <p className="text-xs text-amber-600">Attach a customer above to sell on credit.</p>}
            </>
          )}
        </div>

        <button className={`${btnPrimary} w-full`} disabled={placing || cart.length === 0 || (isAr && !customer)} onClick={pay}>
          {placing ? 'Processing…' : isAr && onAccount > 0 ? `Charge ${peso(onAccount)} to account` : `Charge ${peso(grandTotal)}`}
        </button>

        {lastSale && (
          <div className="rounded-md bg-emerald-50 border border-emerald-200 p-3 text-sm text-emerald-800">
            <div className="font-medium">Sale {lastSale.num} completed</div>
            <div>Change due: {peso(lastSale.change)}</div>
            <button className={`${btnGhost} mt-2`} onClick={() => setLastSale(null)}>New sale</button>
          </div>
        )}
      </div>
    </div>
  )
}
