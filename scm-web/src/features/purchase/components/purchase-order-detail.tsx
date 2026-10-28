'use client'
import { Descriptions, Card, Tag, Table } from 'antd'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/ui/page-header'
import { usePurchaseOrderDetail } from '../hooks'
import { purchaseService } from '../services/purchase.service'

interface Props { id: string }

export default function PurchaseOrderDetail({ id }: Props) {
  const { data: order, isLoading } = usePurchaseOrderDetail(id)
  const router = useRouter()
  const statusMap = purchaseService.getStatusMap()
  if (!order) return null

  const itemColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const },
    { title: '已收货', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 80, align: 'center' as const },
    { title: '小计', dataIndex: 'totalPrice', key: 'totalPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
  ]

  const cfg = statusMap[order.status]

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="采购协同"
        title="采购单详情"
        description={`采购单号 ${order.purchaseNo} · 供应商 ${order.supplierName}`}
        actions={
          <Button variant="outline" onClick={() => router.push('/purchase')}>
            <ArrowLeft />
            返回列表
          </Button>
        }
      />
      <Card title="基本信息" loading={isLoading}>
        <Descriptions column={2}>
          <Descriptions.Item label="采购单号">{order.purchaseNo}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={cfg?.color}>{cfg?.label}</Tag></Descriptions.Item>
          <Descriptions.Item label="供应商">{order.supplierName}</Descriptions.Item>
          <Descriptions.Item label="总金额">¥{order.totalAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{order.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="采购明细">
        <Table columns={itemColumns} dataSource={order.items || []} rowKey="id" pagination={false} size="small" />
      </Card>
    </div>
  )
}
