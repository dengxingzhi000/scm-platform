import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/useAuthStore'
import { useTenantStore } from '@/stores/tenant-store'
import type { LoginResponse } from './contracts/auth'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8761'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { accessToken } = useAuthStore.getState()
    const { currentTenant } = useTenantStore.getState()

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    if (currentTenant) {
      config.headers['X-Tenant-Id'] = currentTenant.id
      config.headers['X-Tenant-Code'] = currentTenant.code
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

    // 401 — attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
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
        const { refreshToken } = useAuthStore.getState()
        if (!refreshToken) {
          throw new Error('No refresh token')
        }

        const response = await axios.post(
          `${API_BASE_URL}/auth/api/auth/refresh`,
          { refreshToken }
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

        const { login, user } = useAuthStore.getState()
        if (user) {
          login(user, newToken, newRefreshToken ?? refreshToken)
        }

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
