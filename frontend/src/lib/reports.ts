import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const reportErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export interface SalesOverview {
  from: string
  to: string
  salesCount: number
  gross: number
  lineDiscounts: number
  orderDiscounts: number
  netSales: number
  byMode: { mode: string; count: number; total: number }[]
  byDay: { date: string; count: number; net: number }[]
}

export async function getSalesOverview(from: string, to: string): Promise<SalesOverview> {
  const { data } = await api.get<SalesOverview>('/reports/sales-overview', { params: { from, to } })
  return data
}

/** Download a report as pdf/xlsx and trigger a browser save. */
export async function downloadReport(report: string, format: 'pdf' | 'xlsx', params: Record<string, string> = {}): Promise<void> {
  const res = await api.get(`/reports/${report}`, { params: { ...params, format }, responseType: 'blob' })
  const blob = new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${report}-${new Date().toISOString().slice(0, 10)}.${format}`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

// ── Inventory import ─────────────────────────────────────────────────────────
export interface ImportResult { created: number; updated: number; skipped: number; errors: string[] }
export async function importInventory(file: File): Promise<ImportResult> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<ImportResult>('/items/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
