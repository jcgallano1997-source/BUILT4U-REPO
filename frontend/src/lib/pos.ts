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
  cashIn: number
  cashOut: number
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
export interface DenomCount { denom: number; qty: number }
export async function closeShift(
  shiftNumber: string,
  body: { countedCash: number; closeNote?: string; denominations?: DenomCount[] },
): Promise<Shift> {
  const { data } = await api.post<Shift>(`/shifts/${encodeURIComponent(shiftNumber)}/close`, body)
  return data
}
export async function listMyShifts(): Promise<ShiftSummary[]> {
  const { data } = await api.get<ShiftSummary[]>('/shifts/mine')
  return data
}

// ── Cash movements ─────────────────────────────────────────────────────────
export interface CashMovement {
  movementId: number
  direction: 'IN' | 'OUT'
  amount: number
  reason: string | null
  createdBy: string | null
  creationDate: string
}
export async function recordCashMovement(
  shiftNumber: string,
  body: { direction: 'IN' | 'OUT'; amount: number; reason?: string },
): Promise<Shift> {
  const { data } = await api.post<Shift>(`/shifts/${encodeURIComponent(shiftNumber)}/cash-movement`, body)
  return data
}
export async function listCashMovements(shiftNumber: string): Promise<CashMovement[]> {
  const { data } = await api.get<CashMovement[]>(`/shifts/${encodeURIComponent(shiftNumber)}/cash-movements`)
  return data
}

/** PH peso denominations (bills + coins), high → low. */
export const PH_DENOMS = [1000, 500, 200, 100, 50, 20, 10, 5, 1, 0.25] as const

// ── Sales ────────────────────────────────────────────────────────────────────
export type SaleStatus = 'COMPLETED' | 'VOIDED' | 'REFUNDED'
export interface SaleLine {
  itemId: number
  itemName: string
  uom: string | null
  quantity: number
  adjustment: number
  unitCost: number
  listPrice: number | null
  overrideReason: string | null
  approvedBy: string | null
  subTotal: number
  refundedQuantity: number
  refundableQuantity: number
}
export interface PaymentLine {
  mode: string
  amount: number
  tendered: number
  change: number
  reference: string | null
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
  payments: PaymentLine[]
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
export interface Tender {
  mode: string
  amount: number
  reference?: string
}
export interface CheckoutPayload {
  customerId?: number
  modeOfPayment: string
  payment: number
  discountAll?: number
  voucherCode?: string
  reference?: string
  /** Optional split/multiple tender (paid methods only). Overrides modeOfPayment/payment when set. */
  payments?: Tender[]
  /** Manager credentials to approve a price override / line discount when the cashier lacks PRICE_OVERRIDE. */
  approvalUser?: string
  approvalPassword?: string
  lines: { itemId: number; quantity: number; adjustment?: number; unitPrice?: number; overrideReason?: string }[]
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

// ── Held (parked) sales ──────────────────────────────────────────────────────
export interface HeldSaleSummary {
  heldId: number
  label: string | null
  customerName: string | null
  itemCount: number
  totalAmount: number
  createdBy: string
  creationDate: string
}
export interface HeldSaleDetail extends HeldSaleSummary {
  customerId: number | null
  cartJson: string
}
export interface SaveHeldSalePayload {
  label?: string
  customerId?: number
  customerName?: string
  itemCount: number
  totalAmount: number
  cartJson: string
}
export async function listHeldSales(): Promise<HeldSaleSummary[]> {
  const { data } = await api.get<HeldSaleSummary[]>('/held-sales')
  return data
}
export async function getHeldSale(id: number): Promise<HeldSaleDetail> {
  const { data } = await api.get<HeldSaleDetail>(`/held-sales/${id}`)
  return data
}
export async function saveHeldSale(body: SaveHeldSalePayload): Promise<HeldSaleDetail> {
  const { data } = await api.post<HeldSaleDetail>('/held-sales', body)
  return data
}
export async function deleteHeldSale(id: number): Promise<void> {
  await api.delete(`/held-sales/${id}`)
}

export const PAYMENT_MODES = ['CASH', 'GCASH', 'PAYMAYA', 'CARD', 'BANK TRANSFER', 'CHEQUE'] as const
export const peso = (n: number) => `₱${Number(n).toFixed(2)}`
