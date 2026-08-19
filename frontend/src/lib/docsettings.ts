import api from '@/lib/api'
import type { AxiosError } from 'axios'
import * as agent from '@/lib/printagent'

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
  // Logo + report-PDF page setup
  hasLogo: boolean
  logoMime: string | null
  logoPosition: string
  showLogoPdf: boolean
  paperSize: string
  orientation: string
  marginPreset: string
  fontScale: string
  zebraStriping: boolean
  showPageNumbers: boolean
  showTimestamp: boolean
  showPrintedBy: boolean
  // Receipt customization
  showLogoReceipt: boolean
  receiptHeaderNote: string | null
  receiptShowCashier: boolean
  receiptShowCustomer: boolean
  receiptShowVoucher: boolean
  receiptFormat: string
  // Network printer + drawer
  receiptPrinterHost: string | null
  receiptPrinterPort: number
  receiptPrinterEnabled: boolean
  openDrawerOnSale: boolean
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
  logoPosition?: string
  showLogoPdf?: boolean
  paperSize?: string
  orientation?: string
  marginPreset?: string
  fontScale?: string
  zebraStriping?: boolean
  showPageNumbers?: boolean
  showTimestamp?: boolean
  showPrintedBy?: boolean
  showLogoReceipt?: boolean
  receiptHeaderNote?: string
  receiptShowCashier?: boolean
  receiptShowCustomer?: boolean
  receiptShowVoucher?: boolean
  receiptFormat?: string
  // Network printer + drawer
  receiptPrinterHost?: string
  receiptPrinterPort?: number
  receiptPrinterEnabled?: boolean
  openDrawerOnSale?: boolean
}

export async function getDocSettings(): Promise<DocSettings> {
  const { data } = await api.get<DocSettings>('/admin/doc-settings')
  return data
}
/** Business identity + logo placement (module DOC_SETTINGS). */
export async function saveDocIdentity(body: SaveDocSettings): Promise<DocSettings> {
  const { data } = await api.put<DocSettings>('/admin/doc-settings/identity', body)
  return data
}
/** Report-PDF layout (module PDF_CONFIG). */
export async function saveDocPdf(body: SaveDocSettings): Promise<DocSettings> {
  const { data } = await api.put<DocSettings>('/admin/doc-settings/pdf', body)
  return data
}
/** Sale-receipt customization (module RECEIPT_CONFIG). */
export async function saveDocReceipt(body: SaveDocSettings): Promise<DocSettings> {
  const { data } = await api.put<DocSettings>('/admin/doc-settings/receipt', body)
  return data
}

/** Upload a logo (PNG/JPEG, ≤512 KB) used on report PDFs and receipts. */
export async function uploadDocLogo(file: File): Promise<DocSettings> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<DocSettings>('/admin/doc-settings/logo', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
export async function deleteDocLogo(): Promise<DocSettings> {
  const { data } = await api.delete<DocSettings>('/admin/doc-settings/logo')
  return data
}
/** Cache-busting URL for the current logo (auth cookie/header applied by <img> via api base is N/A — use fetch). */
export async function fetchDocLogoObjectUrl(): Promise<string | null> {
  try {
    const res = await api.get('/admin/doc-settings/logo', { responseType: 'blob' })
    return URL.createObjectURL(res.data as Blob)
  } catch {
    return null
  }
}

// Printer actions go through the print-agent helper: it relays via a local agent
// when one is running (needed when the backend is hosted off the shop's LAN) and
// otherwise prints straight from the server, exactly as before.

/** Print a sale receipt to the site's network thermal printer (opens the drawer if configured). */
export async function printSaleReceipt(salesNumber: string): Promise<void> {
  await agent.printReceipt(salesNumber)
}
/** Print a test slip + kick the drawer to confirm the printer is reachable. */
export async function testPrinter(): Promise<void> {
  await agent.printTest()
}
/** Open the cash drawer without printing. */
export async function openDrawer(): Promise<void> {
  await agent.openDrawer()
}

/** Fetch a sale's receipt PDF (with auth) and open it in a new tab. */
export async function openReceipt(salesNumber: string): Promise<void> {
  const res = await api.get(`/sales/${salesNumber}/receipt`, { responseType: 'blob' })
  const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
  window.open(url, '_blank')
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
