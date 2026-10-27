'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useOrderList } from '../hooks'
import { orderService } from '../services/order.service'
import type { Order, OrderStatus } from '../types'

const PAGE_SIZE = 20

export default function OrderList() {
  const [current, setCurrent] = useState(1)
  const [orderNo, setOrderNo] = useState('')
  const [status, setStatus] = useState<OrderStatus>()
  const router = useRouter()
  const { data, isLoading } = useOrderList({ current, pageSize: PAGE_SIZE, orderNo, status })
  const statusFlow = orderService.getStatusFlow()

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', render: (text: string, record: Order) => <a onClick={() => router.push(`/order/${record.id}`)}>{text}</a> },
    { title: '下单用户', dataIndex: 'username', key: 'username', width: 120 },
    { title: '订单金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' as const, render: (v: number) => `¥${v.toLocaleString()}` },
    { title: '应付', dataIndex: 'payableAmount', key: 'payableAmount', width: 110, align: 'right' as const, render: (v: number) => `¥${v.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: OrderStatus, record: Order) => <Tag color={statusFlow[s]?.color}>{record.statusLabel}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
    { title: '操作', key: 'action', width: 90, fixed: 'right' as const, render: (_: unknown, record: Order) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/order/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>订单管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          {/* The query API matches order_no exactly; there is no fuzzy keyword filter. */}
          <Input.Search placeholder="按订单号精确查询" style={{ width: 240 }} onSearch={(v) => { setOrderNo(v.trim()); setCurrent(1) }} allowClear />
          <Select placeholder="订单状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={Object.entries(statusFlow).map(([key, val]) => ({ label: val.label, value: key }))} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id" scroll={{ x: 900 }}
          pagination={{ current, pageSize: PAGE_SIZE, total: data?.total, onChange: setCurrent, showTotal: (t) => `共 ${t} 条` }} />
      </Card>
    </div>
  )
}
