'use client'

import { ReactNode, useEffect } from 'react'
import { useAuthStore } from '@/stores/useAuthStore'
import { authService } from '@/lib/api/services/auth'
import { mergeUserInfo } from '@/lib/auth/session'

interface AuthProviderProps {
  children: ReactNode
}

export default function AuthProvider({ children }: AuthProviderProps) {
  const {
    accessToken,
    setUser,
    setPermissions,
    setLoading,
    logout,
    initialize,
  } = useAuthStore()

  useEffect(() => {
    void initialize()
  }, [initialize])

  useEffect(() => {
    const initAuth = async () => {
      if (!accessToken) {
        setLoading(false)
        return
      }

      try {
        const userInfo = await authService.getUserInfo()
        setUser(mergeUserInfo(useAuthStore.getState().user, userInfo))
        setPermissions(userInfo.permissions ?? [])
      } catch {
        logout()
      } finally {
        setLoading(false)
      }
    }

    initAuth()
  }, [accessToken, setUser, setPermissions, setLoading, logout])

  return <>{children}</>
}
