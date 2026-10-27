'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select, Progress, Modal, Form, InputNumber } from 'antd'
import { useAdjustStock, useInventoryList } from '../hooks'
import type { InventoryItem, StockLevel } from '../types'

const PAGE_SIZE = 20

const statusConfig: Record<StockLevel, { color: string; label: string }> = {
  normal: { color: 'success', label: '正常' },
  low: { color: 'warning', label: '偏低' },
  out: { color: 'default', label: '缺货' },
}

const ADJUST_TYPE_LABELS: Record<number, string> = {
  1: '入库',
  2: '出库',
  6: '盘点调整',
  7: '调拨',
}

interface AdjustForm {
  quantity: number
  adjustType: 1 | 2 | 6 | 7
  remark?: string
}

export default function InventoryList() {
  const [current, setCurrent] = useState(1)
  const [skuId, setSkuId] = useState('')
  const [status, setStatus] = useState<StockLevel>()
  const [adjustTarget, setAdjustTarget] = useState<InventoryItem | null>(null)
  const [form] = Form.useForm<AdjustForm>()
  const adjustStock = useAdjustStock()

  const skuIds = skuId ? [skuId] : undefined
  const { data, isLoading } = useInventoryList({ current, pageSize: PAGE_SIZE, skuIds, status })

  const columns = [
    { title: 'SKU ID', dataIndex: 'skuId', key: 'skuId', width: 220, render: (v: string) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v}</span> },
    { title: '仓库 ID', dataIndex: 'warehouseId', key: 'warehouseId', width: 220, render: (v: string) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v}</span> },
    { title: '库位', dataIndex: 'locationCode', key: 'locationCode', width: 100, render: (v?: string) => v || '-' },
    { title: '总库存', dataIndex: 'totalStock', key: 'totalStock', width: 80, align: 'center' as const },
    { title: '可用', dataIndex: 'availableStock', key: 'availableStock', width: 80, align: 'center' as const },
    { title: '锁定', dataIndex: 'lockedStock', key: 'lockedStock', width: 80, align: 'center' as const },
    { title: '安全库存', dataIndex: 'safetyStock', key: 'safetyStock', width: 90, align: 'center' as const },
    {
      title: '水位', key: 'stockLevel', width: 120,
      render: (_: unknown, r: InventoryItem) => {
        const percent = r.safetyStock > 0 ? Math.round((r.availableStock / r.safetyStock) * 100) : 100
        return <Progress percent={Math.min(percent, 100)} size="small" status={r.status === 'out' ? 'exception' : r.status === 'low' ? 'active' : 'success'} />
      },
    },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: StockLevel) => <Tag color={statusConfig[s].color}>{statusConfig[s].label}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 140 },
    {
      title: '操作', key: 'action', width: 90, fixed: 'right' as const,
      render: (_: unknown, record: InventoryItem) => (
        <Button type="link" size="small" onClick={() => { setAdjustTarget(record); form.resetFields() }}>调整</Button>
      ),
    },
  ]

  const submitAdjust = async () => {
    if (!adjustTarget) return
    const values = await form.validateFields()
    await adjustStock.mutateAsync({
      skuId: adjustTarget.skuId,
      warehouseId: adjustTarget.warehouseId,
      quantity: values.quantity,
      adjustType: values.adjustType,
      remark: values.remark,
    })
    setAdjustTarget(null)
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>库存管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          {/* The query API filters by exact SKU id; there is no name or barcode keyword. */}
          <Input.Search placeholder="按 SKU ID 精确查询" style={{ width: 280 }} onSearch={(v) => { setSkuId(v.trim()); setCurrent(1) }} allowClear />
          <Select placeholder="库存状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={[{ label: '正常', value: 'normal' }, { label: '偏低', value: 'low' }, { label: '缺货', value: 'out' }]} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id" scroll={{ x: 1400 }}
          pagination={{ current, pageSize: PAGE_SIZE, total: data?.total, onChange: setCurrent, showTotal: (t) => `共 ${t} 条` }} />
      </Card>
      <Modal
        title={`调整库存 · ${adjustTarget?.skuId ?? ''}`}
        open={!!adjustTarget}
        okText="提交"
        confirmLoading={adjustStock.isPending}
        onOk={submitAdjust}
        onCancel={() => setAdjustTarget(null)}
      >
        <Form form={form} layout="vertical" initialValues={{ adjustType: 6, quantity: 0 }}>
          <Form.Item name="adjustType" label="调整类型" rules={[{ required: true }]}>
            <Select options={Object.entries(ADJUST_TYPE_LABELS).map(([value, label]) => ({ label, value: Number(value) }))} />
          </Form.Item>
          {/* The API takes a signed quantity: positive increases, negative decreases. */}
          <Form.Item name="quantity" label="变动数量（正数增加 / 负数减少）" rules={[{ required: true, message: '请输入变动数量' }]}>
            <InputNumber style={{ width: '100%' }} min={-1000000} max={1000000} precision={0} />
          </Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} maxLength={200} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
