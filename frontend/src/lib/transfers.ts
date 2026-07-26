import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const xferErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export type TransferStatus = 'IN_TRANSIT' | 'RECEIVED' | 'CANCELLED'
export interface Page<T> { content: T[]; totalElements: number; number: number; totalPages: number }

export interface TransferSummary {
  id: number
  transferNumber: string
  sourceSiteId: number
  sourceSiteName: string
  destSiteId: number
  destSiteName: string
  status: TransferStatus
  remarks: string | null
  shippedAt: string
  sentBy: string
  receivedAt: string | null
  receivedBy: string | null
  cancelledAt: string | null
  cancelledBy: string | null
  lineCount: number
}
export interface TransferLine {
  id: number
  sourceItemId: number
  itemCode: string
  itemName: string | null
  uom: string | null
  quantity: number
  unitCost: number
  lineTotal: number
}
export interface TransferDetail { header: TransferSummary; items: TransferLine[] }
export interface SiteOption { id: number; code: string; name: string }
export interface CreateTransfer { destSiteId: number; remarks?: string; lines: { itemId: number; quantity: number }[] }

export async function listTransfers(params?: { status?: TransferStatus | ''; direction?: 'OUTBOUND' | 'INBOUND' | ''; search?: string }): Promise<Page<TransferSummary>> {
  const { data } = await api.get<Page<TransferSummary>>('/stock-transfers', {
    params: { status: params?.status || undefined, direction: params?.direction || undefined, search: params?.search || undefined },
  })
  return data
}
export async function getTransfer(transferNumber: string): Promise<TransferDetail> {
  const { data } = await api.get<TransferDetail>(`/stock-transfers/${transferNumber}`)
  return data
}
export async function listDestinations(): Promise<SiteOption[]> {
  const { data } = await api.get<SiteOption[]>('/stock-transfers/destinations')
  return data
}
export async function shipTransfer(body: CreateTransfer): Promise<TransferDetail> {
  const { data } = await api.post<TransferDetail>('/stock-transfers', body)
  return data
}
export async function receiveTransfer(transferNumber: string): Promise<TransferDetail> {
  const { data } = await api.post<TransferDetail>(`/stock-transfers/${transferNumber}/receive`, {})
  return data
}
export async function cancelTransfer(transferNumber: string): Promise<TransferDetail> {
  const { data } = await api.post<TransferDetail>(`/stock-transfers/${transferNumber}/cancel`, {})
  return data
}

// ── Policy (admin) ───────────────────────────────────────────────────────────
export interface PolicyRule {
  id: number
  sourceSiteId: number
  sourceSiteCode: string
  sourceSiteName: string
  destSiteId: number
  destSiteCode: string
  destSiteName: string
}
export interface PolicyState { enforced: boolean; rules: PolicyRule[] }

export async function getPolicy(): Promise<PolicyState> {
  const { data } = await api.get<PolicyState>('/stock-transfer-policy')
  return data
}
export async function addPolicyRule(sourceSiteId: number, destSiteId: number): Promise<PolicyRule> {
  const { data } = await api.post<PolicyRule>('/stock-transfer-policy', { sourceSiteId, destSiteId })
  return data
}
export async function deletePolicyRule(id: number): Promise<void> {
  await api.delete(`/stock-transfer-policy/${id}`)
}
