import { create } from 'zustand'
import { persist } from 'zustand/middleware'

type ThemeMode = 'light' | 'dark' | 'system'
type Density = 'default' | 'compact' | 'loose'

interface UIState {
  sidebarCollapsed: boolean
  themeMode: ThemeMode
  density: Density
  toggleSidebar: () => void
  setSidebarCollapsed: (collapsed: boolean) => void
  setThemeMode: (mode: ThemeMode) => void
  setDensity: (density: Density) => void
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      themeMode: 'light',
      density: 'default',
      toggleSidebar: () =>
        set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
      setThemeMode: (mode) => set({ themeMode: mode }),
      setDensity: (density) => set({ density }),
    }),
    {
      name: 'scm-ui',
      // Server and the first client render must agree on the theme; localStorage is
      // applied in an effect by StoreHydrator (see AppProvider).
      skipHydration: true,
    }
  )
)
