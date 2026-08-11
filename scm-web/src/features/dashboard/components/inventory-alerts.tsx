'use client'

import { Card, List, Tag, Progress, theme } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import type { InventoryAlert } from '../types'

interface InventoryAlertsProps {
  alerts: InventoryAlert[]
  loading?: boolean
}

export default function InventoryAlerts({ alerts, loading }: InventoryAlertsProps) {
  const { token } = theme.useToken()

  const statusConfig = {
    critical: { color: token.colorError, label: '紧急' },
    low: { color: token.colorWarning, label: '偏低' },
    out: { color: token.colorTextTertiary, label: '缺货' },
  } as const

  return (
    <Card
      title="库存预警"
      extra={<WarningOutlined style={{ color: token.colorWarning }} />}
      loading={loading}
    >
      <List
        dataSource={alerts}
        renderItem={(item) => {
          const config = statusConfig[item.status as keyof typeof statusConfig] ?? statusConfig.low
          const percent = Math.round((item.currentStock / item.threshold) * 100)
          return (
            <List.Item>
              <List.Item.Meta
                title={
                  <span>
                    {item.productName} <Tag color={config.color}>{config.label}</Tag>
                  </span>
                }
                description={
                  <span style={{ fontSize: 12, color: token.colorTextSecondary }}>
                    {item.skuCode} · 库存 {item.currentStock} / 阈值 {item.threshold}
                  </span>
                }
              />
              <Progress
                percent={Math.min(percent, 100)}
                size="small"
                status={item.status === 'out' || item.status === 'critical' ? 'exception' : 'active'}
                strokeColor={config.color}
                style={{ width: 100 }}
              />
            </List.Item>
          )
        }}
      />
    </Card>
  )
}