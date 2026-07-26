import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const auditErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export interface AuditEntry {
  id: number
  siteId: number | null
  username: string
  occurredAt: string
  entityName: string
  entityId: string | null
  action: 'CREATE' | 'UPDATE' | 'DELETE'
  module: string | null
  reference: string | null
  changes: string | null
}
export interface AuditPage {
  content: AuditEntry[]
  number: number
  totalPages: number
  totalElements: number
}
export interface AuditFilters {
  username?: string
  entity?: string
  action?: string
  from?: string
  to?: string
  q?: string
  page?: number
}

export async function listAudit(f: AuditFilters): Promise<AuditPage> {
  const { data } = await api.get<AuditPage>('/admin/audit-log', {
    params: {
      username: f.username || undefined, entity: f.entity || undefined,
      action: f.action || undefined, from: f.from || undefined, to: f.to || undefined,
      q: f.q || undefined, page: f.page ?? 0, size: 30,
    },
  })
  return data
}

// ── Error log ────────────────────────────────────────────────────────────────
export interface ErrorEntry {
  id: number
  ref: string
  occurredAt: string
  siteCode: string | null
  siteName: string | null
  username: string | null
  httpMethod: string | null
  requestPath: string | null
  exceptionClass: string | null
  message: string | null
  stackTrace: string | null
}
export async function listErrors(limit = 100): Promise<ErrorEntry[]> {
  const { data } = await api.get<ErrorEntry[]>('/admin/error-logs', { params: { limit } })
  return data
}
export async function getError(id: number): Promise<ErrorEntry> {
  const { data } = await api.get<ErrorEntry>(`/admin/error-logs/${id}`)
  return data
}

export async function downloadAudit(format: 'pdf' | 'xlsx', f: AuditFilters): Promise<void> {
  const res = await api.get('/admin/audit-log', {
    params: {
      username: f.username || undefined, entity: f.entity || undefined,
      action: f.action || undefined, from: f.from || undefined, to: f.to || undefined,
      q: f.q || undefined, format,
    },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(new Blob([res.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = `audit-log-${new Date().toISOString().slice(0, 10)}.${format}`
  document.body.appendChild(a); a.click(); a.remove()
  URL.revokeObjectURL(url)
}
