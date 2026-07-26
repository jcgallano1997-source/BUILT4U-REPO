import api from '@/lib/api'

// ── Categories ───────────────────────────────────────────────────────────────
export interface Category { id: number; name: string; active: boolean }
export async function listCategories(includeInactive = false): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/categories', { params: { includeInactive } })
  return data
}
export async function createCategory(name: string): Promise<Category> {
  const { data } = await api.post<Category>('/categories', { name })
  return data
}
export async function updateCategory(id: number, body: { name: string; active: boolean }): Promise<Category> {
  const { data } = await api.put<Category>(`/categories/${id}`, body)
  return data
}
export async function deleteCategory(id: number): Promise<void> {
  await api.delete(`/categories/${id}`)
}

// ── Locations ────────────────────────────────────────────────────────────────
export interface Location { id: number; name: string; capacity: number | null; active: boolean }
export async function listLocations(includeInactive = false): Promise<Location[]> {
  const { data } = await api.get<Location[]>('/locations', { params: { includeInactive } })
  return data
}
export async function createLocation(body: { name: string; capacity?: number }): Promise<Location> {
  const { data } = await api.post<Location>('/locations', body)
  return data
}
export async function updateLocation(
  id: number,
  body: { name: string; capacity?: number; active: boolean },
): Promise<Location> {
  const { data } = await api.put<Location>(`/locations/${id}`, body)
  return data
}
export async function deleteLocation(id: number): Promise<void> {
  await api.delete(`/locations/${id}`)
}

// ── Units of measure ─────────────────────────────────────────────────────────
export interface Uom { uom: string; active: boolean }
export async function listUoms(includeInactive = false): Promise<Uom[]> {
  const { data } = await api.get<Uom[]>('/uoms', { params: { includeInactive } })
  return data
}
export async function createUom(uom: string): Promise<Uom> {
  const { data } = await api.post<Uom>('/uoms', { uom })
  return data
}
export async function setUomActive(uom: string, active: boolean): Promise<Uom> {
  const { data } = await api.put<Uom>(`/uoms/${encodeURIComponent(uom)}`, { active })
  return data
}
export async function deleteUom(uom: string): Promise<void> {
  await api.delete(`/uoms/${encodeURIComponent(uom)}`)
}

// ── Inventory items ──────────────────────────────────────────────────────────
export type StockLevel = 'OK' | 'WARNING' | 'CRITICAL'
export interface Item {
  id: number
  code: string
  name: string
  description: string | null
  catId: number
  categoryName: string | null
  locId: number
  locationName: string | null
  uom: string
  quantity: number
  sellingPrice: number
  costPrice: number | null
  warning: number | null
  critical: number | null
  barcodeId: number | null
  active: boolean
  stockLevel: StockLevel
}
export interface ItemPayload {
  code: string
  name: string
  description?: string
  catId: number
  locId: number
  uom: string
  quantity: number
  sellingPrice: number
  costPrice?: number
  warning?: number
  critical?: number
  barcodeId?: number
  active?: boolean
}

export async function listItems(params: {
  search?: string
  catId?: number
  locId?: number
  includeInactive?: boolean
  stockLevel?: StockLevel | ''
}): Promise<Item[]> {
  const { data } = await api.get<Item[]>('/items', { params })
  return data
}
export async function createItem(body: ItemPayload): Promise<Item> {
  const { data } = await api.post<Item>('/items', body)
  return data
}
export async function updateItem(id: number, body: ItemPayload): Promise<Item> {
  const { data } = await api.put<Item>(`/items/${id}`, body)
  return data
}
export async function deleteItem(id: number): Promise<void> {
  await api.delete(`/items/${id}`)
}
export async function adjustStock(id: number, body: { delta: number; reason: string }): Promise<Item> {
  const { data } = await api.post<Item>(`/items/${id}/adjust`, body)
  return data
}
