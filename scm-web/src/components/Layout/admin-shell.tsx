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
    <Layout
      suppressHydrationWarning
      style={{ minHeight: '100vh', background: 'var(--color-bg-layout)' }}
    >
      <AppSidebar />
      <Layout
        suppressHydrationWarning
        style={{ background: 'var(--color-bg-layout)' }}
      >
        <AppHeader />
        <Content
          suppressHydrationWarning
          style={{
            padding: 'var(--content-padding)',
            minHeight: 'calc(100vh - var(--header-height))',
            background: 'var(--color-bg-layout)',
          }}
        >
          {/* Data screens stay readable on ultrawide monitors instead of
              stretching edge to edge. */}
          <div
            style={{
              width: '100%',
              maxWidth: 'var(--content-max-width)',
              margin: '0 auto',
            }}
          >
            {children}
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}