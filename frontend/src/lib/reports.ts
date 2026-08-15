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

export interface SalesDetailLine {
  date: string
  salesNumber: string
  customer: string
  mode: string
  item: string
  category: string | null
  qty: number
  uom: string | null
  unitPrice: number
  lineDiscount: number
  lineTotal: number
  unitCogs: number
  lineCogs: number
  margin: number
}
export interface SalesDetailed {
  from: string
  to: string
  saleCount: number
  lineCount: number
  totalQty: number
  totalAmount: number
  totalCogs: number
  totalMargin: number
  lines: SalesDetailLine[]
}
export async function getSalesDetailed(from: string, to: string): Promise<SalesDetailed> {
  const { data } = await api.get<SalesDetailed>('/reports/sales-detailed', { params: { from, to } })
  return data
}

/** Fetch a report's JSON (no format) for on-screen preview. */
export async function fetchReportJson<T = unknown>(report: string, params: Record<string, string> = {}): Promise<T> {
  const { data } = await api.get<T>(`/reports/${report}`, { params })
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

/** Download the ready-to-fill inventory import template (.xlsx). */
export async function downloadImportTemplate(): Promise<void> {
  const res = await api.get('/items/import/template', { responseType: 'blob' })
  const url = URL.createObjectURL(new Blob([res.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = 'inventory-import-template.xlsx'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
