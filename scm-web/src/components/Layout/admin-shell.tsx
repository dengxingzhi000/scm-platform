'use client'

import { Layout } from 'antd'
import AppHeader from './header'
import AppSidebar from './sidebar'

const { Content } = Layout

interface AdminShellProps {
  children: React.ReactNode
}

export default function AdminShell({ children }: AdminShellProps) {
  return (
    <Layout style={{ minHeight: '100vh', background: 'var(--color-bg-layout)' }}>
      <AppSidebar />
      <Layout style={{ background: 'var(--color-bg-layout)' }}>
        <AppHeader />
        <Content
          style={{
            padding: 'var(--content-padding)',
            minHeight: 'calc(100vh - var(--header-height))',
            background: 'var(--color-bg-layout)',
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  )
}