import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/useAuthStore'
import { useTenantStore } from '@/stores/tenant-store'
import type { LoginResponse } from './contracts/auth'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8761'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

const DEVICE_ID_STORAGE_KEY = 'scm-device-id'

/**
 * The backend binds each JWT to the device id it saw at login (X-Device-ID) and rejects the
 * token when a later request presents a different value, so it must be generated once and reused.
 */
function resolveDeviceId(): string {
  if (typeof window === 'undefined') return ''
  const stored = window.localStorage.getItem(DEVICE_ID_STORAGE_KEY)
  if (stored) return stored
  const generated = crypto.randomUUID().replace(/-/g, '')
  window.localStorage.setItem(DEVICE_ID_STORAGE_KEY, generated)
  return generated
}

/**
 * On a full page reload the zustand store is empty until `initialize()` runs, but page
 * queries fire before it — the cookie is the only token source that is available
 * synchronously, so requests must fall back to it instead of going out unauthenticated.
 */
function readCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

// Request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { accessToken: storeToken } = useAuthStore.getState()
    const accessToken = storeToken ?? readCookie('access_token')
    const { currentTenant } = useTenantStore.getState()

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    if (currentTenant) {
      config.headers['X-Tenant-Id'] = currentTenant.id
      config.headers['X-Tenant-Code'] = currentTenant.code
    }

    const deviceId = resolveDeviceId()
    if (deviceId) {
      config.headers['X-Device-ID'] = deviceId
    }

    // Request ID for tracing
    config.headers['X-Request-Id'] = crypto.randomUUID()

    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor
let isRefreshing = false
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (error: Error) => void
}> = []

apiClient.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    // 401 — attempt token refresh, except on the auth endpoints themselves: refreshing a
    // rejected login/userinfo call would only fail again and force a logout loop.
    const isAuthEndpoint = originalRequest.url?.startsWith('/auth/') ?? false
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingRequests.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(apiClient(originalRequest))
            },
            reject,
          })
        })
      }

      isRefreshing = true

      try {
        // Same reload race as the access token: fell back to the persisted cookie so a
        // 401 that arrives before `initialize()` can still rotate the session.
        const { refreshToken: storedRefreshToken } = useAuthStore.getState()
        const refreshToken = storedRefreshToken ?? readCookie('refresh_token')
        if (!refreshToken) {
          throw new Error('No refresh token')
        }

        // The auth service requires the tenant (and device) headers on every call,
        // including this one, otherwise it fails with a 500 TenantParseException.
        const { currentTenant } = useTenantStore.getState()
        const deviceId = resolveDeviceId()
        const response = await axios.post(
          `${API_BASE_URL}/auth/api/auth/refresh`,
          { refreshToken },
          {
            headers: {
              ...(currentTenant
                ? {
                    'X-Tenant-Id': currentTenant.id,
                    'X-Tenant-Code': currentTenant.code,
                  }
                : {}),
              ...(deviceId ? { 'X-Device-ID': deviceId } : {}),
            },
          }
        )

        const payload = response.data as {
          code: number
          message?: string
          data?: LoginResponse
        }

        if (payload.code !== 200 || !payload.data?.accessToken) {
          throw new Error(payload.message || 'Token refresh failed')
        }

        const {
          accessToken: newToken,
          refreshToken: newRefreshToken,
        } = payload.data

        // Persist the rotated tokens even when the profile was not restored yet.
        useAuthStore.getState().setTokens(newToken, newRefreshToken ?? refreshToken)

        pendingRequests.forEach(({ resolve }) => resolve(newToken))
        pendingRequests = []

        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        pendingRequests.forEach(({ reject }) =>
          reject(new Error('Token refresh failed'))
        )
        pendingRequests = []

        useAuthStore.getState().logout()
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default apiClient
