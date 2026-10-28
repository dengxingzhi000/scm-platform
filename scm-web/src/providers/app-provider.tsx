'use client'

import { ReactNode, useEffect } from 'react'
import QueryProvider from '@/components/providers/QueryProvider'
import ThemeProvider from './theme-provider'
import AuthProvider from './auth-provider'
import { useTenantStore } from '@/stores/tenant-store'
import { useUIStore } from '@/stores/ui-store'

interface AppProviderProps {
  children: ReactNode
}

function StoreHydrator() {
  useEffect(() => {
    // Both stores skip hydration so SSR and the first client render agree.
    void useTenantStore.persist.rehydrate()
    void useUIStore.persist.rehydrate()
  }, [])

  return null
}

export default function AppProvider({ children }: AppProviderProps) {
  return (
    <QueryProvider>
      <ThemeProvider>
        <AuthProvider>
          <StoreHydrator />
          {children}
        </AuthProvider>
      </ThemeProvider>
    </QueryProvider>
  )
}
