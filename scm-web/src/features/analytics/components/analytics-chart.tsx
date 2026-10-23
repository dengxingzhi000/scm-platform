'use client'

import dynamic from 'next/dynamic'
import { Card, Empty, theme } from 'antd'
import type { DatasetQueryColumn, DatasetQueryResult } from '../types'

const ReactECharts = dynamic(() => import('echarts-for-react'), { ssr: false })

interface AnalyticsChartProps {
  result?: DatasetQueryResult
  dimensions: string[]
  metrics: string[]
  loading?: boolean
}

function labelOf(columns: DatasetQueryColumn[], code: string) {
  return columns.find((column) => column.code === code)?.code ?? code
}

export default function AnalyticsChart({
  result,
  dimensions,
  metrics,
  loading,
}: AnalyticsChartProps) {
  const { token } = theme.useToken()

  if (!result || result.rows.length === 0) {
    return (
      <Card title="趋势图" loading={loading}>
        <Empty description="运行查询后展示图表" />
      </Card>
    )
  }

  const xField = dimensions[0]
  const categories = result.rows.map((row, index) =>
    xField ? String(row[xField] ?? '') : `#${index + 1}`,
  )
  const series = metrics.map((metric) => ({
    name: labelOf(result.columns, metric),
    type: 'bar',
    data: result.rows.map((row) => {
      const value = row[metric]
      return typeof value === 'number' ? value : Number(value ?? 0)
    }),
  }))

  const option = {
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: token.colorBgElevated,
      borderColor: token.colorBorderSecondary,
      textStyle: { color: token.colorText },
    },
    legend: {
      data: series.map((item) => item.name),
      bottom: 0,
      textStyle: { color: token.colorTextSecondary },
    },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
    xAxis: {
      type: 'category' as const,
      data: categories,
      axisLine: { lineStyle: { color: token.colorBorderSecondary } },
      axisLabel: { color: token.colorTextSecondary, rotate: categories.length > 8 ? 30 : 0 },
    },
    yAxis: {
      type: 'value' as const,
      splitLine: { lineStyle: { color: token.colorBorderSecondary, type: 'dashed' as const } },
      axisLabel: { color: token.colorTextSecondary },
    },
    series,
  }

  return (
    <Card title="趋势图" loading={loading}>
      <ReactECharts option={option} style={{ height: 320 }} />
    </Card>
  )
}
