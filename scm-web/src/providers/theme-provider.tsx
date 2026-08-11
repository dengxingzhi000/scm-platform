'use client'

import { ReactNode, useEffect, useMemo, useState } from 'react'
import { ConfigProvider, App as AntApp } from 'antd'
import { lightTheme, darkTheme } from '@/lib/antd-theme'
import { useUIStore } from '@/stores/ui-store'

interface ThemeProviderProps {
  children: ReactNode
}

export default function ThemeProvider({ children }: ThemeProviderProps) {
  const { themeMode } = useUIStore()
  const [systemDark, setSystemDark] = useState(false)

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const update = () => setSystemDark(media.matches)
    update()
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])

  const isDark = useMemo(() => {
    if (themeMode === 'system') return systemDark
    return themeMode === 'dark'
  }, [themeMode, systemDark])

  useEffect(() => {
    document.documentElement.setAttribute(
      'data-theme',
      isDark ? 'dark' : 'light'
    )
  }, [isDark])

  return (
    <ConfigProvider theme={isDark ? darkTheme : lightTheme}>
      <AntApp>{children}</AntApp>
    </ConfigProvider>
  )
}