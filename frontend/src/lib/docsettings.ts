import api from '@/lib/api'
import type { AxiosError } from 'axios'

export const docErr = (e: unknown, f: string) =>
  (e as AxiosError<{ message?: string }>).response?.data?.message ?? f

export interface DocSettings {
  siteId: number
  usingDefault: boolean
  businessName: string | null
  addressLine: string | null
  contactLine: string | null
  tin: string | null
  footerNote: string | null
  accentColor: string
  receiptTitle: string
  receiptFooter: string
  updatedBy: string | null
  updatedAt: string | null
}
export interface SaveDocSettings {
  businessName?: string
  addressLine?: string
  contactLine?: string
  tin?: string
  footerNote?: string
  accentColor?: string
  receiptTitle?: string
  receiptFooter?: string
}

export async function getDocSettings(): Promise<DocSettings> {
  const { data } = await api.get<DocSettings>('/admin/doc-settings')
  return data
}
export async function saveDocSettings(body: SaveDocSettings): Promise<DocSettings> {
  const { data } = await api.put<DocSettings>('/admin/doc-settings', body)
  return data
}

/** Fetch a sale's receipt PDF (with auth) and open it in a new tab. */
export async function openReceipt(salesNumber: string): Promise<void> {
  const res = await api.get(`/sales/${salesNumber}/receipt`, { responseType: 'blob' })
  const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
  window.open(url, '_blank')
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
