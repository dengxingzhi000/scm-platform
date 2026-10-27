'use client'
import { Alert, Card, List, Tag, Progress, Button, Empty } from 'antd'
import { WarningOutlined, ReloadOutlined } from '@ant-design/icons'
import { useStockAlerts } from '../hooks'
import type { StockAlert } from '../types'

const statusConfig: Record<'low' | 'out', { color: string; label: string }> = {
  low: { color: 'warning', label: '偏低' },
  out: { color: 'default', label: '缺货' },
}

export default function StockAlerts() {
  const { data: alerts, isLoading, refetch } = useStockAlerts()

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}><WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />库存预警</h2>
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>刷新</Button>
      </div>
      <Card>
        {/* scm-inventory exposes no alert API, so this list is the LOW_STOCK + OUT_OF_STOCK slice of the stock query. */}
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="预警由库存水位实时计算：后端按 availableStock 与 safetyStock 比较得出偏低 / 缺货，无独立告警记录与通知状态。"
        />
        {!isLoading && (alerts?.length ?? 0) === 0 ? (
          <Empty description="当前没有偏低或缺货的 SKU" />
        ) : (
          <List
            loading={isLoading}
            dataSource={alerts || []}
            locale={{ emptyText: '加载中' }}
            renderItem={(item: StockAlert) => {
              const config = statusConfig[item.status]
              const percent = item.threshold > 0 ? Math.round((item.currentStock / item.threshold) * 100) : 100
              return (
                <List.Item>
                  <List.Item.Meta
                    title={<span style={{ fontFamily: 'monospace', fontSize: 13 }}>{item.skuId} <Tag color={config.color}>{config.label}</Tag></span>}
                    description={<span style={{ fontSize: 12, color: '#8c8c8c' }}>仓库 {item.warehouseId} · 库位 {item.locationCode || '-'} · 可用 {item.currentStock} / 安全 {item.threshold}</span>}
                  />
                  <Progress percent={Math.min(percent, 100)} size="small" status={item.status === 'out' ? 'exception' : 'active'} style={{ width: 100 }} />
                </List.Item>
              )
            }}
          />
        )}
      </Card>
    </div>
  )
}
