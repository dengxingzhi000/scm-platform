'use client'

import dynamic from 'next/dynamic'
import { Card, theme } from 'antd'
import type { OrderStatusData } from '../types'

const ReactECharts = dynamic(() => import('echarts-for-react'), { ssr: false })

interface OrderStatusChartProps {
  data: OrderStatusData[]
  loading?: boolean
}

export default function OrderStatusChart({ data, loading }: OrderStatusChartProps) {
  const { token } = theme.useToken()

  const option = {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: token.colorBgElevated,
      borderColor: token.colorBorderSecondary,
      textStyle: { color: token.colorText },
    },
    legend: {
      bottom: 0,
      data: data.map((d) => d.status),
      textStyle: { color: token.colorTextSecondary },
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: token.colorBgContainer,
          borderWidth: 2,
        },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: data.map((d) => ({ name: d.status, value: d.count, itemStyle: { color: d.color } })),
      },
    ],
  }

  return (
    <Card title="订单状态分布" loading={loading}>
      <ReactECharts option={option} style={{ height: 300 }} />
    </Card>
  )
}