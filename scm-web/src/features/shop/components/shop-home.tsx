'use client'

import { useMemo, useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Search, Sparkles } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { productApiService } from '@/lib/api/services/product'
import { ProductCard } from './product-card'

const PAGE_SIZE = 12

export default function ShopHome() {
  const [keyword, setKeyword] = useState('')

  const latestQuery = useQuery({
    queryKey: ['shop', 'home', 'latest'],
    queryFn: () => productApiService.search.latest({ page: 1, size: PAGE_SIZE }),
  })

  const hotQuery = useQuery({
    queryKey: ['shop', 'home', 'hot'],
    queryFn: () => productApiService.search.hot({ page: 1, size: PAGE_SIZE }),
  })

  const categoriesQuery = useQuery({
    queryKey: ['shop', 'home', 'categories'],
    queryFn: () => productApiService.listCategories(),
  })

  const categories = useMemo(
    () => (categoriesQuery.data?.records ?? []).slice(0, 10),
    [categoriesQuery.data]
  )

  return (
    <div className="space-y-10">
      <section className="relative overflow-hidden rounded-2xl border border-[var(--color-border-secondary)] bg-white px-8 py-12 sm:px-12">
        <div
          aria-hidden
          className="pointer-events-none absolute -top-24 -right-20 size-72 rounded-full bg-[radial-gradient(circle,var(--color-primary-soft),transparent_70%)]"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute -bottom-28 left-1/3 size-64 rounded-full bg-[radial-gradient(circle,rgba(136,66,253,0.10),transparent_70%)]"
        />
        <div className="relative max-w-2xl">
          <p className="mb-3 inline-flex items-center gap-1.5 rounded-full bg-[var(--color-primary-soft)] px-3 py-1 text-[11px] font-medium tracking-[0.08em] text-[var(--color-primary-text)] uppercase">
            <Sparkles className="size-3.5" />
            SCM Mall
          </p>
          <h1 className="font-serif text-[34px] leading-[1.15] font-semibold tracking-[-0.02em] text-foreground sm:text-[42px]">
            好商品，从这里出发
          </h1>
          <p className="mt-3 max-w-lg text-[13px] leading-6 text-[var(--color-text-secondary)]">
            商品、库存、购物车与订单实时联动。搜索全站商品，加入购物车，提交订单并创建支付单。
          </p>

          <form
            className="mt-7 flex max-w-xl items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault()
              const trimmed = keyword.trim()
              window.location.href = trimmed
                ? `/shop/search?keyword=${encodeURIComponent(trimmed)}`
                : '/shop/search'
            }}
          >
            <div className="relative flex-1">
              <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-[var(--color-text-tertiary)]" />
              <Input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="搜索商品名称、编码或品牌"
                className="h-11 bg-white pl-9 text-sm"
              />
            </div>
            <Button type="submit" className="h-11 px-5">
              搜索
            </Button>
          </form>

          {categories.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-2">
              {categories.map((category) => (
                <Link
                  key={category.id}
                  href={`/shop/search?categoryId=${category.id}`}
                  className="rounded-full border border-[var(--color-border-secondary)] bg-white px-3 py-1 text-[11px] text-[var(--color-text-secondary)] transition-colors hover:border-[var(--color-primary)]/50 hover:text-[var(--color-primary-text)]"
                >
                  {category.categoryName}
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      <ProductSection
        eyebrow="NEW ARRIVALS"
        title="最新上架"
        description="最近发布的商品，按发布时间倒序"
        products={latestQuery.data?.content ?? []}
        loading={latestQuery.isLoading}
        error={latestQuery.isError}
        actionHref="/shop/search"
      />

      <ProductSection
        eyebrow="BEST SELLERS"
        title="热销商品"
        description="按累计销量排序，看看大家都在买什么"
        products={hotQuery.data?.content ?? []}
        loading={hotQuery.isLoading}
        error={hotQuery.isError}
        actionHref="/shop/search"
      />
    </div>
  )
}

function ProductSection({
  eyebrow,
  title,
  description,
  products,
  loading,
  error,
  actionHref,
}: {
  eyebrow: string
  title: string
  description: string
  products: import('@/lib/api/contracts/product').ProductSearchResult[]
  loading: boolean
  error: boolean
  actionHref: string
}) {
  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-[10px] font-medium tracking-[0.16em] text-[var(--color-text-tertiary)] uppercase">
            {eyebrow}
          </p>
          <h2 className="mt-1 text-[20px] font-semibold tracking-[-0.01em]">{title}</h2>
          <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">{description}</p>
        </div>
        <Link
          href={actionHref}
          className="inline-flex items-center gap-1 text-[12px] font-medium text-[var(--color-primary-text)] hover:underline"
        >
          查看全部
          <ArrowRight className="size-3.5" />
        </Link>
      </div>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div
              key={index}
              className="h-64 animate-pulse rounded-xl border border-[var(--color-border-secondary)] bg-[var(--color-bg-sunken)]"
            />
          ))}
        </div>
      ) : error ? (
        <StateBlock text="商品服务暂不可用，请确认 scm-product 已启动。" />
      ) : products.length === 0 ? (
        <StateBlock text="暂无商品，请先在运营后台创建并上架商品。" />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </section>
  )
}

function StateBlock({ text }: { text: string }) {
  return (
    <div className="rounded-xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-12 text-center text-[12px] text-[var(--color-text-secondary)]">
      {text}
    </div>
  )
}
