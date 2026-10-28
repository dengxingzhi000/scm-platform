'use client'

import { Typography } from 'antd'
import type { ReactNode } from 'react'

const { Title, Text } = Typography

interface AuthBrandCardProps {
  title: string
  subtitle?: string
  maxWidth?: number
  children: ReactNode
}

/**
 * The auth panel: a flat white sheet with a single hairline, a violet mark and
 * a sans headline. Elevation stays minimal — the page's radial violet bloom
 * does the separating.
 */
export default function AuthBrandCard({
  title,
  subtitle,
  maxWidth = 440,
  children,
}: AuthBrandCardProps) {
  return (
    <div
      suppressHydrationWarning
      style={{
        width: '100%',
        maxWidth,
        background: 'var(--color-bg-container)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--rounding-2xl)',
        boxShadow: 'var(--auth-card-shadow)',
        padding: 'var(--spacing-xxl) var(--spacing-xl)',
      }}
    >
      <div style={{ marginBottom: 'var(--spacing-xl)' }}>
        <div
          suppressHydrationWarning
          style={{
            width: 44,
            height: 44,
            borderRadius: 'var(--rounding-lg)',
            background: 'var(--auth-brand-bg)',
            color: 'var(--brand-tile-ink)',
            fontFamily: 'var(--font-family-serif)',
            fontSize: 24,
            lineHeight: '44px',
            textAlign: 'center',
            marginBottom: 'var(--spacing-lg)',
          }}
        >
          S
        </div>
        <Title level={3} style={{ marginBottom: 6 }}>
          {title}
        </Title>
        {subtitle && (
          <Text suppressHydrationWarning style={{ color: 'var(--color-text-secondary)' }}>
            {subtitle}
          </Text>
        )}
      </div>
      {children}
    </div>
  )
}
