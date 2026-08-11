'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { Layout, Menu } from 'antd'
import type { MenuProps } from 'antd'
import { useUIStore } from '@/stores/ui-store'
import { appMenuItems, matchMenuKey, stripLocale } from './menu-config'

const { Sider } = Layout

export default function AppSidebar() {
  const router = useRouter()
  const pathname = usePathname()
  const { sidebarCollapsed } = useUIStore()
  const [openKeys, setOpenKeys] = useState<string[]>([])

  const getOpenKeys = () => {
    const parts = stripLocale(pathname).split('/').filter(Boolean)
    return parts.length > 1 ? [`/${parts[0]}`] : []
  }

  useEffect(() => {
    if (!sidebarCollapsed) setOpenKeys(getOpenKeys())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname, sidebarCollapsed])

  const handleOpenChange = (keys: string[]) => {
    setOpenKeys(keys.slice(-1))
  }

  return (
    <Sider
      trigger={null}
      collapsible
      collapsed={sidebarCollapsed}
      width={240}
      collapsedWidth={80}
      style={{
        height: '100vh',
        position: 'sticky',
        top: 0,
        overflow: 'auto',
        borderRight: '1px solid var(--color-border-secondary)',
        background: 'var(--color-bg-sider)',
      }}
    >
      <div
        style={{
          height: 'var(--header-height)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 12,
          borderBottom: '1px solid var(--color-border-secondary)',
          position: 'sticky',
          top: 0,
          background: 'var(--color-bg-sider)',
          zIndex: 1,
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 8,
            background: 'var(--auth-brand-bg)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontWeight: 700,
            fontSize: 14,
            flexShrink: 0,
          }}
        >
          S
        </div>
        {!sidebarCollapsed && (
          <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
            <span
              style={{
                color: 'var(--color-primary)',
                fontWeight: 700,
                fontSize: 16,
              }}
            >
              SCM Platform
            </span>
            <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>
              供应链管理中台
            </span>
          </div>
        )}
      </div>
      <Menu
        mode="inline"
        selectedKeys={[matchMenuKey(pathname) ?? stripLocale(pathname)]}
        openKeys={sidebarCollapsed ? undefined : openKeys}
        onOpenChange={handleOpenChange}
        items={appMenuItems}
        onClick={({ key }) => router.push(key)}
        style={{ borderInlineEnd: 'none' }}
      />
    </Sider>
  )
}