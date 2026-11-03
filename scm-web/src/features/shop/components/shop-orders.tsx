'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { ChevronLeft, ChevronRight, Package, ReceiptText } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { toApiError } from '@/lib/api/envelope'
import { orderCenterApi, paymentApi } from '@/lib/api/services/ecommerce'
import type { OcOrder } from '@/lib/api/contracts/ecommerce'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  formatAmount,
  formatDateTime,
  orderCenterStatusMap,
  statusMeta,
} from '@/features/ecommerce/lib/status'

const PAGE_SIZE = 10
const ALL = '__all__'

export default function ShopOrders() {
  const userId = useAuthStore((state) => state.user?.id ?? null)
  const queryClient = useQueryClient()
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState<number | undefined>()
  const [detail, setDetail] = useState<OcOrder | null>(null)
  const [cancelTarget, setCancelTarget] = useState<OcOrder | null>(null)
  const [cancelReason, setCancelReason] = useState('')

  const ordersQuery = useQuery({
    queryKey: ['shop', 'orders', userId, page, status],
    queryFn: () =>
      orderCenterApi.page({
        page,
        size: PAGE_SIZE,
        userId: userId!,
        status,
      }),
    enabled: !!userId,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['shop', 'orders'] })
  }

  const payOrder = useMutation({
    mutationFn: async (order: OcOrder) => {
      const payment = await paymentApi.create({
        orderNo: order.orderNo,
        userId: order.userId,
        amount: order.payableAmount ?? order.totalAmount ?? 0,
        paymentChannel: 'alipay',
      })
      await orderCenterApi.pay(order.orderNo, payment.paymentNo)
      return payment
    },
    onSuccess: (payment) => {
      message.success(`支付完成，支付单号 ${payment.paymentNo}`)
      invalidate()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const confirmOrder = useMutation({
    mutationFn: (orderNo: string) => orderCenterApi.confirm(orderNo),
    onSuccess: () => {
      message.success('已确认收货')
      invalidate()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const cancelOrder = useMutation({
    mutationFn: () => orderCenterApi.cancel(cancelTarget!.orderNo, cancelReason),
    onSuccess: () => {
      message.success('订单已取消')
      setCancelTarget(null)
      setCancelReason('')
      invalidate()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  if (!userId) {
    return (
      <div className="rounded-2xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-20 text-center">
        <h2 className="text-[16px] font-semibold">请先登录</h2>
        <Button asChild className="mt-4">
          <Link href="/login">去登录</Link>
        </Button>
      </div>
    )
  }

  const orders = ordersQuery.data?.records ?? []
  const total = ordersQuery.data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-[10px] font-medium tracking-[0.16em] text-[var(--color-text-tertiary)] uppercase">
            MY ORDERS
          </p>
          <h1 className="mt-1 text-[24px] font-semibold tracking-[-0.02em]">我的订单</h1>
          <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
            共 {total} 笔订单
          </p>
        </div>
        <Select
          value={status === undefined ? ALL : String(status)}
          onValueChange={(value) => {
            setStatus(value === ALL ? undefined : Number(value))
            setPage(1)
          }}
        >
          <SelectTrigger className="h-9 w-36">
            <SelectValue placeholder="全部状态" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>全部状态</SelectItem>
            {Object.entries(orderCenterStatusMap).map(([value, meta]) => (
              <SelectItem key={value} value={value}>
                {meta.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {ordersQuery.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-[var(--color-bg-sunken)]" />
      ) : ordersQuery.isError ? (
        <State text="订单加载失败，请确认 scm-order-center 服务已启动。" />
      ) : orders.length === 0 ? (
        <State text="还没有订单，去商城挑点商品吧。" />
      ) : (
        <div className="space-y-3">
          {orders.map((order) => {
            const meta = statusMeta(orderCenterStatusMap, order.status)
            return (
              <div
                key={order.orderNo}
                className="rounded-2xl border border-[var(--color-border-secondary)] bg-white p-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2 text-[12px]">
                    <span className="font-mono text-[11px] text-[var(--color-text-secondary)]">
                      {order.orderNo}
                    </span>
                    <Badge variant={meta.tone}>{meta.label}</Badge>
                  </div>
                  <span className="text-[11px] text-[var(--color-text-tertiary)]">
                    {formatDateTime(order.createdAt)}
                  </span>
                </div>

                <div className="mt-3 flex flex-wrap items-center gap-4">
                  <div className="flex-1 min-w-[200px] text-[12px] text-[var(--color-text-secondary)]">
                    <p>
                      收货人：{order.receiverName || '—'} {order.receiverPhone || ''}
                    </p>
                    <p className="mt-1 line-clamp-1">{order.receiverAddress || '未填写收货地址'}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-[10px] text-[var(--color-text-tertiary)]">应付金额</p>
                    <p className="text-[16px] font-semibold text-[var(--color-primary-text)]">
                      {formatAmount(order.payableAmount)}
                    </p>
                  </div>
                </div>

                <Separator className="my-3" />

                <div className="flex flex-wrap justify-end gap-2">
                  <Button variant="ghost" size="sm" onClick={() => setDetail(order)}>
                    <ReceiptText />
                    详情
                  </Button>
                  {order.status === 1 && (
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCancelTarget(order)}
                      >
                        取消订单
                      </Button>
                      <Button
                        size="sm"
                        disabled={payOrder.isPending}
                        onClick={() => payOrder.mutate(order)}
                      >
                        立即支付
                      </Button>
                    </>
                  )}
                  {order.status === 5 && (
                    <Button
                      size="sm"
                      disabled={confirmOrder.isPending}
                      onClick={() => confirmOrder.mutate(order.orderNo)}
                    >
                      确认收货
                    </Button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {pageCount > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 1}
            onClick={() => setPage((value) => value - 1)}
          >
            <ChevronLeft />
            上一页
          </Button>
          <span className="text-[12px] text-[var(--color-text-secondary)]">
            第 {page} / {pageCount} 页
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= pageCount}
            onClick={() => setPage((value) => value + 1)}
          >
            下一页
            <ChevronRight />
          </Button>
        </div>
      )}

      <Dialog open={!!detail} onOpenChange={(open) => !open && setDetail(null)}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>订单详情</DialogTitle>
            <DialogDescription>{detail?.orderNo}</DialogDescription>
          </DialogHeader>
          {detail && (
            <div className="space-y-2 text-[12px]">
              <Row label="状态" value={statusMeta(orderCenterStatusMap, detail.status).label} />
              <Row label="渠道" value={detail.channel || '—'} />
              <Row label="商品总额" value={formatAmount(detail.totalAmount)} />
              <Row label="优惠金额" value={formatAmount(detail.discountAmount)} />
              <Row label="运费" value={formatAmount(detail.shippingFee)} />
              <Row label="应付金额" value={formatAmount(detail.payableAmount)} />
              <Row label="收货人" value={detail.receiverName || '—'} />
              <Row label="联系电话" value={detail.receiverPhone || '—'} />
              <Row label="收货地址" value={detail.receiverAddress || '—'} />
              <Row label="备注" value={detail.remark || '—'} />
              <Row label="创建时间" value={formatDateTime(detail.createdAt)} />
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDetail(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!cancelTarget}
        onOpenChange={(open) => !open && setCancelTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>取消订单</DialogTitle>
            <DialogDescription>{cancelTarget?.orderNo}</DialogDescription>
          </DialogHeader>
          <Input
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            placeholder="取消原因（可选）"
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelTarget(null)}>
              返回
            </Button>
            <Button
              variant="destructive"
              disabled={cancelOrder.isPending}
              onClick={() => cancelOrder.mutate()}
            >
              确认取消
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 rounded-md bg-[var(--color-bg-sunken)] px-3 py-2">
      <span className="text-[var(--color-text-secondary)]">{label}</span>
      <span className="text-right font-medium">{value}</span>
    </div>
  )
}

function State({ text }: { text: string }) {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-16 text-center">
      <Package className="size-8 text-[var(--color-text-tertiary)]" />
      <p className="mt-3 text-[12px] text-[var(--color-text-secondary)]">{text}</p>
    </div>
  )
}
