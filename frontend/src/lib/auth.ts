import api from '@/lib/api'
import type { SessionData, Site, User } from '@/store/authStore'

export interface LoginPayload {
  username: string
  password: string
  siteCode: string
}

export async function fetchSites(username: string): Promise<Site[]> {
  const { data } = await api.get<Site[]>('/auth/sites', { params: { username } })
  return data
}

export async function login(payload: LoginPayload): Promise<SessionData> {
  const { data } = await api.post<SessionData>('/auth/login', payload)
  return data
}

export async function fetchMe(): Promise<User> {
  const { data } = await api.get<User>('/auth/me')
  return data
}

export async function logout(refreshToken: string | null): Promise<void> {
  await api.post('/auth/logout', { refreshToken: refreshToken ?? '' })
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api.post('/auth/change-password', { currentPassword, newPassword })
}
