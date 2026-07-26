import api from '@/lib/api'
import type { AxiosError } from 'axios'

export function posErr(e: unknown, fallback: string): string {
  return (e as AxiosError<{ message?: string }>).response?.data?.message ?? fallback
}

// ── Shifts ───────────────────────────────────────────────────────────────────
export interface Shift {
  shiftNumber: string
  cashier: string
  status: 'OPEN' | 'CLOSED'
  openingFloat: number
  openedAt: string
  closedAt: string | null
  closedBy: string | null
  cashSalesTotal: number
  cashRefundsTotal: number
  expectedCash: number
  countedCash: number | null
  cashVariance: number | null
  gcashTotal: number
  paymayaTotal: number
  bankTransferTotal: number
  chequeTotal: number
  chargeTotal: number
  saleCount: number
  closeNote: string | null
}
export interface ShiftSummary {
  shiftNumber: string
  cashier: string
  status: 'OPEN' | 'CLOSED'
  openingFloat: number
  expectedCash: number
  countedCash: number | null
  cashVariance: number | null
  openedAt: string
  closedAt: string | null
  saleCount: number
}

export async function getCurrentShift(): Promise<Shift | null> {
  try {
    const { data } = await api.get<Shift>('/shifts/current')
    return data
  } catch (e) {
    if ((e as AxiosError).response?.status === 404) return null
    throw e
  }
}
export async function openShift(openingFloat: number): Promise<Shift> {
  const { data } = await api.post<Shift>('/shifts/open', { openingFloat })
  return data
}
export async function closeShift(shiftNumber: string, body: { countedCash: number; closeNote?: string }): Promise<Shift> {
  const { data } = await api.post<Shift>(`/shifts/${encodeURIComponent(shiftNumber)}/close`, body)
  return data
}
export async function listMyShifts(): Promise<ShiftSummary[]> {
  const { data } = await api.get<ShiftSummary[]>('/shifts/mine')
  return data
}

// ── Sales ────────────────────────────────────────────────────────────────────
export type SaleStatus = 'COMPLETED' | 'VOIDED' | 'REFUNDED'
export interface SaleLine {
  itemId: number
  itemName: string
  uom: string | null
  quantity: number
  adjustment: number
  unitCost: number
  subTotal: number
  refundedQuantity: number
  refundableQuantity: number
}
export interface Sale {
  salesNumber: string
  total: number
  discountAll: number
  totalDiscItem: number
  grandTotal: number
  payment: number
  change: number
  modeOfPayment: string
  customerId: number | null
  customerName: string | null
  reference: string | null
  status: SaleStatus
  reprintCount: number
  creationDate: string
  createdBy: string
  lines: SaleLine[]
}
export interface SaleSummary {
  salesNumber: string
  grandTotal: number
  modeOfPayment: string
  customerName: string | null
  lineCount: number
  status: SaleStatus
  creationDate: string
  createdBy: string
}
export interface CheckoutPayload {
  customerId?: number
  modeOfPayment: string
  payment: number
  discountAll?: number
  reference?: string
  lines: { itemId: number; quantity: number; adjustment?: number }[]
}
export interface ReturnResult {
  returnNumber: string
  salesNumber: string
  totalRefunded: number
}

export async function checkout(body: CheckoutPayload): Promise<Sale> {
  const { data } = await api.post<Sale>('/sales', body)
  return data
}
export async function listSales(status?: SaleStatus | ''): Promise<SaleSummary[]> {
  const { data } = await api.get<SaleSummary[]>('/sales', { params: status ? { status } : {} })
  return data
}
export async function getSale(salesNumber: string): Promise<Sale> {
  const { data } = await api.get<Sale>(`/sales/${encodeURIComponent(salesNumber)}`)
  return data
}
export async function voidSale(salesNumber: string): Promise<Sale> {
  const { data } = await api.post<Sale>(`/sales/${encodeURIComponent(salesNumber)}/void`, {})
  return data
}
export async function refundSale(
  salesNumber: string,
  body: { reason?: string; lines: { itemId: number; quantity: number }[] },
): Promise<ReturnResult> {
  const { data } = await api.post<ReturnResult>(`/sales/${encodeURIComponent(salesNumber)}/refund`, body)
  return data
}

export const PAYMENT_MODES = ['CASH', 'GCASH', 'PAYMAYA', 'CARD', 'BANK TRANSFER', 'CHEQUE'] as const
export const peso = (n: number) => `₱${Number(n).toFixed(2)}`
