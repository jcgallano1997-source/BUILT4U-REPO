import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const emailErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export interface ReportEmailConfig {
  reportCode: string
  label: string | null
  recipientEmail: string | null
  subject: string | null
  body: string | null
  updatedBy: string | null
  updatedAt: string | null
}
export interface ReportEmailState {
  enabled: boolean
  configs: ReportEmailConfig[]
}

/** The report codes the backend can email, with friendly labels for the admin screen. */
export const REPORT_CODES: { code: string; label: string }[] = [
  { code: 'sales-overview', label: 'Sales overview' },
  { code: 'sales-detailed', label: 'Sales — detailed' },
  { code: 'inventory-snapshot', label: 'Inventory snapshot' },
  { code: 'inventory-valuation', label: 'Inventory valuation' },
  { code: 'inventory-movement', label: 'Inventory movement' },
  { code: 'shift-history', label: 'Shift history' },
  { code: 'goods-receipts', label: 'Goods receive' },
  { code: 'purchase-orders', label: 'Purchase order' },
  { code: 'stock-transfers', label: 'Stock transfers' },
  { code: 'receivables', label: 'Accounts receivable' },
  { code: 'payables', label: 'Accounts payable' },
]

export async function getReportEmailState(): Promise<ReportEmailState> {
  const { data } = await api.get<ReportEmailState>('/admin/report-email')
  return data
}

export async function saveReportEmailConfig(
  reportCode: string,
  body: { label?: string; recipientEmail?: string; subject?: string; body?: string },
): Promise<ReportEmailConfig> {
  const { data } = await api.put<ReportEmailConfig>(`/admin/report-email/${reportCode}`, body)
  return data
}

/** Email a report (pdf/xlsx) to its configured recipient. Resolves on 204, throws otherwise. */
export async function emailReport(
  report: string,
  format: 'pdf' | 'xlsx',
  params: Record<string, string> = {},
): Promise<void> {
  await api.get(`/reports/${report}`, { params: { ...params, format, email: 'true' } })
}
