import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface User {
  id: number
  username: string
  fullName: string
  email: string | null
  roles: string[]
  modules: string[]
  mustChangePassword: boolean
  passwordExpired?: boolean
}

export interface Site {
  id: number
  code: string
  name: string
}

export interface SessionData {
  user: User
  site: Site
  accessToken: string
  refreshToken: string
}

interface AuthState {
  user: User | null
  site: Site | null
  accessToken: string | null
  refreshToken: string | null
  setSession: (data: SessionData) => void
  setUser: (user: User) => void
  clear: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      site: null,
      accessToken: null,
      refreshToken: null,
      setSession: (data) =>
        set({
          user: data.user,
          site: data.site,
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        }),
      setUser: (user) => set({ user }),
      clear: () => set({ user: null, site: null, accessToken: null, refreshToken: null }),
    }),
    { name: 'built4u-pos-auth' },
  ),
)
