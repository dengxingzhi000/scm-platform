'use client'

import { Card, Typography, theme } from 'antd'
import type { ReactNode } from 'react'

const { Title, Text } = Typography

interface AuthBrandCardProps {
  title: string
  subtitle?: string
  maxWidth?: number
  children: ReactNode
}

export default function AuthBrandCard({
  title,
  subtitle,
  maxWidth = 400,
  children,
}: AuthBrandCardProps) {
  const { token } = theme.useToken()

  return (
    <Card
      style={{
        width: '100%',
        maxWidth,
        boxShadow: 'var(--auth-card-shadow)',
        border: '1px solid var(--color-border-secondary)',
      }}
      styles={{ body: { padding: 'var(--spacing-xl)' } }}
    >
      <div style={{ textAlign: 'center', marginBottom: 'var(--spacing-lg)' }}>
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: token.borderRadiusLG,
            background: 'var(--auth-brand-bg)',
            color: '#fff',
            fontWeight: 700,
            fontSize: 22,
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 16,
          }}
        >
          S
        </div>
        <Title level={3} style={{ marginBottom: 4 }}>
          {title}
        </Title>
        {subtitle && <Text type="secondary">{subtitle}</Text>}
      </div>
      {children}
    </Card>
  )
}