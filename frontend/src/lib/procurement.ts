import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const procErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export type PoStatus = 'DRAFT' | 'APPROVED' | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'CANCELLED'

// ── Purchase orders ──────────────────────────────────────────────────────────
export interface PoSummary {
  poNumber: string
  supplier: string
  deliveryDate: string | null
  status: PoStatus
  grandTotal: number
  lineCount: number
  creationDate: string
  createdBy: string
}
export interface PoLine {
  itemId: number
  itemCode: string | null
  itemName: string | null
  itemDesc: string | null
  uom: string | null
  orderedQty: number
  receivedQty: number
  remainingQty: number
  unitPrice: number
  subTotal: number
}
export interface Po {
  poNumber: string
  supplier: string
  deliveryDate: string | null
  remarks: string | null
  status: PoStatus
  grandTotal: number
  creationDate: string
  createdBy: string
  approvedAt: string | null
  approvedBy: string | null
  autoApproved: boolean
  canCurrentUserApprove: boolean
  lines: PoLine[]
}
export interface CreatePoLine { itemId: number; quantity: number; unitPrice: number }
export interface CreatePo { supplier: string; deliveryDate?: string; remarks?: string; lines: CreatePoLine[] }

export async function listPurchaseOrders(status?: PoStatus | '', supplier?: string): Promise<PoSummary[]> {
  const { data } = await api.get<PoSummary[]>('/purchase-orders', {
    params: { status: status || undefined, supplier: supplier || undefined },
  })
  return data
}
export async function getPurchaseOrder(poNumber: string): Promise<Po> {
  const { data } = await api.get<Po>(`/purchase-orders/${poNumber}`)
  return data
}
export async function createPurchaseOrder(body: CreatePo): Promise<Po> {
  const { data } = await api.post<Po>('/purchase-orders', body)
  return data
}
export async function approvePurchaseOrder(poNumber: string): Promise<Po> {
  const { data } = await api.post<Po>(`/purchase-orders/${poNumber}/approve`, {})
  return data
}
export async function cancelPurchaseOrder(poNumber: string): Promise<Po> {
  const { data } = await api.post<Po>(`/purchase-orders/${poNumber}/cancel`, {})
  return data
}

// ── Goods receipts ───────────────────────────────────────────────────────────
export interface GrLine {
  itemId: number
  itemCode: string | null
  itemName: string | null
  uom: string | null
  quantity: number
  supPrice: number
  subTotal: number
}
export interface RepriceSuggestion {
  itemId: number
  code: string
  name: string
  oldCost: number
  newCost: number
  sellingPrice: number
  suggestedPrice: number
}
export interface GoodsReceipt {
  grNumber: string
  poNumber: string | null
  supplier: string | null
  reference: string | null
  remarks: string | null
  grandTotal: number
  creationDate: string
  createdBy: string
  lines: GrLine[]
  repriceSuggestions?: RepriceSuggestion[]
}
export interface CreateGrLine { itemId: number; quantity: number; unitCost?: number; inPurchaseUnit?: boolean }
export interface CreateGr { poNumber?: string; supplier?: string; reference?: string; remarks?: string; lines: CreateGrLine[] }

export async function listGoodsReceipts(params?: { poNumber?: string; search?: string; source?: 'PO' | 'DIRECT' | '' }): Promise<GoodsReceipt[]> {
  const { data } = await api.get<GoodsReceipt[]>('/goods-receipts', {
    params: { poNumber: params?.poNumber || undefined, search: params?.search || undefined, source: params?.source || undefined },
  })
  return data
}
export async function getGoodsReceipt(grNumber: string): Promise<GoodsReceipt> {
  const { data } = await api.get<GoodsReceipt>(`/goods-receipts/${grNumber}`)
  return data
}
export async function createGoodsReceipt(body: CreateGr): Promise<GoodsReceipt> {
  const { data } = await api.post<GoodsReceipt>('/goods-receipts', body)
  return data
}

// ── PO approvers (admin) ─────────────────────────────────────────────────────
export interface PoApprover {
  userId: number
  username: string
  fullName: string
  approverUserId: number | null
  approverUsername: string | null
  approverFullName: string | null
}
export async function listPoApprovers(): Promise<PoApprover[]> {
  const { data } = await api.get<PoApprover[]>('/po-approvers')
  return data
}
export async function setPoApprover(userId: number, approverUserId: number | null): Promise<void> {
  await api.put(`/po-approvers/${userId}`, { approverUserId })
}
