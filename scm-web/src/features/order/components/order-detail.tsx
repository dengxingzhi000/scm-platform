'use client'
import { useState } from 'react'
import { Descriptions, Card, Tag, Table, Timeline, Empty } from 'antd'
import { useRouter } from 'next/navigation'
import { ArrowLeft, Ban, ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Textarea } from '@/components/ui/textarea'
import { PageHeader } from '@/components/ui/page-header'
import {
  useCancelOrder,
  useOrderDetail,
  useOrderStatusHistory,
  useUpdateOrderStatus,
} from '../hooks'
import { orderService } from '../services/order.service'
import { formatDateTime } from '../lib/mappers'
import type { OrderStatus } from '../types'
import OrderStatusFlow from './order-status-flow'

interface OrderDetailProps { id: string }

/** Mirrors `OrderStatus.canTransitionTo()` in scm-order; cancellation goes through its own endpoint. */
const NEXT_STATUSES: Partial<Record<OrderStatus, OrderStatus[]>> = {
  PENDING_PAYMENT: ['PAID'],
  PAID: ['PENDING_SHIP', 'REFUNDING'],
  PENDING_SHIP: ['SHIPPED', 'REFUNDING'],
  SHIPPED: ['IN_TRANSIT', 'REFUNDING'],
  IN_TRANSIT: ['DELIVERED', 'REFUNDING'],
  DELIVERED: ['COMPLETED', 'REFUNDING'],
  REFUNDING: ['REFUNDED'],
}

const CANCELABLE: OrderStatus[] = ['PENDING_PAYMENT', 'PAID']

export default function OrderDetail({ id }: OrderDetailProps) {
  const { data: order, isLoading } = useOrderDetail(id)
  const { data: history } = useOrderStatusHistory(id)
  const updateStatus = useUpdateOrderStatus()
  const cancelOrder = useCancelOrder()
  const router = useRouter()
  const statusFlow = orderService.getStatusFlow()
  const [cancelOpen, setCancelOpen] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

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

  // The status-history endpoint is authoritative; the timestamp fallback covers legacy
  // orders that were created before history rows existed.
  const historyItems = (history ?? []).map((row) => ({
    children: [
      row.toStatusName || (row.toStatus ? statusFlow[row.toStatus]?.label : '') || row.toStatus || '-',
      formatDateTime(row.transitionedAt),
      row.operatorName ? `· ${row.operatorName}` : '',
      row.remark ? `· ${row.remark}` : '',
    ]
      .filter(Boolean)
      .join(' '),
    color:
      row.toStatus === 'COMPLETED'
        ? 'green'
        : row.toStatus === 'CANCELLED' || row.toStatus === 'REFUNDED'
          ? 'red'
          : undefined,
  }))

  const timelineItems: { children: string; color?: string }[] =
    historyItems.length > 0
      ? historyItems
      : [
          { children: `创建订单 ${order.createdAt}` },
        ]
  if (historyItems.length === 0) {
    if (order.payTime) timelineItems.push({ children: `付款 ${order.payTime}`, color: 'green' })
    if (order.shipTime) timelineItems.push({ children: `发货 ${order.shipTime}`, color: 'green' })
    if (order.completeTime) timelineItems.push({ children: `完成 ${order.completeTime}`, color: 'green' })
  }
  if (order.cancelTime) {
    timelineItems.push({ children: `取消 ${order.cancelTime}${order.cancelReason ? ` · ${order.cancelReason}` : ''}`, color: 'red' })
  }

  const cfg = statusFlow[order.status]
  const nextStatuses = NEXT_STATUSES[order.status] ?? []
  const canCancel = CANCELABLE.includes(order.status)

  const submitCancel = async () => {
    if (!cancelReason.trim()) return
    await cancelOrder.mutateAsync({ id: order.id, reason: cancelReason.trim() })
    setCancelOpen(false)
    setCancelReason('')
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="订单履约"
        title="订单详情"
        description={`订单号 ${order.orderNo} · 下单用户 ${order.username}`}
        actions={
          <div className="flex items-center gap-2">
            {nextStatuses.length > 0 && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    disabled={updateStatus.isPending}
                  >
                    状态流转
                    <ChevronDown />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>选择目标状态</DropdownMenuLabel>
                  {nextStatuses.map((status) => (
                    <DropdownMenuItem
                      key={status}
                      onSelect={() =>
                        updateStatus.mutate({ id: order.id, status })
                      }
                    >
                      {statusFlow[status]?.label ?? status}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
            {canCancel && (
              <Button
                variant="outline"
                className="text-destructive hover:text-destructive"
                disabled={cancelOrder.isPending}
                onClick={() => setCancelOpen(true)}
              >
                <Ban />
                取消订单
              </Button>
            )}
            <Button variant="outline" onClick={() => router.push('/order')}>
              <ArrowLeft />
              返回列表
            </Button>
          </div>
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

      <Dialog open={cancelOpen} onOpenChange={setCancelOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>取消订单</DialogTitle>
            <DialogDescription>
              取消后订单不可恢复，取消原因会写入订单日志。
            </DialogDescription>
          </DialogHeader>
          <Textarea
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            placeholder="请输入取消原因"
            maxLength={200}
            rows={3}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelOpen(false)}>
              返回
            </Button>
            <Button
              variant="destructive"
              disabled={!cancelReason.trim() || cancelOrder.isPending}
              onClick={submitCancel}
            >
              确认取消
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
