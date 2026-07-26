import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/store/authStore'

// Local dev: VITE_API_BASE_URL is unset → '/api', proxied to :8083 (vite.config.ts).
// Hosted: set VITE_API_BASE_URL to the backend's public URL at build time.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Attach the access token to every request (except anonymous auth endpoints).
api.interceptors.request.use((config) => {
  const url = config.url ?? ''
  const isAnonymousAuthEndpoint =
    url.includes('/auth/login') ||
    url.includes('/auth/refresh') ||
    url.includes('/auth/sites')

  if (isAnonymousAuthEndpoint) {
    delete config.headers.Authorization
    return config
  }

  const { accessToken } = useAuthStore.getState()
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

// Single-flight refresh: many simultaneous 401s trigger only one refresh call.
let refreshPromise: Promise<string | null> | null = null

async function tryRefresh(): Promise<string | null> {
  const { refreshToken, setSession, clear } = useAuthStore.getState()
  if (!refreshToken) return null

  try {
    // Direct axios call (not the `api` instance) so this request can't itself
    // trigger a recursive 401 → refresh loop.
    const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
    setSession(data)
    return data.accessToken as string
  } catch {
    clear()
    return null
  }
}

api.interceptors.response.use(
  (res) => res,
  async (err: AxiosError) => {
    const status = err.response?.status
    const original = err.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined

    const url = original?.url ?? ''
    const isAuthEndpoint =
      url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout')

    if ((status === 401 || status === 403) && original && !original._retried && !isAuthEndpoint) {
      original._retried = true

      if (!refreshPromise) refreshPromise = tryRefresh()
      const newToken = await refreshPromise
      refreshPromise = null

      if (newToken) {
        original.headers = { ...(original.headers ?? {}), Authorization: `Bearer ${newToken}` }
        return api(original)
      }

      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    return Promise.reject(err)
  },
)

export default api
