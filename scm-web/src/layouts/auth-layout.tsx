'use client'

import { Layout } from 'antd'

const { Content } = Layout

interface AuthLayoutProps {
  children: React.ReactNode
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <Layout
      style={{
        minHeight: '100vh',
        background: 'var(--auth-bg)',
      }}
    >
      <Content
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: 'var(--spacing-lg)',
        }}
      >
        {children}
      </Content>
    </Layout>
  )
}