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
        // Profile enrichment is best-effort — a failing userinfo call must not
        // destroy a valid session restored from the persisted tokens.
      } finally {
        setLoading(false)
      }
    }

    initAuth()
  }, [accessToken, setUser, setPermissions, setLoading])

  return <>{children}</>
}
