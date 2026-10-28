import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface Tenant {
  id: string
  name: string
  code: string
}

/**
 * Development default: the backend rejects requests without `X-Tenant-Id`, and there is no
 * tenant picker before login. Set NEXT_PUBLIC_DEFAULT_TENANT_ID/CODE to target a real tenant.
 */
const defaultTenant: Tenant | null = process.env.NEXT_PUBLIC_DEFAULT_TENANT_ID
  ? {
      id: process.env.NEXT_PUBLIC_DEFAULT_TENANT_ID,
      name: process.env.NEXT_PUBLIC_DEFAULT_TENANT_NAME || '默认租户',
      code: process.env.NEXT_PUBLIC_DEFAULT_TENANT_CODE || 'default',
    }
  : null

interface TenantState {
  currentTenant: Tenant | null
  tenantList: Tenant[]
  tenantLoading: boolean
  setTenant: (tenant: Tenant) => void
  setTenantList: (list: Tenant[]) => void
  setTenantLoading: (loading: boolean) => void
  clearTenant: () => void
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
      currentTenant: defaultTenant,
      tenantList: [],
      tenantLoading: false,
      setTenant: (tenant) => set({ currentTenant: tenant }),
      setTenantList: (list) => set({ tenantList: list }),
      setTenantLoading: (loading) => set({ tenantLoading: loading }),
      clearTenant: () =>
        set({ currentTenant: null, tenantList: [], tenantLoading: false }),
    }),
    {
      name: 'scm-tenant',
      // Hydration-safe: the server and the first client render use the same default tenant,
      // then localStorage is applied in an effect (see AppProvider).
      skipHydration: true,
      partialize: (state) => ({ currentTenant: state.currentTenant }),
      // A previously persisted `null` must not shadow the development default.
      merge: (persisted, current) => {
        const stored = persisted as { currentTenant?: Tenant | null } | undefined
        return { ...current, currentTenant: stored?.currentTenant ?? defaultTenant }
      },
    }
  )
)
