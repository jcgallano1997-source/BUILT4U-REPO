import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const arapErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export type LedgerStatus = 'OPEN' | 'PARTIAL' | 'PAID' | 'CANCELLED'

export interface Page<T> { content: T[]; totalElements: number; number: number; totalPages: number }
export interface Payment {
  id: number
  amount: number
  note: string | null
  paidAt: string
  createdBy: string | null
}

// ── Receivables ──────────────────────────────────────────────────────────────
export interface Receivable {
  id: number
  salesNumber: string
  customerId: number
  customerName: string
  modeCode: string
  originalAmount: number
  amountPaid: number
  balance: number
  dueDate: string
  status: LedgerStatus
  overdue: boolean
  closedAt: string | null
  creationDate: string
}
export interface ReceivableDetail { receivable: Receivable; payments: Payment[] }

export async function listReceivables(params?: { status?: LedgerStatus | ''; overdue?: boolean; search?: string }): Promise<Page<Receivable>> {
  const { data } = await api.get<Page<Receivable>>('/receivables', {
    params: { status: params?.status || undefined, overdue: params?.overdue || undefined, search: params?.search || undefined },
  })
  return data
}
export async function getReceivable(id: number): Promise<ReceivableDetail> {
  const { data } = await api.get<ReceivableDetail>(`/receivables/${id}`)
  return data
}
export async function collectReceivable(id: number, amount: number, note?: string): Promise<ReceivableDetail> {
  const { data } = await api.post<ReceivableDetail>(`/receivables/${id}/payments`, { amount, note })
  return data
}

// ── Payables ─────────────────────────────────────────────────────────────────
export type PayableSource = 'PURCHASE' | 'EXPENSE'
export interface Payable {
  id: number
  source: PayableSource
  category: string | null
  poNumber: string | null
  grNumber: string | null
  supplierId: number | null
  payeeName: string
  description: string | null
  originalAmount: number
  amountPaid: number
  balance: number
  dueDate: string
  status: LedgerStatus
  overdue: boolean
  closedAt: string | null
  creationDate: string
}
export interface PayableDetail { payable: Payable; payments: Payment[] }

export async function listPayables(params?: { status?: LedgerStatus | ''; source?: PayableSource | ''; overdue?: boolean; search?: string }): Promise<Page<Payable>> {
  const { data } = await api.get<Page<Payable>>('/payables', {
    params: {
      status: params?.status || undefined, source: params?.source || undefined,
      overdue: params?.overdue || undefined, search: params?.search || undefined,
    },
  })
  return data
}
export async function getPayable(id: number): Promise<PayableDetail> {
  const { data } = await api.get<PayableDetail>(`/payables/${id}`)
  return data
}
export async function createExpense(body: { category?: string; payeeName: string; description?: string; amount: number; dueDate: string }): Promise<Payable> {
  const { data } = await api.post<Payable>('/payables', body)
  return data
}
export async function payPayable(id: number, amount: number, note?: string): Promise<PayableDetail> {
  const { data } = await api.post<PayableDetail>(`/payables/${id}/payments`, { amount, note })
  return data
}
