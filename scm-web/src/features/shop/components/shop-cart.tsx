'use client'

import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { ArrowRight, Minus, Plus, ShoppingCart, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { toApiError } from '@/lib/api/envelope'
import { mallApi } from '@/lib/api/services/ecommerce'
import { useAuthStore } from '@/stores/useAuthStore'
import { cartTotal, loadCartLines } from '../lib/cart'

export default function ShopCart() {
  const userId = useAuthStore((state) => state.user?.id ?? null)
  const queryClient = useQueryClient()

  const cartQuery = useQuery({
    queryKey: ['shop', 'cart', userId, 'lines'],
    queryFn: () => loadCartLines(userId!),
    enabled: !!userId,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['shop', 'cart'] })
  }

  const updateQuantity = useMutation({
    mutationFn: ({ cartItemId, quantity }: { cartItemId: number; quantity: number }) =>
      mallApi.cart.updateQuantity(cartItemId, userId!, quantity),
    onSuccess: invalidate,
    onError: (error) => message.error(toApiError(error).message),
  })

  const removeItem = useMutation({
    mutationFn: (cartItemId: number) => mallApi.cart.remove(cartItemId, userId!),
    onSuccess: () => {
      message.success('已移出购物车')
      invalidate()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const clearCart = useMutation({
    mutationFn: () => mallApi.cart.clear(userId!),
    onSuccess: () => {
      message.success('购物车已清空')
      invalidate()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  if (!userId) {
    return (
      <EmptyState
        title="请先登录"
        description="登录后即可查看购物车、下单与支付。"
        actionLabel="去登录"
        actionHref="/login"
      />
    )
  }

  if (cartQuery.isLoading) {
    return <div className="h-64 animate-pulse rounded-2xl bg-[var(--color-bg-sunken)]" />
  }

  if (cartQuery.isError) {
    return (
      <EmptyState
        title="购物车加载失败"
        description="请确认 scm-mall 服务已启动。"
        actionLabel="返回商城"
        actionHref="/shop"
      />
    )
  }

  const lines = cartQuery.data ?? []
  if (lines.length === 0) {
    return (
      <EmptyState
        title="购物车还是空的"
        description="去挑选几件喜欢的商品吧。"
        actionLabel="去逛逛"
        actionHref="/shop"
      />
    )
  }

  const total = cartTotal(lines)
  const count = lines.reduce((sum, line) => sum + line.item.quantity, 0)

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-[10px] font-medium tracking-[0.16em] text-[var(--color-text-tertiary)] uppercase">
            SHOPPING CART
          </p>
          <h1 className="mt-1 text-[24px] font-semibold tracking-[-0.02em]">购物车</h1>
          <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
            共 {count} 件商品
          </p>
        </div>
        <Button
          variant="ghost"
          size="sm"
          className="text-[var(--color-text-secondary)]"
          disabled={clearCart.isPending}
          onClick={() => clearCart.mutate()}
        >
          <Trash2 />
          清空购物车
        </Button>
      </div>

      <div className="overflow-hidden rounded-2xl border border-[var(--color-border-secondary)] bg-white">
        {lines.map((line, index) => (
          <div key={line.item.id}>
            {index > 0 && <Separator />}
            <div className="flex flex-wrap items-center gap-4 px-4 py-4">
              <div className="flex size-20 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-[var(--color-bg-sunken)]">
                {line.image ? (
                  <img src={line.image} alt={line.name} className="size-full object-cover" />
                ) : (
                  <span className="font-serif text-2xl text-[var(--color-primary-text)]/40">
                    {line.name.slice(0, 1)}
                  </span>
                )}
              </div>

              <div className="min-w-[180px] flex-1">
                <p className="text-[13px] font-medium">{line.name}</p>
                <p className="mt-0.5 text-[11px] text-[var(--color-text-secondary)]">
                  {line.skuName || `SKU ${line.item.skuId}`}
                </p>
                <p className="mt-1 text-[12px] font-semibold text-[var(--color-primary-text)]">
                  ¥{line.price.toFixed(2)}
                </p>
              </div>

              <div className="flex items-center rounded-lg border border-[var(--color-border-secondary)]">
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-8 rounded-none"
                  aria-label="减少"
                  disabled={line.item.quantity <= 1 || updateQuantity.isPending}
                  onClick={() =>
                    updateQuantity.mutate({
                      cartItemId: line.item.id,
                      quantity: line.item.quantity - 1,
                    })
                  }
                >
                  <Minus className="size-3.5" />
                </Button>
                <span className="w-10 text-center text-xs">{line.item.quantity}</span>
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-8 rounded-none"
                  aria-label="增加"
                  disabled={updateQuantity.isPending}
                  onClick={() =>
                    updateQuantity.mutate({
                      cartItemId: line.item.id,
                      quantity: line.item.quantity + 1,
                    })
                  }
                >
                  <Plus className="size-3.5" />
                </Button>
              </div>

              <div className="ml-auto text-right">
                <p className="text-[13px] font-semibold">
                  ¥{(line.price * line.item.quantity).toFixed(2)}
                </p>
                <Button
                  variant="ghost"
                  size="sm"
                  className="mt-1 h-7 text-[11px] text-[var(--color-text-secondary)] hover:text-destructive"
                  disabled={removeItem.isPending}
                  onClick={() => removeItem.mutate(line.item.id)}
                >
                  移除
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-[var(--color-primary-soft)] px-5 py-4">
        <div>
          <p className="text-[11px] text-[var(--color-text-secondary)]">合计</p>
          <p className="text-[24px] font-semibold text-[var(--color-primary-text)]">
            ¥{total.toFixed(2)}
          </p>
        </div>
        <Button asChild className="h-11 px-8">
          <Link href="/shop/checkout">
            去结算
            <ArrowRight />
          </Link>
        </Button>
      </div>
    </div>
  )
}

function EmptyState({
  title,
  description,
  actionLabel,
  actionHref,
}: {
  title: string
  description: string
  actionLabel: string
  actionHref: string
}) {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-20 text-center">
      <span className="flex size-14 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[var(--color-primary-text)]">
        <ShoppingCart className="size-6" />
      </span>
      <h2 className="mt-4 text-[16px] font-semibold">{title}</h2>
      <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">{description}</p>
      <Button asChild className="mt-5">
        <Link href={actionHref}>{actionLabel}</Link>
      </Button>
    </div>
  )
}
