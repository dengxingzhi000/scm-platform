'use client'

import { Button, Card, Table } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import type { TableColumnsType } from 'antd'
import type { DatasetQueryResult } from '../types'

interface AnalyticsResultTableProps {
  result?: DatasetQueryResult
  loading?: boolean
}

const NUMERIC_TYPES = new Set(['INT', 'BIGINT', 'DECIMAL', 'DOUBLE'])

function escapeCsv(value: unknown) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

export function toCsv(result: DatasetQueryResult) {
  const header = result.columns.map((column) => escapeCsv(column.code)).join(',')
  const rows = result.rows.map((row) =>
    result.columns.map((column) => escapeCsv(row[column.code])).join(','),
  )
  return [header, ...rows].join('\n')
}

function downloadCsv(filename: string, content: string) {
  // BOM keeps Excel from mangling UTF-8 headers.
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export default function AnalyticsResultTable({ result, loading }: AnalyticsResultTableProps) {
  const columns: TableColumnsType<Record<string, unknown>> =
    result?.columns.map((column) => ({
      title: column.code,
      dataIndex: column.code,
      key: column.code,
      align: NUMERIC_TYPES.has(column.dataType) ? 'right' : 'left',
      render: (value: unknown) => (value == null ? '-' : String(value)),
    })) ?? []

  const exportCsv = () => {
    if (!result || result.rows.length === 0) {
      return
    }
    downloadCsv(`${result.datasetCode}-${new Date().toISOString().slice(0, 10)}.csv`, toCsv(result))
  }

  return (
    <Card
      title={
        result
          ? `查询结果（${result.rowCount} 行 / ${result.elapsedMs} ms）`
          : '查询结果'
      }
      loading={loading}
      extra={
        <Button icon={<DownloadOutlined />} onClick={exportCsv} disabled={!result?.rows.length}>
          导出 CSV
        </Button>
      }
    >
      <Table<Record<string, unknown>>
        size="small"
        rowKey={(_, index) => String(index)}
        columns={columns}
        dataSource={result?.rows ?? []}
        locale={{ emptyText: '暂无数据' }}
        pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${total} 行` }}
        scroll={{ x: 'max-content' }}
      />
    </Card>
  )
}
