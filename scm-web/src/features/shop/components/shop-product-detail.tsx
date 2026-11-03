'use client'

import { useMemo, useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { message } from 'antd'
import { ArrowLeft, Check, Minus, Plus, ShoppingCart } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { toApiError } from '@/lib/api/envelope'
import { mallApi, searchApi } from '@/lib/api/services/ecommerce'
import { productApiService } from '@/lib/api/services/product'
import { useAuthStore } from '@/stores/useAuthStore'
import { ProductCard } from './product-card'

export default function ShopProductDetail({ productId }: { productId: string }) {
  const userId = useAuthStore((state) => state.user?.id ?? null)
  const [selectedSkuId, setSelectedSkuId] = useState<string | null>(null)
  const [quantity, setQuantity] = useState(1)

  const spuQuery = useQuery({
    queryKey: ['shop', 'product', productId],
    queryFn: () => productApiService.getSpu(productId),
  })

  const skusQuery = useQuery({
    queryKey: ['shop', 'product', productId, 'skus'],
    queryFn: () => productApiService.listSkusBySpu(productId).catch(() => []),
  })

  const similarQuery = useQuery({
    queryKey: ['shop', 'product', productId, 'similar'],
    queryFn: () => {
      const numericId = Number(productId)
      if (!Number.isFinite(numericId)) return Promise.resolve([])
      return searchApi.similar(numericId, 8).catch(() => [])
    },
  })

  const spu = spuQuery.data
  const skus = useMemo(
    () => (skusQuery.data ?? []).filter((sku) => (sku.status ?? 1) === 1),
    [skusQuery.data]
  )
  const selectedSku =
    skus.find((sku) => String(sku.id) === selectedSkuId) ?? skus[0]

  const gallery = useMemo(() => {
    const values = [
      ...(spu?.images ?? '').split(','),
      spu?.mainImage,
      selectedSku?.imageUrl,
    ]
      .map((value) => value?.trim())
      .filter((value): value is string => !!value)
    return [...new Set(values)]
  }, [spu, selectedSku])

  const price = selectedSku?.sellingPrice ?? spu?.minPrice ?? 0
  const stock = selectedSku?.availableStock ?? selectedSku?.stock ?? spu?.totalStock ?? 0

  const addToCart = async () => {
    if (!userId) {
      message.warning('请先登录后再加入购物车')
      return
    }
    if (!spu || !selectedSku) {
      message.warning('该商品暂无可售 SKU')
      return
    }
    try {
      await mallApi.cart.add({
        userId,
        productId: Number(spu.id),
        skuId: Number(selectedSku.id),
        quantity,
      })
      message.success('已加入购物车')
    } catch (error) {
      message.error(toApiError(error).message)
    }
  }

  if (spuQuery.isLoading) {
    return (
      <div className="grid gap-8 lg:grid-cols-[1.05fr_1fr]">
        <div className="aspect-square animate-pulse rounded-2xl bg-[var(--color-bg-sunken)]" />
        <div className="space-y-4">
          <div className="h-8 w-2/3 animate-pulse rounded bg-[var(--color-bg-sunken)]" />
          <div className="h-5 w-1/3 animate-pulse rounded bg-[var(--color-bg-sunken)]" />
          <div className="h-24 animate-pulse rounded bg-[var(--color-bg-sunken)]" />
        </div>
      </div>
    )
  }

  if (spuQuery.isError || !spu) {
    return (
      <div className="rounded-xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-20 text-center">
        <p className="text-sm font-medium">商品不存在或已下架</p>
        <Button asChild variant="outline" className="mt-4">
          <Link href="/shop">返回商城</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-10">
      <div className="flex items-center gap-2 text-[12px] text-[var(--color-text-secondary)]">
        <Link href="/shop" className="inline-flex items-center gap-1 hover:text-[var(--color-primary-text)]">
          <ArrowLeft className="size-3.5" />
          商城首页
        </Link>
        <span>/</span>
        <span className="truncate">{spu.spuName}</span>
      </div>

      <div className="grid gap-8 lg:grid-cols-[1.05fr_1fr]">
        <div className="space-y-3">
          <div className="flex aspect-square items-center justify-center overflow-hidden rounded-2xl border border-[var(--color-border-secondary)] bg-[var(--color-bg-sunken)]">
            {gallery[0] ? (
              <img src={gallery[0]} alt={spu.spuName ?? '商品'} className="size-full object-cover" />
            ) : (
              <span className="font-serif text-6xl text-[var(--color-primary-text)]/30">
                {(spu.spuName ?? 'S').slice(0, 1)}
              </span>
            )}
          </div>
          {gallery.length > 1 && (
            <div className="flex gap-2 overflow-x-auto">
              {gallery.slice(0, 6).map((url) => (
                <img
                  key={url}
                  src={url}
                  alt=""
                  className="size-16 shrink-0 rounded-lg border border-[var(--color-border-secondary)] object-cover"
                />
              ))}
            </div>
          )}
        </div>

        <div className="space-y-5">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              {spu.brandId && <Badge variant="secondary">品牌 {spu.brandId}</Badge>}
              {spu.status === 1 ? (
                <Badge variant="success">在售</Badge>
              ) : (
                <Badge variant="secondary">未上架</Badge>
              )}
            </div>
            <h1 className="text-[26px] leading-tight font-semibold tracking-[-0.02em]">
              {spu.spuName ?? '未命名商品'}
            </h1>
            <p className="font-mono text-[11px] text-[var(--color-text-tertiary)]">
              {spu.spuCode}
            </p>
          </div>

          <div className="rounded-2xl bg-[var(--color-primary-soft)] px-5 py-4">
            <p className="text-[10px] font-medium tracking-[0.14em] text-[var(--color-primary-text)] uppercase">
              PRICE
            </p>
            <p className="mt-1 text-[30px] leading-none font-semibold text-[var(--color-primary-text)]">
              ¥{Number(price ?? 0).toFixed(2)}
            </p>
            <p className="mt-1 text-[11px] text-[var(--color-text-secondary)]">
              库存 {stock} 件 · 累计销量 {spu.totalSales ?? 0}
            </p>
          </div>

          {skus.length > 0 && (
            <div className="space-y-2">
              <p className="text-[12px] font-medium">选择规格</p>
              <div className="flex flex-wrap gap-2">
                {skus.map((sku) => {
                  const active = String(sku.id) === String(selectedSku?.id)
                  return (
                    <button
                      key={sku.id}
                      type="button"
                      onClick={() => setSelectedSkuId(String(sku.id))}
                      className={`rounded-lg border px-3 py-2 text-left text-[11px] transition-colors ${
                        active
                          ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary-text)]'
                          : 'border-[var(--color-border-secondary)] bg-white hover:border-[var(--color-primary)]/40'
                      }`}
                    >
                      <span className="block font-medium">{sku.skuName ?? sku.skuCode}</span>
                      <span className="mt-0.5 block text-[10px] opacity-70">
                        ¥{Number(sku.sellingPrice ?? price).toFixed(2)} · 库存 {sku.availableStock ?? sku.stock ?? 0}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
          )}

          <div className="flex items-center gap-3">
            <span className="text-[12px] text-[var(--color-text-secondary)]">数量</span>
            <div className="flex items-center rounded-lg border border-[var(--color-border-secondary)] bg-white">
              <Button
                variant="ghost"
                size="icon"
                className="size-8 rounded-none"
                aria-label="减少"
                onClick={() => setQuantity((value) => Math.max(1, value - 1))}
              >
                <Minus className="size-3.5" />
              </Button>
              <Input
                value={quantity}
                onChange={(event) =>
                  setQuantity(Math.max(1, Number(event.target.value) || 1))
                }
                className="h-8 w-14 rounded-none border-0 text-center text-xs shadow-none"
              />
              <Button
                variant="ghost"
                size="icon"
                className="size-8 rounded-none"
                aria-label="增加"
                onClick={() =>
                  setQuantity((value) =>
                    stock > 0 ? Math.min(stock, value + 1) : value + 1
                  )
                }
              >
                <Plus className="size-3.5" />
              </Button>
            </div>
          </div>

          <div className="flex gap-2">
            <Button
              className="h-11 flex-1"
              disabled={!selectedSku || stock === 0}
              onClick={() => void addToCart()}
            >
              <ShoppingCart />
              {stock === 0 ? '暂时缺货' : '加入购物车'}
            </Button>
            <Button
              asChild
              variant="outline"
              className="h-11"
            >
              <Link href="/shop/cart">
                <Check />
                去购物车
              </Link>
            </Button>
          </div>

          {spu.description && (
            <>
              <Separator />
              <div className="space-y-1">
                <p className="text-[12px] font-medium">商品描述</p>
                <p className="text-[12px] leading-6 whitespace-pre-line text-[var(--color-text-secondary)]">
                  {spu.description}
                </p>
              </div>
            </>
          )}
        </div>
      </div>

      {(similarQuery.data?.length ?? 0) > 0 && (
        <section className="space-y-4">
          <div>
            <p className="text-[10px] font-medium tracking-[0.16em] text-[var(--color-text-tertiary)] uppercase">
              YOU MAY ALSO LIKE
            </p>
            <h2 className="mt-1 text-[20px] font-semibold">相似商品</h2>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {similarQuery.data!.map((product) => (
              <ProductCard
                key={product.id ?? product.productId}
                product={{
                  id: String(product.productId),
                  spuName: product.title,
                  mainImage: product.mainImage,
                  minPrice: product.price,
                  maxPrice: product.price,
                  totalSales: product.salesCount,
                  totalStock: undefined,
                  brandName: product.brandName,
                  categoryName: product.categoryPath,
                }}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

