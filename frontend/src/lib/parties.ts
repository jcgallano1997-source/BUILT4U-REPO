import api from '@/lib/api'

// ── Customers ────────────────────────────────────────────────────────────────
export interface Customer {
  id: number
  name: string
  contact: string | null
  address: string | null
  email: string | null
  points: number
  creditLimit: number
  active: boolean
}
export async function listCustomers(search?: string, includeInactive = false): Promise<Customer[]> {
  const { data } = await api.get<Customer[]>('/customers', { params: { search: search || undefined, includeInactive } })
  return data
}
export async function createCustomer(body: { name: string; contact?: string; address?: string; email?: string; creditLimit?: number }): Promise<Customer> {
  const { data } = await api.post<Customer>('/customers', body)
  return data
}
export async function updateCustomer(
  id: number,
  body: { name: string; contact?: string; address?: string; email?: string; points: number; creditLimit: number; active: boolean },
): Promise<Customer> {
  const { data } = await api.put<Customer>(`/customers/${id}`, body)
  return data
}
export async function deleteCustomer(id: number): Promise<void> { await api.delete(`/customers/${id}`) }

// ── Suppliers ────────────────────────────────────────────────────────────────
export interface Supplier {
  id: number
  code: string
  name: string
  address: string | null
  agentName: string | null
  contact: string | null
  apEnabled: boolean
  payableDays: number
  active: boolean
}
export async function listSuppliers(search?: string, includeInactive = false): Promise<Supplier[]> {
  const { data } = await api.get<Supplier[]>('/suppliers', { params: { search: search || undefined, includeInactive } })
  return data
}
export async function createSupplier(body: { code: string; name: string; address?: string; agentName?: string; contact?: string; apEnabled: boolean; payableDays: number }): Promise<Supplier> {
  const { data } = await api.post<Supplier>('/suppliers', body)
  return data
}
export async function updateSupplier(
  id: number,
  body: { code: string; name: string; address?: string; agentName?: string; contact?: string; apEnabled: boolean; payableDays: number; active: boolean },
): Promise<Supplier> {
  const { data } = await api.put<Supplier>(`/suppliers/${id}`, body)
  return data
}
export async function deleteSupplier(id: number): Promise<void> { await api.delete(`/suppliers/${id}`) }

// ── Payment modes ────────────────────────────────────────────────────────────
export interface PaymentMode {
  id: number
  code: string
  label: string
  surchargeType: 'NONE' | 'PERCENT' | 'FIXED'
  surchargeValue: number
  isCash: boolean
  allowsPartial: boolean
  customerRequired: boolean
  accountsReceivable: boolean
  arDueDays: number
  sortOrder: number
  active: boolean
}
export interface SavePaymentMode {
  code: string
  label: string
  surchargeType: 'NONE' | 'PERCENT' | 'FIXED'
  surchargeValue: number
  isCash: boolean
  allowsPartial: boolean
  customerRequired: boolean
  accountsReceivable: boolean
  arDueDays: number
  sortOrder: number
  active: boolean
}
/** POS-facing active modes for the current site. */
export async function listActivePaymentModes(): Promise<PaymentMode[]> {
  const { data } = await api.get<PaymentMode[]>('/payment-modes')
  return data
}
export async function listPaymentModes(): Promise<PaymentMode[]> {
  const { data } = await api.get<PaymentMode[]>('/admin/payment-modes')
  return data
}
export async function createPaymentMode(body: SavePaymentMode): Promise<PaymentMode> {
  const { data } = await api.post<PaymentMode>('/admin/payment-modes', body)
  return data
}
export async function updatePaymentMode(id: number, body: SavePaymentMode): Promise<PaymentMode> {
  const { data } = await api.put<PaymentMode>(`/admin/payment-modes/${id}`, body)
  return data
}
export async function deletePaymentMode(id: number): Promise<void> { await api.delete(`/admin/payment-modes/${id}`) }
