'use client'

import { useRouter, usePathname } from 'next/navigation'
import { Layout, Avatar, Dropdown, Space, Tooltip, Button, Breadcrumb } from 'antd'
import {
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  ExpandOutlined,
  SunOutlined,
  MoonOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '@/stores/useAuthStore'
import { useUIStore } from '@/stores/ui-store'
import TenantSwitcher from '@/components/business/tenant-switcher'
import NotificationBell from '@/features/notification/components/notification-bell'
import { getBreadcrumb } from './menu-config'

const { Header: AntHeader } = Layout

export default function AppHeader() {
  const router = useRouter()
  const pathname = usePathname()
  const { user, logout } = useAuthStore()
  const { sidebarCollapsed, toggleSidebar, themeMode, setThemeMode } =
    useUIStore()

  const handleLogout = () => {
    logout()
    router.push('/login')
  }

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
    } else {
      document.exitFullscreen()
    }
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => router.push('/settings/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '系统设置',
      onClick: () => router.push('/settings/preferences'),
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
      danger: true,
    },
  ]

  return (
    <AntHeader
      suppressHydrationWarning
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: 'var(--color-bg-header)',
        borderBottom: '1px solid var(--color-border)',
        height: 'var(--header-height)',
        paddingInline: 20,
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <Button
          type="text"
          aria-label={sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'}
          icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={toggleSidebar}
        />
        <Breadcrumb
          items={getBreadcrumb(pathname).map((item) => ({ title: item.title }))}
          style={{ fontSize: 12 }}
        />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        <TenantSwitcher />

        <Tooltip title="全屏">
          <Button
            type="text"
            aria-label="全屏"
            icon={<ExpandOutlined />}
            onClick={toggleFullscreen}
          />
        </Tooltip>

        <Tooltip title={themeMode === 'dark' ? '浅色模式' : '深色模式'}>
          <Button
            type="text"
            aria-label={themeMode === 'dark' ? '切换为浅色模式' : '切换为深色模式'}
            icon={themeMode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
            onClick={() => setThemeMode(themeMode === 'dark' ? 'light' : 'dark')}
          />
        </Tooltip>

        <NotificationBell />

        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space size={8} style={{ cursor: 'pointer', marginInlineStart: 4 }}>
            <Avatar icon={<UserOutlined />} size={24} />
            <span style={{ fontSize: 13, color: 'var(--color-text)' }}>
              {user?.displayName || user?.username || 'User'}
            </span>
          </Space>
        </Dropdown>
      </div>
    </AntHeader>
  )
}