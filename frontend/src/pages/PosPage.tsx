import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { History, Minus, PauseCircle, Plus, ScanLine, ShieldCheck, ShoppingCart, Trash2, X } from 'lucide-react'
import { toast } from 'sonner'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import { listItems, type Item } from '@/lib/inventory'
import {
  checkout, deleteHeldSale, getCurrentShift, getHeldSale, listHeldSales, PAYMENT_MODES, peso, posErr,
  saveHeldSale, type HeldSaleSummary, type Shift,
} from '@/lib/pos'
import { listActivePaymentModes, listCustomers, type Customer } from '@/lib/parties'
import { validateVoucher } from '@/lib/promo'
import { useAuthStore } from '@/store/authStore'

interface CartLine { item: Item; qty: number; overridePrice?: number; overrideReason?: string }

/** Effective unit price for a cart line — the override when set, else the catalog price. */
const linePrice = (l: CartLine) => l.overridePrice ?? Number(l.item.sellingPrice)
const isOverridden = (l: CartLine) => l.overridePrice != null && l.overridePrice !== Number(l.item.sellingPrice)

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
  const [voucherInput, setVoucherInput] = useState('')
  const [voucher, setVoucher] = useState<{ code: string; discount: number } | null>(null)
  const [checkingVoucher, setCheckingVoucher] = useState(false)
  const [held, setHeld] = useState<HeldSaleSummary[]>([])
  const [heldOpen, setHeldOpen] = useState(false)
  const [splitMode, setSplitMode] = useState(false)
  const [tenders, setTenders] = useState<{ mode: string; amount: string }[]>([])
  const [approvalOpen, setApprovalOpen] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)
  const canOverride = useAuthStore((s) => s.user?.modules?.includes('PRICE_OVERRIDE') ?? false)

  const refreshHeld = () => listHeldSales().then(setHeld).catch(() => {})

  useEffect(() => { getCurrentShift().then(setShift).catch(() => setShift(null)) }, [])
  useEffect(() => { refreshHeld() }, [])
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
    () => cart.reduce((s, l) => s + linePrice(l) * l.qty, 0),
    [cart],
  )
  const overriddenLines = cart.filter(isOverridden)
  const selMode = modes.find((m) => m.code === mode)
  const isAr = !!selMode?.accountsReceivable
  const netTotal = Math.max(0, grandTotal - (voucher?.discount ?? 0))
  const change = Math.max(0, (Number(paid) || 0) - netTotal)
  const onAccount = isAr ? Math.max(0, netTotal - (Number(paid) || 0)) : 0

  // Split tender (paid methods only — no accounts-receivable).
  const splitModes = modes.filter((m) => !m.accountsReceivable)
  const appliedSum = tenders.reduce((s, t) => s + (Number(t.amount) || 0), 0)
  const remaining = Math.max(0, netTotal - appliedSum)
  const splitChange = Math.max(0, appliedSum - netTotal)
  const startSplit = () => {
    setSplitMode(true)
    setTenders([{ mode: (splitModes[0]?.code ?? 'CASH'), amount: netTotal ? String(netTotal.toFixed(2)) : '' }])
  }
  const addTender = () => setTenders((p) => [...p, { mode: splitModes[0]?.code ?? 'CASH', amount: '' }])
  const removeTender = (i: number) => setTenders((p) => p.filter((_, idx) => idx !== i))
  const updateTender = (i: number, k: 'mode' | 'amount', v: string) =>
    setTenders((p) => p.map((t, idx) => (idx === i ? { ...t, [k]: v } : t)))

  async function applyVoucher() {
    const code = voucherInput.trim()
    if (!code) return
    if (grandTotal <= 0) { toast.error('Add items first'); return }
    setCheckingVoucher(true)
    try {
      const ev = await validateVoucher(code, grandTotal, customer?.id)
      if (!ev.valid) { toast.error(ev.message ?? 'Voucher not valid'); setVoucher(null); return }
      setVoucher({ code: ev.code, discount: ev.discountAmount })
      toast.success(`Voucher ${ev.code} — ${peso(ev.discountAmount)} off`)
    } catch (e) { toast.error(posErr(e, 'Could not check voucher')) } finally { setCheckingVoucher(false) }
  }

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
  function setOverridePrice(id: number, raw: string) {
    setCart((prev) => prev.map((l) => {
      if (l.item.id !== id) return l
      if (raw === '') { const { overridePrice, ...rest } = l; return rest }
      return { ...l, overridePrice: Math.max(0, Number(raw)) }
    }))
  }
  function setOverrideReason(id: number, reason: string) {
    setCart((prev) => prev.map((l) => (l.item.id === id ? { ...l, overrideReason: reason } : l)))
  }

  async function pay(approval?: { user: string; password: string }) {
    if (cart.length === 0) { toast.error('Cart is empty'); return }

    // A price override needs authorization: cashiers with PRICE_OVERRIDE self-approve;
    // otherwise collect a manager's credentials via the approval modal first.
    if (overriddenLines.length > 0 && !canOverride && !approval) { setApprovalOpen(true); return }

    const base = {
      customerId: customer?.id,
      voucherCode: voucher?.code,
      ...(approval ? { approvalUser: approval.user, approvalPassword: approval.password } : {}),
      lines: cart.map((l) => (isOverridden(l)
        ? { itemId: l.item.id, quantity: l.qty, unitPrice: l.overridePrice, overrideReason: l.overrideReason?.trim() || undefined }
        : { itemId: l.item.id, quantity: l.qty })),
    }
    let payload
    if (splitMode) {
      const payments = tenders
        .filter((t) => Number(t.amount) > 0)
        .map((t) => ({ mode: t.mode, amount: Number(t.amount) }))
      if (payments.length === 0) { toast.error('Add at least one payment'); return }
      if (appliedSum + 0.001 < netTotal) { toast.error('Payments are less than the total'); return }
      if (splitModes.some((m) => m.customerRequired && payments.some((p) => p.mode === m.code)) && !customer) {
        toast.error('One of the selected modes requires a customer'); return
      }
      payload = { ...base, modeOfPayment: 'SPLIT', payment: appliedSum, payments }
    } else {
      let payment: number
      if (isAr) {
        if (!customer) { toast.error('A customer is required for a credit sale'); return }
        payment = Number(paid) || 0   // down-payment (optional); remainder goes on account
      } else if (mode === 'CASH') {
        payment = Number(paid)
        if (payment < netTotal) { toast.error('Cash tendered is less than the total'); return }
      } else {
        payment = netTotal
      }
      if (selMode?.customerRequired && !customer) { toast.error('This payment mode requires a customer'); return }
      payload = { ...base, modeOfPayment: mode, payment }
    }

    setPlacing(true)
    try {
      const sale = await checkout(payload)
      setLastSale({ num: sale.salesNumber, change: sale.change })
      toast.success(`Sale ${sale.salesNumber} completed`)
      setCart([]); setPaid(''); setVoucher(null); setVoucherInput(''); setSplitMode(false); setTenders([]); setApprovalOpen(false)
    } catch (e) { toast.error(posErr(e, 'Checkout failed')) } finally { setPlacing(false) }
  }

  // ── Hold / recall ──────────────────────────────────────────────────────────
  async function hold() {
    if (cart.length === 0) { toast.error('Cart is empty'); return }
    const label = window.prompt('Name this held sale (optional):')
    if (label === null) return // cancelled
    const units = cart.reduce((s, l) => s + l.qty, 0)
    try {
      await saveHeldSale({
        label: label.trim() || undefined,
        customerId: customer?.id,
        customerName: customer?.name,
        itemCount: units,
        totalAmount: grandTotal,
        cartJson: JSON.stringify({ lines: cart, customer, voucher, mode }),
      })
      toast.success('Sale held — recall it anytime from Held')
      setCart([]); setPaid(''); setVoucher(null); setVoucherInput(''); setCustomer(null)
      refreshHeld()
    } catch (e) { toast.error(posErr(e, 'Could not hold this sale')) }
  }

  async function recall(id: number) {
    if (cart.length > 0 && !window.confirm('Replace the current cart with this held sale?')) return
    try {
      const h = await getHeldSale(id)
      const snap = JSON.parse(h.cartJson) as {
        lines?: CartLine[]; customer?: Customer | null
        voucher?: { code: string; discount: number } | null; mode?: string
      }
      setCart(snap.lines ?? [])
      setCustomer(snap.customer ?? null)
      setVoucher(snap.voucher ?? null)
      setVoucherInput('')
      if (snap.mode) setMode(snap.mode)
      setPaid(''); setSplitMode(false); setTenders([])
      await deleteHeldSale(id) // recalling consumes the hold
      refreshHeld()
      setHeldOpen(false)
      toast.success('Held sale recalled')
    } catch (e) { toast.error(posErr(e, 'Could not recall this sale')) }
  }

  async function discardHeld(id: number) {
    try { await deleteHeldSale(id); refreshHeld(); toast.success('Held sale discarded') }
    catch (e) { toast.error(posErr(e, 'Could not discard')) }
  }

  // ── POS action hotkeys — F2 search · F4 hold · F8 recall · F9 charge/new ─────
  // Kept in refs so the always-on listener sees the latest cart/payment state.
  const newSale = () => { setLastSale(null); setPaid(''); searchRef.current?.focus() }
  const lastSaleRef = useRef(lastSale)
  lastSaleRef.current = lastSale
  const actions = useRef<{ pay: () => void; hold: () => void; openHeld: () => void; newSale: () => void }>(
    { pay, hold, openHeld: () => {}, newSale })
  actions.current = { pay, hold, openHeld: () => { refreshHeld(); setHeldOpen(true) }, newSale }
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      switch (e.key) {
        case 'F2': e.preventDefault(); searchRef.current?.focus(); searchRef.current?.select(); break
        case 'F4': e.preventDefault(); actions.current.hold(); break
        case 'F8': e.preventDefault(); actions.current.openHeld(); break
        // After a completed sale F9 starts the next one; otherwise it charges.
        case 'F9': e.preventDefault(); if (lastSaleRef.current) actions.current.newSale(); else actions.current.pay(); break
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

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
          <ScanLine size={18} className="text-blue-600" />
          <input ref={searchRef} className={inputCls} placeholder="Search items by code, name or barcode…   (F2)" value={search}
            onChange={(e) => setSearch(e.target.value)} autoFocus />
        </div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {results.map((it) => (
            <button key={it.id} onClick={() => addToCart(it)}
              className="rounded-lg border border-slate-200 bg-white p-3 text-left transition hover:border-blue-500 hover:shadow-sm hover:shadow-blue-600/10">
              <div className="truncate text-sm font-medium text-slate-700">{it.name}</div>
              <div className="num text-xs text-slate-400">{it.code} · {it.quantity} {it.uom}</div>
              <div className="num mt-1 text-sm font-semibold text-blue-700">{peso(it.sellingPrice)}</div>
            </button>
          ))}
          {results.length === 0 && <p className="col-span-full text-sm text-slate-400">No items.</p>}
        </div>
      </div>

      {/* Cart + payment */}
      <div className="space-y-3 overflow-hidden rounded-xl border border-slate-200 bg-white p-4">
        <div className="safety-stripe -mx-4 -mt-4 mb-1 h-1" />
        <div className="flex items-center gap-2 font-semibold text-slate-800">
          <ShoppingCart size={16} className="text-blue-600" /> Cart
          <div className="ml-auto flex items-center gap-2">
            <button
              className="inline-flex items-center gap-1 rounded-md border border-slate-200 px-2 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50"
              onClick={() => { refreshHeld(); setHeldOpen(true) }}
              title="Recall a held sale (F8)"
            >
              <History size={13} /> Held{held.length ? ` (${held.length})` : ''}
              <span className="rounded border border-slate-200 px-1 text-[10px] font-semibold text-slate-400">F8</span>
            </button>
            <span className="text-xs font-normal text-slate-400">Shift {shift.shiftNumber}</span>
          </div>
        </div>

        <div className="max-h-72 space-y-1 overflow-y-auto">
          {cart.length === 0 ? <p className="py-6 text-center text-sm text-slate-400">Tap items to add</p>
            : cart.map((l) => (
              <div key={l.item.id} className="space-y-1">
                <div className="flex items-center gap-2 text-sm">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-slate-700">{l.item.name}</div>
                    <div className="flex items-center gap-1 text-xs text-slate-400">
                      <span>₱</span>
                      <input
                        className={`w-16 rounded border px-1 py-0.5 text-xs ${isOverridden(l) ? 'border-amber-300 text-amber-700' : 'border-slate-200 text-slate-500'}`}
                        type="number" min={0} value={l.overridePrice ?? Number(l.item.sellingPrice)}
                        onChange={(e) => setOverridePrice(l.item.id, e.target.value)} title="Override the unit price" />
                      <span>each</span>
                      {isOverridden(l) && <span className="text-slate-400 line-through">{peso(l.item.sellingPrice)}</span>}
                    </div>
                  </div>
                  <button className="rounded border border-slate-300 p-1 text-slate-500" onClick={() => setQty(l.item.id, l.qty - 1)}><Minus size={12} /></button>
                  <input className="w-12 rounded border border-slate-300 px-1 py-0.5 text-center" type="number"
                    value={l.qty} onChange={(e) => setQty(l.item.id, Number(e.target.value))} />
                  <button className="rounded border border-slate-300 p-1 text-slate-500" onClick={() => setQty(l.item.id, l.qty + 1)}><Plus size={12} /></button>
                  <div className="w-16 text-right text-slate-700">{peso(linePrice(l) * l.qty)}</div>
                  <button className="text-red-500" onClick={() => setQty(l.item.id, 0)}><Trash2 size={13} /></button>
                </div>
                {isOverridden(l) && (
                  <input className="w-full rounded border border-amber-200 bg-amber-50/40 px-2 py-1 text-xs text-slate-600"
                    placeholder="Reason for price override (optional)" value={l.overrideReason ?? ''}
                    onChange={(e) => setOverrideReason(l.item.id, e.target.value)} />
                )}
              </div>
            ))}
        </div>

        <div className="flex items-center justify-between border-t border-slate-100 pt-2 text-sm text-slate-600">
          <span>Subtotal</span><span>{peso(grandTotal)}</span>
        </div>

        {/* Voucher */}
        {voucher ? (
          <div className="flex items-center justify-between rounded-md bg-emerald-50 px-2 py-1.5 text-sm">
            <span className="text-emerald-800">{voucher.code} · −{peso(voucher.discount)}</span>
            <button className="text-slate-400 hover:text-slate-600" onClick={() => { setVoucher(null); setVoucherInput('') }}><X size={14} /></button>
          </div>
        ) : (
          <div className="flex gap-2">
            <input className={inputCls} placeholder="Voucher code" value={voucherInput}
              onChange={(e) => setVoucherInput(e.target.value.toUpperCase())} onKeyDown={(e) => { if (e.key === 'Enter') applyVoucher() }} />
            <button className={btnGhost} disabled={checkingVoucher || !voucherInput.trim()} onClick={applyVoucher}>Apply</button>
          </div>
        )}

        <div className="flex items-center justify-between text-lg font-semibold text-slate-800">
          <span>Total</span><span>{peso(netTotal)}</span>
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

          {!splitMode ? (
            <>
              <div className="flex items-center gap-2">
                <select className={`${inputCls} flex-1`} value={mode} onChange={(e) => setMode(e.target.value)}>
                  {modes.map((m) => <option key={m.code} value={m.code}>{m.label}</option>)}
                </select>
                {splitModes.length > 1 && (
                  <button type="button" className="whitespace-nowrap text-xs font-medium text-blue-600 hover:underline" onClick={startSplit}>Split</button>
                )}
              </div>
              {mode === 'CASH' && (
                <>
                  <input className={inputCls} type="number" placeholder="Cash tendered  (Enter = charge)" value={paid}
                    onChange={(e) => setPaid(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') pay() }} />
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
            </>
          ) : (
            <div className="space-y-2 rounded-md border border-slate-200 p-2">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-slate-600">Split payment</span>
                <button type="button" className="text-xs text-blue-600 hover:underline" onClick={() => { setSplitMode(false); setTenders([]) }}>Single</button>
              </div>
              {tenders.map((t, i) => (
                <div key={i} className="flex gap-2">
                  <select className={`${inputCls} flex-1`} value={t.mode} onChange={(e) => updateTender(i, 'mode', e.target.value)}>
                    {splitModes.map((m) => <option key={m.code} value={m.code}>{m.label}</option>)}
                  </select>
                  <input className={`${inputCls} w-28`} type="number" placeholder="Amount" value={t.amount}
                    onChange={(e) => updateTender(i, 'amount', e.target.value)} />
                  {tenders.length > 1 && <button type="button" className="px-1 text-red-500" onClick={() => removeTender(i)}><X size={14} /></button>}
                </div>
              ))}
              <button type="button" className={btnGhost} onClick={addTender}><Plus size={14} /> Add payment</button>
              <div className="flex justify-between text-sm text-slate-500"><span>Applied</span><span className="font-medium text-slate-700">{peso(appliedSum)}</span></div>
              {remaining > 0
                ? <div className="flex justify-between text-sm text-amber-700"><span>Remaining</span><span className="font-medium">{peso(remaining)}</span></div>
                : <div className="flex justify-between text-sm text-slate-500"><span>Change</span><span className="font-medium text-slate-700">{peso(splitChange)}</span></div>}
            </div>
          )}
        </div>

        {overriddenLines.length > 0 && !canOverride && (
          <p className="flex items-center gap-1 text-xs text-amber-600">
            <ShieldCheck size={13} /> Price override — a manager must approve at checkout.
          </p>
        )}

        <button className={`${btnPrimary} w-full`} disabled={placing || cart.length === 0 || (!splitMode && isAr && !customer) || (splitMode && remaining > 0)} onClick={() => pay()}>
          {placing ? 'Processing…' : !splitMode && isAr && onAccount > 0 ? `Charge ${peso(onAccount)} to account` : `Charge ${peso(netTotal)}`}
          <span className="ml-1.5 rounded bg-white/20 px-1 text-[11px] font-semibold">F9</span>
        </button>

        <button className={`${btnGhost} w-full`} disabled={placing || cart.length === 0} onClick={hold}>
          <PauseCircle size={15} /> Hold sale
          <span className="ml-1 rounded border border-slate-200 px-1 text-[11px] font-semibold text-slate-400">F4</span>
        </button>

        {lastSale && (
          <div className="rounded-md bg-emerald-50 border border-emerald-200 p-3 text-sm text-emerald-800">
            <div className="font-medium">Sale {lastSale.num} completed</div>
            <div>Change due: {peso(lastSale.change)}</div>
            <button className={`${btnGhost} mt-2`} onClick={newSale} autoFocus>
              New sale
              <span className="ml-1 rounded border border-emerald-300 px-1 text-[11px] font-semibold text-emerald-700">N / F9</span>
            </button>
          </div>
        )}
      </div>

      {heldOpen && (
        <Modal title="Held sales" onClose={() => setHeldOpen(false)} width="max-w-lg">
          {held.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-400">No held sales at this site.</p>
          ) : (
            <div className="space-y-2">
              {held.map((h) => (
                <div key={h.heldId} className="flex items-center gap-3 rounded-lg border border-slate-200 p-3">
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-slate-700">{h.label || `Held #${h.heldId}`}</div>
                    <div className="truncate text-xs text-slate-400">
                      {h.itemCount} item{h.itemCount === 1 ? '' : 's'} · {peso(h.totalAmount)}
                      {h.customerName ? ` · ${h.customerName}` : ''} · {h.createdBy} · {new Date(h.creationDate).toLocaleString()}
                    </div>
                  </div>
                  <button className={btnPrimary} onClick={() => recall(h.heldId)}>Recall</button>
                  <button className="p-1 text-red-500 hover:text-red-600" title="Discard" onClick={() => discardHeld(h.heldId)}>
                    <Trash2 size={15} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </Modal>
      )}

      {approvalOpen && (
        <ApprovalModal
          count={overriddenLines.length}
          giveback={overriddenLines.reduce((s, l) => s + (Number(l.item.sellingPrice) - linePrice(l)) * l.qty, 0)}
          busy={placing}
          onClose={() => setApprovalOpen(false)}
          onApprove={(user, password) => pay({ user, password })}
        />
      )}
    </div>
  )
}

/** Manager-approval prompt for a price override when the cashier lacks PRICE_OVERRIDE. */
function ApprovalModal({
  count, giveback, busy, onClose, onApprove,
}: { count: number; giveback: number; busy: boolean; onClose: () => void; onApprove: (user: string, password: string) => void }) {
  const [user, setUser] = useState('')
  const [password, setPassword] = useState('')
  const submit = () => {
    if (!user.trim() || !password) { toast.error('Enter manager username and password'); return }
    onApprove(user.trim(), password)
  }
  return (
    <Modal title="Manager approval" onClose={onClose} width="max-w-sm">
      <div className="space-y-3">
        <p className="text-sm text-slate-600">
          {count} line{count === 1 ? '' : 's'} priced below catalog{giveback > 0 ? ` (${peso(giveback)} off)` : ''}.
          A manager with override rights must approve.
        </p>
        <input className={inputCls} placeholder="Manager username" value={user} autoFocus
          onChange={(e) => setUser(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') submit() }} />
        <input className={inputCls} type="password" placeholder="Manager password" value={password}
          onChange={(e) => setPassword(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') submit() }} />
        <div className="flex justify-end gap-2 pt-1">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={busy} onClick={submit}>
            <ShieldCheck size={15} /> {busy ? 'Approving…' : 'Approve & charge'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
