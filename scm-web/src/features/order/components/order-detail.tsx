'use client'
import { Descriptions, Card, Tag, Table, Timeline, Empty } from 'antd'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/ui/page-header'
import { useOrderDetail } from '../hooks'
import { orderService } from '../services/order.service'
import OrderStatusFlow from './order-status-flow'

interface OrderDetailProps { id: string }

export default function OrderDetail({ id }: OrderDetailProps) {
  const { data: order, isLoading } = useOrderDetail(id)
  const router = useRouter()
  const statusFlow = orderService.getStatusFlow()

  if (isLoading && !order) return <Card loading style={{ minHeight: 320 }} />
  if (!order) return <Empty description="订单不存在" />

  const itemColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v.toLocaleString()}` },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const },
    { title: '小计', dataIndex: 'totalPrice', key: 'totalPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v.toLocaleString()}` },
    { title: '已退数量', dataIndex: 'refundQuantity', key: 'refundQuantity', width: 90, align: 'center' as const },
  ]

  const timelineItems: { children: string; color?: string }[] = [
    { children: `创建订单 ${order.createdAt}` },
  ]
  if (order.payTime) timelineItems.push({ children: `付款 ${order.payTime}`, color: 'green' })
  if (order.shipTime) timelineItems.push({ children: `发货 ${order.shipTime}`, color: 'green' })
  if (order.completeTime) timelineItems.push({ children: `完成 ${order.completeTime}`, color: 'green' })
  if (order.cancelTime) timelineItems.push({ children: `取消 ${order.cancelTime}${order.cancelReason ? ` · ${order.cancelReason}` : ''}`, color: 'red' })

  const cfg = statusFlow[order.status]

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="订单履约"
        title="订单详情"
        description={`订单号 ${order.orderNo} · 下单用户 ${order.username}`}
        actions={
          <Button variant="outline" onClick={() => router.push('/order')}>
            <ArrowLeft />
            返回列表
          </Button>
        }
      />
      <OrderStatusFlow status={order.status} />
      <Card title="基本信息">
        <Descriptions column={2}>
          <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={cfg?.color}>{order.statusLabel}</Tag></Descriptions.Item>
          <Descriptions.Item label="下单用户">{order.username}</Descriptions.Item>
          <Descriptions.Item label="来源">{order.orderSource || '-'}</Descriptions.Item>
          <Descriptions.Item label="订单金额">¥{order.totalAmount.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="优惠">¥{order.discountAmount.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="运费">¥{order.freightAmount.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="应付金额">¥{order.payableAmount.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="实付金额">¥{order.paidAmount.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="运单号">{order.waybillNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="收货地址" span={2}>{order.shippingAddress || '-'}</Descriptions.Item>
          <Descriptions.Item label="买家留言" span={2}>{order.buyerMessage || '-'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{order.createdAt}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{order.updatedAt}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="商品明细">
        <Table columns={itemColumns} dataSource={order.items} loading={isLoading} rowKey="id" pagination={false} size="small" />
      </Card>
      <Card title="订单日志">
        <Timeline items={timelineItems} />
      </Card>
    </div>
  )
}
