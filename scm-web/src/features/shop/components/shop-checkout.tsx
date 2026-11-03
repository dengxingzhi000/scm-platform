'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useMutation, useQuery } from '@tanstack/react-query'
import { message } from 'antd'
import dayjs from 'dayjs'
import { ArrowLeft, CheckCircle2, CreditCard } from 'lucide-react'

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
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Textarea } from '@/components/ui/textarea'
import { toApiError } from '@/lib/api/envelope'
import { mallApi, orderCenterApi, paymentApi } from '@/lib/api/services/ecommerce'
import type { OcOrder, OcOrderItem, PaymentOrderDTO } from '@/lib/api/contracts/ecommerce'
import { useAuthStore } from '@/stores/useAuthStore'
import { cartTotal, loadCartLines } from '../lib/cart'

export default function ShopCheckout() {
  const userId = useAuthStore((state) => state.user?.id ?? null)
  const router = useRouter()
  const [receiverName, setReceiverName] = useState('')
  const [receiverPhone, setReceiverPhone] = useState('')
  const [receiverAddress, setReceiverAddress] = useState('')
  const [remark, setRemark] = useState('')
  const [channel, setChannel] = useState('alipay')
  const [result, setResult] = useState<{
    order: OcOrder
    payment: PaymentOrderDTO
  } | null>(null)

  const cartQuery = useQuery({
    queryKey: ['shop', 'cart', userId, 'lines'],
    queryFn: () => loadCartLines(userId!),
    enabled: !!userId,
  })

  const submitOrder = useMutation({
    mutationFn: async () => {
      const lines = cartQuery.data ?? []
      if (lines.length === 0) throw new Error('购物车为空')

      const total = cartTotal(lines)
      if (total <= 0 || lines.some((line) => line.price <= 0)) {
        throw new Error('购物车中存在未定价商品，请先维护商品价格后再结算')
      }
      const orderNo = `SO${dayjs().format('YYYYMMDDHHmmss')}${Math.floor(
        Math.random() * 9000 + 1000
      )}`
      const items: OcOrderItem[] = lines.map((line) => ({
        productId: line.item.productId,
        skuId: line.item.skuId,
        productName: line.name,
        skuName: line.skuName,
        price: line.price,
        quantity: line.item.quantity,
        payableAmount: line.price * line.item.quantity,
      }))

      const order = await orderCenterApi.create(
        {
          orderNo,
          userId: userId!,
          channel: 'MALL',
          orderType: 'NORMAL',
          totalAmount: total,
          discountAmount: 0,
          shippingFee: 0,
          payableAmount: total,
          receiverName,
          receiverPhone,
          receiverAddress,
          remark,
        },
        items
      )

      const payment = await paymentApi.create({
        orderNo,
        userId: userId!,
        amount: order.payableAmount ?? total,
        paymentChannel: channel,
      })

      await mallApi.cart.clear(userId!).catch(() => undefined)
      return { order, payment }
    },
    onSuccess: (data) => {
      setResult(data)
      message.success('订单已创建，支付单已生成')
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const payNow = useMutation({
    mutationFn: () =>
      orderCenterApi.pay(result!.order.orderNo, result!.payment.paymentNo),
    onSuccess: () => {
      message.success('订单已标记为已支付')
      router.push('/shop/orders')
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  if (!userId) {
    return (
      <div className="rounded-2xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-20 text-center">
        <h2 className="text-[16px] font-semibold">请先登录</h2>
        <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
          登录后才能提交订单。
        </p>
        <Button asChild className="mt-4">
          <Link href="/login">去登录</Link>
        </Button>
      </div>
    )
  }

  const lines = cartQuery.data ?? []
  const total = cartTotal(lines)

  return (
    <div className="space-y-6">
      <Link
        href="/shop/cart"
        className="inline-flex items-center gap-1 text-[12px] text-[var(--color-text-secondary)] hover:text-[var(--color-primary-text)]"
      >
        <ArrowLeft className="size-3.5" />
        返回购物车
      </Link>

      <div>
        <p className="text-[10px] font-medium tracking-[0.16em] text-[var(--color-text-tertiary)] uppercase">
          CHECKOUT
        </p>
        <h1 className="mt-1 text-[24px] font-semibold tracking-[-0.02em]">确认订单</h1>
      </div>

      {cartQuery.isLoading ? (
        <div className="h-40 animate-pulse rounded-2xl bg-[var(--color-bg-sunken)]" />
      ) : lines.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-16 text-center text-[12px] text-[var(--color-text-secondary)]">
          购物车为空，无法结算。
        </div>
      ) : (
        <div className="grid gap-5 lg:grid-cols-[1.15fr_1fr]">
          <div className="space-y-5">
            <section className="rounded-2xl border border-[var(--color-border-secondary)] bg-white p-5">
              <h2 className="text-[14px] font-semibold">收货信息</h2>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <Field label="收货人">
                  <Input
                    value={receiverName}
                    onChange={(event) => setReceiverName(event.target.value)}
                    placeholder="请输入收货人姓名"
                  />
                </Field>
                <Field label="手机号">
                  <Input
                    value={receiverPhone}
                    onChange={(event) => setReceiverPhone(event.target.value)}
                    placeholder="请输入手机号"
                  />
                </Field>
                <div className="sm:col-span-2">
                  <Field label="收货地址">
                    <Input
                      value={receiverAddress}
                      onChange={(event) => setReceiverAddress(event.target.value)}
                      placeholder="省 / 市 / 区 / 详细地址"
                    />
                  </Field>
                </div>
                <div className="sm:col-span-2">
                  <Field label="订单备注">
                    <Textarea
                      value={remark}
                      onChange={(event) => setRemark(event.target.value)}
                      rows={2}
                      placeholder="可选"
                    />
                  </Field>
                </div>
              </div>
            </section>

            <section className="rounded-2xl border border-[var(--color-border-secondary)] bg-white p-5">
              <h2 className="text-[14px] font-semibold">支付方式</h2>
              <div className="mt-3 flex gap-2">
                {[
                  { value: 'alipay', label: '支付宝' },
                  { value: 'wechat', label: '微信支付' },
                ].map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setChannel(option.value)}
                    className={`flex items-center gap-2 rounded-lg border px-4 py-2.5 text-[12px] transition-colors ${
                      channel === option.value
                        ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary-text)]'
                        : 'border-[var(--color-border-secondary)] bg-white'
                    }`}
                  >
                    <CreditCard className="size-4" />
                    {option.label}
                  </button>
                ))}
              </div>
            </section>
          </div>

          <section className="h-fit rounded-2xl border border-[var(--color-border-secondary)] bg-white p-5">
            <h2 className="text-[14px] font-semibold">商品清单</h2>
            <div className="mt-3 space-y-3">
              {lines.map((line) => (
                <div key={line.item.id} className="flex items-center gap-3">
                  <div className="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-[var(--color-bg-sunken)]">
                    {line.image ? (
                      <img src={line.image} alt={line.name} className="size-full object-cover" />
                    ) : (
                      <span className="font-serif text-lg text-[var(--color-primary-text)]/40">
                        {line.name.slice(0, 1)}
                      </span>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[12px] font-medium">{line.name}</p>
                    <p className="text-[10px] text-[var(--color-text-secondary)]">
                      {line.skuName || `SKU ${line.item.skuId}`} × {line.item.quantity}
                    </p>
                  </div>
                  <p className="text-[12px] font-semibold">
                    ¥{(line.price * line.item.quantity).toFixed(2)}
                  </p>
                </div>
              ))}
            </div>

            <Separator className="my-4" />

            <div className="space-y-1.5 text-[12px]">
              <div className="flex justify-between text-[var(--color-text-secondary)]">
                <span>商品合计</span>
                <span>¥{total.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-[var(--color-text-secondary)]">
                <span>运费</span>
                <span>¥0.00</span>
              </div>
              <div className="flex items-center justify-between pt-2 text-[14px] font-semibold">
                <span>应付金额</span>
                <span className="text-[var(--color-primary-text)]">¥{total.toFixed(2)}</span>
              </div>
            </div>

            <Button
              className="mt-4 h-11 w-full"
              disabled={
                submitOrder.isPending ||
                !receiverName.trim() ||
                !receiverPhone.trim() ||
                !receiverAddress.trim()
              }
              onClick={() => submitOrder.mutate()}
            >
              {submitOrder.isPending ? '提交中…' : '提交订单并创建支付单'}
            </Button>
            <p className="mt-2 text-[10px] leading-4 text-[var(--color-text-tertiary)]">
              演示流程：创建订单后会调用 scm-payment 生成支付单，不会跳转真实支付网关。
            </p>
          </section>
        </div>
      )}

      <Dialog open={!!result} onOpenChange={(open) => !open && setResult(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <CheckCircle2 className="size-5 text-[var(--color-success)]" />
              订单创建成功
            </DialogTitle>
            <DialogDescription>
              订单号 {result?.order.orderNo}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 rounded-lg bg-[var(--color-bg-sunken)] p-3 text-[12px]">
            <div className="flex justify-between">
              <span className="text-[var(--color-text-secondary)]">应付金额</span>
              <span className="font-semibold">¥{Number(result?.order.payableAmount ?? 0).toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[var(--color-text-secondary)]">支付单号</span>
              <span className="font-mono text-[11px]">{result?.payment.paymentNo}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[var(--color-text-secondary)]">支付渠道</span>
              <span>{result?.payment.paymentChannel}</span>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" asChild>
              <Link href="/shop/orders">稍后支付</Link>
            </Button>
            <Button disabled={payNow.isPending} onClick={() => payNow.mutate()}>
              {payNow.isPending ? '处理中…' : '模拟支付完成'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <Label className="text-xs">{label}</Label>
      {children}
    </div>
  )
}
