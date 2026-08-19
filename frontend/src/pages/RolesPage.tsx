import { useEffect, useMemo, useState } from 'react'
import { Plus, Shield, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import {
  createRole, deleteRole, listModules, listRoles, updateRole,
  type ModuleInfo, type RoleDetail,
} from '@/lib/admin'

function apiErr(e: unknown, fallback: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? fallback
}

/**
 * Groups for the module picker, roughly mirroring the sidebar so permissions are
 * found where they're used. Codes listed here appear in this order; anything not
 * listed still shows, under "Other", so a newly added module is never hidden.
 */
const MODULE_GROUPS: { title: string; codes: string[] }[] = [
  { title: 'Sell', codes: ['POS', 'SALES', 'SHIFTS', 'PRICE_OVERRIDE'] },
  { title: 'Catalog & inventory', codes: [
    'INVENTORY', 'INVENTORY_CREATE', 'INVENTORY_EDIT', 'INVENTORY_ADJUST', 'INVENTORY_IMPORT',
    'STOCKTAKE', 'CATEGORIES', 'LOCATIONS', 'UOMS',
  ] },
  { title: 'Procurement', codes: [
    'SUPPLIERS', 'PURCHASE_ORDERS', 'PO_APPROVERS', 'GOODS_RECEIPTS',
    'STOCK_TRANSFER', 'STOCK_TRANSFER_POLICY',
  ] },
  { title: 'Customers & promos', codes: ['CUSTOMERS', 'VOUCHERS', 'LOYALTY_CONFIG', 'LOYALTY_REWARDS'] },
  { title: 'Finance', codes: ['RECEIVABLES', 'PAYABLES'] },
  { title: 'Reports', codes: [
    'SALES_REPORTS', 'SALES_ANALYTICS', 'PROFIT_REPORT', 'INVENTORY_SNAPSHOT', 'INVENTORY_VALUATION',
    'INVENTORY_MOVEMENT', 'REORDER_REPORT', 'DEAD_STOCK_REPORT', 'CUSTOMER_REPORT', 'DISCOUNTS_REPORT',
    'SHIFT_HISTORY_REPORT', 'GOODS_RECEIPTS_REPORT', 'PURCHASE_ORDERS_REPORT', 'STOCK_TRANSFER_REPORT',
    'RECEIVABLES_REPORT', 'PAYABLES_REPORT',
  ] },
  { title: 'Administration', codes: [
    'USERS', 'ROLES', 'SITES', 'SHIFTS_ADMIN', 'PAYMENT_MODES', 'DOC_SETTINGS', 'PDF_CONFIG',
    'RECEIPT_CONFIG', 'EMAIL_CONFIG', 'AUDIT_LOG', 'ERROR_LOG',
  ] },
]

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleDetail[]>([])
  const [modules, setModules] = useState<ModuleInfo[]>([])
  const [editing, setEditing] = useState<RoleDetail | 'new' | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try {
      const [r, m] = await Promise.all([listRoles(), listModules()])
      setRoles(r)
      setModules(m)
    } catch (e) {
      toast.error(apiErr(e, 'Failed to load roles'))
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { reload() }, [])

  async function remove(role: RoleDetail) {
    if (!confirm(`Delete role ${role.code}?`)) return
    try {
      await deleteRole(role.id)
      toast.success('Role deleted')
      reload()
    } catch (e) {
      toast.error(apiErr(e, 'Delete failed'))
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Shield size={18} className="text-blue-600" /> Roles
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New role</button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Code</th>
              <th className="px-4 py-2 font-medium">Name</th>
              <th className="px-4 py-2 font-medium">Modules</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
            ) : (
              roles.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">
                    {r.code} {r.builtIn && <span className="ml-1 rounded bg-slate-100 px-1 text-xs text-slate-500">built-in</span>}
                  </td>
                  <td className="px-4 py-2">{r.name}</td>
                  <td className="px-4 py-2 text-slate-500">{r.wildcard ? 'All modules' : `${r.moduleCodes.length} modules`}</td>
                  <td className="px-4 py-2 text-right">
                    {!r.wildcard && (
                      <button className="text-blue-600 hover:underline" onClick={() => setEditing(r)}>Edit</button>
                    )}
                    {!r.builtIn && (
                      <button className="ml-3 text-red-600 hover:underline inline-flex items-center gap-1" onClick={() => remove(r)}>
                        <Trash2 size={13} /> Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <RoleForm
          role={editing === 'new' ? null : editing}
          modules={modules}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); reload() }}
        />
      )}
    </div>
  )
}

function RoleForm({
  role, modules, onClose, onSaved,
}: { role: RoleDetail | null; modules: ModuleInfo[]; onClose: () => void; onSaved: () => void }) {
  const isNew = role === null
  const [code, setCode] = useState(role?.code ?? '')
  const [name, setName] = useState(role?.name ?? '')
  const [description, setDescription] = useState(role?.description ?? '')
  const [selected, setSelected] = useState<Set<string>>(new Set(role?.moduleCodes ?? []))
  const [saving, setSaving] = useState(false)

  function toggle(codeVal: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(codeVal)) next.delete(codeVal)
      else next.add(codeVal)
      return next
    })
  }

  // Bucket the API's modules into MODULE_GROUPS, keeping any unmapped code under
  // "Other" so nothing can silently disappear from the picker.
  const grouped = useMemo(() => {
    const byCode = new Map(modules.map((m) => [m.code, m]))
    const seen = new Set<string>()
    const out = MODULE_GROUPS.map((g) => {
      const items = g.codes.flatMap((c) => {
        const m = byCode.get(c)
        if (!m) return []
        seen.add(c)
        return [m]
      })
      return { title: g.title, items }
    }).filter((g) => g.items.length > 0)
    const rest = modules.filter((m) => !seen.has(m.code))
    if (rest.length) out.push({ title: 'Other', items: rest })
    return out
  }, [modules])

  /** Tick or untick a whole group at once — a role is usually "all reports" or none. */
  function toggleGroup(items: ModuleInfo[], on: boolean) {
    setSelected((prev) => {
      const next = new Set(prev)
      items.forEach((m) => (on ? next.add(m.code) : next.delete(m.code)))
      return next
    })
  }

  async function save() {
    if (selected.size === 0) { toast.error('Select at least one module'); return }
    setSaving(true)
    try {
      const moduleCodes = [...selected]
      if (isNew) {
        await createRole({ code: code.trim().toUpperCase(), name: name.trim(), description: description.trim() || undefined, moduleCodes })
        toast.success('Role created')
      } else {
        await updateRole(role!.id, { name: name.trim(), description: description.trim() || undefined, moduleCodes })
        toast.success('Role updated')
      }
      onSaved()
    } catch (e) {
      toast.error(apiErr(e, 'Save failed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isNew ? 'New role' : `Edit ${role!.code}`} onClose={onClose} width="max-w-2xl">
      <div className="space-y-4">
        {isNew && (
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Code</label>
            <input className={inputCls} value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="VIEWER" />
          </div>
        )}
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Name</label>
          <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Description</label>
          <input className={inputCls} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div>
          <div className="mb-1 flex items-center justify-between">
            <label className="text-sm font-medium text-slate-700">Modules ({selected.size})</label>
            <button type="button" className="text-xs text-blue-600 hover:underline"
              onClick={() => setSelected(new Set(modules.map((m) => m.code)))}>Select all</button>
          </div>
          <div className="max-h-72 space-y-3 overflow-y-auto rounded-md border border-slate-200 p-2">
            {grouped.map((g) => {
              const on = g.items.filter((m) => selected.has(m.code)).length
              const all = on === g.items.length
              return (
                <div key={g.title}>
                  <div className="sticky top-0 z-10 -mx-2 flex items-center gap-2 border-b border-slate-100 bg-white px-2 pb-1 pt-0.5">
                    <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-slate-500">{g.title}</span>
                    <span className="num text-[11px] text-slate-400">{on}/{g.items.length}</span>
                    <button type="button" className="ml-auto text-[11px] text-blue-600 hover:underline"
                      onClick={() => toggleGroup(g.items, !all)}>
                      {all ? 'Clear' : 'Select all'}
                    </button>
                  </div>
                  <div className="mt-1 grid grid-cols-1 gap-1 sm:grid-cols-2">
                    {g.items.map((m) => (
                      <label key={m.code} className="flex items-start gap-2 rounded px-2 py-1 text-sm hover:bg-slate-50">
                        <input type="checkbox" className="mt-0.5" checked={selected.has(m.code)} onChange={() => toggle(m.code)} />
                        <span><span className="text-slate-700">{m.name}</span> <span className="text-slate-400">· {m.code}</span></span>
                      </label>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}
