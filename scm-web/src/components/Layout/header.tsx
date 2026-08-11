'use client'

import { useRouter, usePathname } from 'next/navigation'
import { Layout, Avatar, Dropdown, Space, Tooltip, Switch, Button, Breadcrumb } from 'antd'
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
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: 'var(--color-bg-header)',
        borderBottom: '1px solid var(--color-border-secondary)',
        height: 'var(--header-height)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Button
          type="text"
          aria-label={sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'}
          icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={toggleSidebar}
        />
        <Breadcrumb
          items={getBreadcrumb(pathname).map((item) => ({ title: item.title }))}
        />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
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
          <Switch
            checkedChildren={<MoonOutlined />}
            unCheckedChildren={<SunOutlined />}
            checked={themeMode === 'dark'}
            onChange={(checked) => setThemeMode(checked ? 'dark' : 'light')}
            size="small"
          />
        </Tooltip>

        <NotificationBell />

        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space style={{ cursor: 'pointer' }}>
            <Avatar icon={<UserOutlined />} size="small" />
            <span>{user?.displayName || user?.username || 'User'}</span>
          </Space>
        </Dropdown>
      </div>
    </AntHeader>
  )
}