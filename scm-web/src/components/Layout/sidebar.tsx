'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { Layout, Menu } from 'antd'
import type { MenuProps } from 'antd'
import { useUIStore } from '@/stores/ui-store'
import { appMenuItems, getParentGroupKey, matchMenuKey, stripLocale } from './menu-config'

const { Sider } = Layout

export default function AppSidebar() {
  const router = useRouter()
  const pathname = usePathname()
  const { sidebarCollapsed } = useUIStore()
  const [openKeys, setOpenKeys] = useState<string[]>([])

  const getOpenKeys = () => {
    const groupKey = getParentGroupKey(pathname)
    return groupKey ? [groupKey] : []
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
      width={236}
      collapsedWidth={80}
      /* Dark Reader 等浏览器扩展会在水合前改写内联样式，这里容忍属性差异 */
      suppressHydrationWarning
      style={{
        height: '100vh',
        position: 'sticky',
        top: 0,
        overflow: 'auto',
        borderRight: '1px solid var(--color-border)',
        background: 'var(--color-bg-sider)',
      }}
    >
      <div
        suppressHydrationWarning
        style={{
          height: 'var(--header-height)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: sidebarCollapsed ? 'center' : 'flex-start',
          gap: 12,
          padding: sidebarCollapsed ? 0 : '0 var(--spacing-md)',
          borderBottom: '1px solid var(--color-border)',
          position: 'sticky',
          top: 0,
          background: 'var(--color-bg-sider)',
          zIndex: 1,
        }}
      >
        <div
          suppressHydrationWarning
          style={{
            width: 30,
            height: 30,
            borderRadius: 'var(--rounding-sm)',
            background: 'var(--auth-brand-bg)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--brand-tile-ink)',
            fontFamily: 'var(--font-family-serif)',
            fontWeight: 500,
            fontSize: 17,
            lineHeight: 1,
            flexShrink: 0,
          }}
        >
          S
        </div>
        {!sidebarCollapsed && (
          <div
            suppressHydrationWarning
            style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}
          >
            <span
              suppressHydrationWarning
              style={{
                color: 'var(--color-text)',
                fontFamily: 'var(--font-family-serif)',
                fontWeight: 500,
                fontSize: 17,
                letterSpacing: '-0.011em',
              }}
            >
              SCM Platform
            </span>
            <span
              suppressHydrationWarning
              style={{
                color: 'var(--color-text-tertiary)',
                fontSize: 11,
                letterSpacing: '0.04em',
              }}
            >
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
        style={{ borderInlineEnd: 'none', fontSize: 13, paddingTop: 6 }}
      />
    </Sider>
  )
}