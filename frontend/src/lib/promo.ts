import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const promoErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export interface Page<T> { content: T[]; totalElements: number; number: number; totalPages: number }

// ── Vouchers (admin) ─────────────────────────────────────────────────────────
export type DiscountType = 'FIXED' | 'PERCENT'
export interface Voucher {
  id: number
  code: string
  description: string | null
  discountType: DiscountType
  discountValue: number
  maxDiscount: number | null
  minSpend: number | null
  validFrom: string | null
  validTo: string | null
  usageLimit: number | null
  usedCount: number
  active: boolean
}
export interface SaveVoucher {
  code: string
  description?: string
  discountType: DiscountType
  discountValue: number
  maxDiscount?: number
  minSpend?: number
  validFrom?: string
  validTo?: string
  usageLimit?: number
  active: boolean
}
export async function listVouchers(search?: string, includeInactive = false): Promise<Voucher[]> {
  const { data } = await api.get<Page<Voucher>>('/admin/vouchers', { params: { search: search || undefined, includeInactive } })
  return data.content
}
export async function createVoucher(body: SaveVoucher): Promise<Voucher> {
  const { data } = await api.post<Voucher>('/admin/vouchers', body)
  return data
}
export async function updateVoucher(id: number, body: SaveVoucher): Promise<Voucher> {
  const { data } = await api.put<Voucher>(`/admin/vouchers/${id}`, body)
  return data
}
export async function deleteVoucher(id: number): Promise<void> { await api.delete(`/admin/vouchers/${id}`) }

// ── Voucher validate (POS) ───────────────────────────────────────────────────
export interface VoucherEvaluation {
  valid: boolean
  code: string
  reason: string | null
  message: string | null
  description: string | null
  discountType: DiscountType | null
  discountAmount: number
}
export async function validateVoucher(code: string, subtotal: number, customerId?: number): Promise<VoucherEvaluation> {
  const { data } = await api.post<VoucherEvaluation>('/vouchers/validate', { code, subtotal, customerId })
  return data
}

// ── Loyalty config (admin) ───────────────────────────────────────────────────
export interface LoyaltyConfig {
  siteId: number
  usingDefault: boolean
  pointsRate: number
  redeemValue: number
  updatedBy: string | null
  updatedAt: string | null
}
export async function getLoyaltyConfig(): Promise<LoyaltyConfig> {
  const { data } = await api.get<LoyaltyConfig>('/admin/loyalty-config')
  return data
}
export async function saveLoyaltyConfig(pointsRate: number, redeemValue: number): Promise<LoyaltyConfig> {
  const { data } = await api.put<LoyaltyConfig>('/admin/loyalty-config', { pointsRate, redeemValue })
  return data
}

// ── Loyalty rewards ──────────────────────────────────────────────────────────
export type RewardType = 'ITEM' | 'FREETEXT'
export interface Reward {
  id: number
  name: string
  description: string | null
  pointsCost: number
  rewardType: RewardType
  itemId: number | null
  itemName: string | null
  sortOrder: number
  active: boolean
}
export interface SaveReward {
  name: string
  description?: string
  pointsCost: number
  rewardType: RewardType
  itemId?: number
  sortOrder: number
  active: boolean
}
export async function listRewardsAdmin(): Promise<Reward[]> {
  const { data } = await api.get<Reward[]>('/admin/loyalty-rewards')
  return data
}
export async function createReward(body: SaveReward): Promise<Reward> {
  const { data } = await api.post<Reward>('/admin/loyalty-rewards', body)
  return data
}
export async function updateReward(id: number, body: SaveReward): Promise<Reward> {
  const { data } = await api.put<Reward>(`/admin/loyalty-rewards/${id}`, body)
  return data
}
export async function deleteReward(id: number): Promise<void> { await api.delete(`/admin/loyalty-rewards/${id}`) }

// ── Loyalty (cashier / customers screen) ─────────────────────────────────────
export async function listActiveRewards(): Promise<Reward[]> {
  const { data } = await api.get<Reward[]>('/loyalty/rewards')
  return data
}
export interface RedeemResult { rewardName: string; rewardType: RewardType; itemName: string | null; pointsSpent: number; newBalance: number }
export async function redeemReward(customerId: number, rewardId: number, note?: string): Promise<RedeemResult> {
  const { data } = await api.post<RedeemResult>('/loyalty/redeem-reward', { customerId, rewardId, note })
  return data
}
export interface LedgerEntry { id: number; entryType: string; points: number; salesNumber: string | null; note: string | null; creationDate: string }
export interface LedgerView { content: LedgerEntry[]; number: number; totalPages: number; ledgerSum: number; liveBalance: number }
export async function getLedger(customerId: number, page = 0, size = 10): Promise<LedgerView> {
  const { data } = await api.get<LedgerView>('/loyalty/ledger', { params: { customerId, page, size } })
  return data
}
