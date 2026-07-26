import api from '@/lib/api'

// ── Sites ────────────────────────────────────────────────────────────────────
export interface SiteSummary {
  id: number
  code: string
  name: string
  address: string | null
  active: boolean
  userCount: number
  createdAt: string
}

export async function listSites(includeInactive = false): Promise<SiteSummary[]> {
  const { data } = await api.get<SiteSummary[]>('/admin/sites', { params: { includeInactive } })
  return data
}
export async function createSite(body: { code: string; name: string; address?: string }): Promise<SiteSummary> {
  const { data } = await api.post<SiteSummary>('/admin/sites', body)
  return data
}
export async function updateSite(
  id: number,
  body: { name: string; address?: string; active: boolean },
): Promise<SiteSummary> {
  const { data } = await api.put<SiteSummary>(`/admin/sites/${id}`, body)
  return data
}

// ── Roles ────────────────────────────────────────────────────────────────────
export interface ModuleInfo {
  code: string
  name: string
  description: string | null
  sortOrder: number
}
export interface RoleDetail {
  id: number
  code: string
  name: string
  description: string | null
  builtIn: boolean
  wildcard: boolean
  moduleCodes: string[]
}

export async function listRoles(): Promise<RoleDetail[]> {
  const { data } = await api.get<RoleDetail[]>('/admin/roles')
  return data
}
export async function listModules(): Promise<ModuleInfo[]> {
  const { data } = await api.get<ModuleInfo[]>('/admin/roles/_meta/modules')
  return data
}
export async function createRole(body: {
  code: string
  name: string
  description?: string
  moduleCodes: string[]
}): Promise<RoleDetail> {
  const { data } = await api.post<RoleDetail>('/admin/roles', body)
  return data
}
export async function updateRole(
  id: number,
  body: { name: string; description?: string; moduleCodes: string[] },
): Promise<RoleDetail> {
  const { data } = await api.put<RoleDetail>(`/admin/roles/${id}`, body)
  return data
}
export async function deleteRole(id: number): Promise<void> {
  await api.delete(`/admin/roles/${id}`)
}

// ── Users ────────────────────────────────────────────────────────────────────
export interface RoleRef { id: number; code: string; name: string; description: string | null }
export interface SiteRef { id: number; code: string; name: string }
export interface UserSummary {
  id: number
  username: string
  fullName: string
  email: string | null
  active: boolean
  locked: boolean
  mustChangePassword: boolean
  roleCodes: string[]
  siteCodes: string[]
  lastLoginAt: string | null
  createdAt: string
}
export interface UserDetail {
  id: number
  username: string
  fullName: string
  email: string | null
  active: boolean
  locked: boolean
  mustChangePassword: boolean
  roles: RoleRef[]
  sites: SiteRef[]
}

export async function listUsers(includeInactive = false): Promise<UserSummary[]> {
  const { data } = await api.get<UserSummary[]>('/admin/users', { params: { includeInactive } })
  return data
}
export async function getUser(id: number): Promise<UserDetail> {
  const { data } = await api.get<UserDetail>(`/admin/users/${id}`)
  return data
}
export async function createUser(body: {
  username: string
  fullName: string
  email?: string
  initialPassword: string
  forceChangeOnFirstLogin?: boolean
  roleCodes: string[]
  siteCodes: string[]
}): Promise<UserDetail> {
  const { data } = await api.post<UserDetail>('/admin/users', body)
  return data
}
export async function updateUser(
  id: number,
  body: { fullName: string; email?: string; active: boolean; roleCodes: string[]; siteCodes: string[] },
): Promise<UserDetail> {
  const { data } = await api.put<UserDetail>(`/admin/users/${id}`, body)
  return data
}
export async function resetUserPassword(
  id: number,
  body: { newPassword: string; forceChangeOnNextLogin?: boolean },
): Promise<void> {
  await api.post(`/admin/users/${id}/reset-password`, body)
}
export async function listUserRoles(): Promise<RoleRef[]> {
  const { data } = await api.get<RoleRef[]>('/admin/users/_meta/roles')
  return data
}
export async function listUserSites(): Promise<SiteRef[]> {
  const { data } = await api.get<SiteRef[]>('/admin/users/_meta/sites')
  return data
}
