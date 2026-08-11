'use client'

import { Row, Col, Card, Statistic, theme } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, MinusOutlined, DollarOutlined, ShoppingCartOutlined, InboxOutlined, TeamOutlined } from '@ant-design/icons'
import type { KPIData } from '../types'

const iconMap: Record<string, React.ReactNode> = {
  DollarOutlined: <DollarOutlined />,
  ShoppingCartOutlined: <ShoppingCartOutlined />,
  InboxOutlined: <InboxOutlined />,
  TeamOutlined: <TeamOutlined />,
}

interface KPICardsProps {
  kpis: KPIData[]
  loading?: boolean
}

export default function KPICards({ kpis, loading }: KPICardsProps) {
  const { token } = theme.useToken()

  return (
    <Row gutter={[16, 16]}>
      {kpis.map((kpi, index) => {
        const trendIcon =
          kpi.trend?.direction === 'up' ? (
            <ArrowUpOutlined />
          ) : kpi.trend?.direction === 'down' ? (
            <ArrowDownOutlined />
          ) : (
            <MinusOutlined />
          )
        const trendColor =
          kpi.trend?.direction === 'up'
            ? token.colorSuccess
            : kpi.trend?.direction === 'down'
              ? token.colorError
              : token.colorTextTertiary

        return (
          <Col key={index} xs={24} sm={12} lg={6}>
            <Card loading={loading} styles={{ body: { display: 'flex', gap: 16, alignItems: 'center' } }}>
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: token.borderRadiusLG,
                  background: token.colorPrimaryBg,
                  color: token.colorPrimary,
                  fontSize: 22,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                {iconMap[kpi.icon]}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: token.fontSizeSM, color: token.colorTextSecondary, marginBottom: 4 }}>
                  {kpi.title}
                </div>
                <Statistic value={kpi.value} valueStyle={{ fontSize: token.fontSizeHeading3, fontWeight: 600 }} />
                {kpi.trend && (
                  <div style={{ fontSize: 12, color: trendColor, marginTop: 4 }}>
                    {trendIcon}{' '}
                    {kpi.trend.direction === 'up' ? '上升 ' : kpi.trend.direction === 'down' ? '下降 ' : '持平 '}
                    {kpi.trend.value}% {kpi.trend.period}
                  </div>
                )}
              </div>
            </Card>
          </Col>
        )
      })}
    </Row>
  )
}