import { useEffect, useState, type Dispatch, type SetStateAction } from 'react'
import { KeyRound, Plus, Users } from 'lucide-react'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import Modal, { btnGhost, btnPrimary, inputCls } from '@/components/Modal'
import {
  createUser, getUser, listUserRoles, listUsers, listUserSites, resetUserPassword, updateUser,
  type RoleRef, type SiteRef, type UserSummary,
} from '@/lib/admin'

function apiErr(e: unknown, fallback: string) {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? fallback
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserSummary[]>([])
  const [roles, setRoles] = useState<RoleRef[]>([])
  const [sites, setSites] = useState<SiteRef[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<UserSummary | 'new' | null>(null)
  const [resetting, setResetting] = useState<UserSummary | null>(null)
  const [loading, setLoading] = useState(true)

  async function reload() {
    setLoading(true)
    try {
      const [u, r, s] = await Promise.all([listUsers(includeInactive), listUserRoles(), listUserSites()])
      setUsers(u); setRoles(r); setSites(s)
    } catch (e) {
      toast.error(apiErr(e, 'Failed to load users'))
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [includeInactive])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
          <Users size={18} className="text-blue-600" /> Users
        </h1>
        <button className={btnPrimary} onClick={() => setEditing('new')}><Plus size={16} /> New user</button>
      </div>

      <label className="flex items-center gap-2 text-sm text-slate-500">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} /> Show inactive
      </label>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Username</th>
              <th className="px-4 py-2 font-medium">Full name</th>
              <th className="px-4 py-2 font-medium">Roles</th>
              <th className="px-4 py-2 font-medium">Sites</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>
            ) : (
              users.map((u) => (
                <tr key={u.id}>
                  <td className="px-4 py-2 font-medium text-slate-700">{u.username}</td>
                  <td className="px-4 py-2">{u.fullName}</td>
                  <td className="px-4 py-2 text-slate-500">{u.roleCodes.join(', ')}</td>
                  <td className="px-4 py-2 text-slate-500">{u.siteCodes.join(', ')}</td>
                  <td className="px-4 py-2">
                    <span className={u.active ? 'text-emerald-600' : 'text-slate-400'}>{u.active ? 'Active' : 'Inactive'}</span>
                    {u.locked && <span className="ml-1 text-amber-600">· locked</span>}
                  </td>
                  <td className="px-4 py-2 text-right whitespace-nowrap">
                    <button className="text-blue-600 hover:underline" onClick={() => setEditing(u)}>Edit</button>
                    <button className="ml-3 inline-flex items-center gap-1 text-slate-500 hover:underline" onClick={() => setResetting(u)}>
                      <KeyRound size={13} /> Reset
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <UserForm
          summary={editing === 'new' ? null : editing}
          roles={roles} sites={sites}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); reload() }}
        />
      )}
      {resetting && <ResetForm user={resetting} onClose={() => setResetting(null)} />}
    </div>
  )
}

function CheckList<T extends { code: string; name: string }>({
  items, selected, onToggle,
}: { items: T[]; selected: Set<string>; onToggle: (c: string) => void }) {
  return (
    <div className="grid grid-cols-1 gap-1 rounded-md border border-slate-200 p-2 sm:grid-cols-2">
      {items.map((it) => (
        <label key={it.code} className="flex items-center gap-2 rounded px-2 py-1 text-sm hover:bg-slate-50">
          <input type="checkbox" checked={selected.has(it.code)} onChange={() => onToggle(it.code)} />
          <span className="text-slate-700">{it.name}</span>
          <span className="text-slate-400">· {it.code}</span>
        </label>
      ))}
    </div>
  )
}

function UserForm({
  summary, roles, sites, onClose, onSaved,
}: {
  summary: UserSummary | null
  roles: RoleRef[]
  sites: SiteRef[]
  onClose: () => void
  onSaved: () => void
}) {
  const isNew = summary === null
  const [username, setUsername] = useState(summary?.username ?? '')
  const [fullName, setFullName] = useState(summary?.fullName ?? '')
  const [email, setEmail] = useState(summary?.email ?? '')
  const [password, setPassword] = useState('')
  const [active, setActive] = useState(summary?.active ?? true)
  const [roleCodes, setRoleCodes] = useState<Set<string>>(new Set(summary?.roleCodes ?? []))
  const [siteCodes, setSiteCodes] = useState<Set<string>>(new Set(summary?.siteCodes ?? []))
  const [saving, setSaving] = useState(false)

  // On edit, hydrate exact role/site codes from the detail endpoint.
  useEffect(() => {
    if (summary) {
      getUser(summary.id)
        .then((d) => {
          setRoleCodes(new Set(d.roles.map((r) => r.code)))
          setSiteCodes(new Set(d.sites.map((s) => s.code)))
        })
        .catch(() => {})
    }
  }, [summary])

  function toggler(set: Dispatch<SetStateAction<Set<string>>>) {
    return (c: string) => set((prev) => {
      const next = new Set(prev)
      if (next.has(c)) next.delete(c); else next.add(c)
      return next
    })
  }

  async function save() {
    if (roleCodes.size === 0) { toast.error('Select at least one role'); return }
    if (siteCodes.size === 0) { toast.error('Select at least one site'); return }
    setSaving(true)
    try {
      if (isNew) {
        await createUser({
          username: username.trim(), fullName: fullName.trim(), email: email.trim() || undefined,
          initialPassword: password, roleCodes: [...roleCodes], siteCodes: [...siteCodes],
        })
        toast.success('User created')
      } else {
        await updateUser(summary!.id, {
          fullName: fullName.trim(), email: email.trim() || undefined, active,
          roleCodes: [...roleCodes], siteCodes: [...siteCodes],
        })
        toast.success('User updated')
      }
      onSaved()
    } catch (e) {
      toast.error(apiErr(e, 'Save failed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isNew ? 'New user' : `Edit ${summary!.username}`} onClose={onClose} width="max-w-2xl">
      <div className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Username</label>
            <input className={inputCls} value={username} disabled={!isNew}
              onChange={(e) => setUsername(e.target.value)} placeholder="cashier1" />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Full name</label>
            <input className={inputCls} value={fullName} onChange={(e) => setFullName(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Email</label>
            <input className={inputCls} value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          {isNew ? (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Initial password</label>
              <input className={inputCls} type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
          ) : (
            <label className="flex items-center gap-2 self-end pb-2 text-sm text-slate-700">
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} /> Active
            </label>
          )}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Roles</label>
          <CheckList items={roles} selected={roleCodes} onToggle={toggler(setRoleCodes)} />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Site access</label>
          <CheckList items={sites} selected={siteCodes} onToggle={toggler(setSiteCodes)} />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </Modal>
  )
}

function ResetForm({ user, onClose }: { user: UserSummary; onClose: () => void }) {
  const [pw, setPw] = useState('')
  const [force, setForce] = useState(true)
  const [saving, setSaving] = useState(false)

  async function save() {
    setSaving(true)
    try {
      await resetUserPassword(user.id, { newPassword: pw, forceChangeOnNextLogin: force })
      toast.success(`Password reset for ${user.username}`)
      onClose()
    } catch (e) {
      toast.error(apiErr(e, 'Reset failed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={`Reset password · ${user.username}`} onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">New password</label>
          <input className={inputCls} type="password" value={pw} onChange={(e) => setPw(e.target.value)} />
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={force} onChange={(e) => setForce(e.target.checked)} /> Force change on next login
        </label>
        <p className="text-xs text-slate-400">All of this user's sessions will be signed out.</p>
        <div className="flex justify-end gap-2 pt-2">
          <button className={btnGhost} onClick={onClose}>Cancel</button>
          <button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Reset password'}</button>
        </div>
      </div>
    </Modal>
  )
}
