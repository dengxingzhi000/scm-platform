'use client'

import { useMemo, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Search } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { productApiService } from '@/lib/api/services/product'
import { ProductCard } from './product-card'

const PAGE_SIZE = 20
const ALL = '__all__'

export default function ShopSearch() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const categoryId = searchParams.get('categoryId') ?? undefined
  const brandId = searchParams.get('brandId') ?? undefined
  const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1)

  const [draft, setDraft] = useState(keyword)

  const categoriesQuery = useQuery({
    queryKey: ['shop', 'search', 'categories'],
    queryFn: () => productApiService.listCategories(),
  })

  const resultQuery = useQuery({
    queryKey: ['shop', 'search', keyword, categoryId, brandId, page],
    queryFn: () => {
      if (keyword) {
        return productApiService.search.quick({ keyword, page, size: PAGE_SIZE })
      }
      if (categoryId) {
        return productApiService.search.byCategory(categoryId, {
          page,
          size: PAGE_SIZE,
        })
      }
      if (brandId) {
        return productApiService.search.byBrand(brandId, {
          page,
          size: PAGE_SIZE,
        })
      }
      return productApiService.search.latest({ page, size: PAGE_SIZE })
    },
  })

  const categories = useMemo(
    () => categoriesQuery.data?.records ?? [],
    [categoriesQuery.data]
  )

  const goto = (next: Record<string, string | undefined>) => {
    const params = new URLSearchParams()
    if (next.keyword) params.set('keyword', next.keyword)
    if (next.categoryId) params.set('categoryId', next.categoryId)
    if (next.brandId) params.set('brandId', next.brandId)
    if (next.page) params.set('page', next.page)
    const query = params.toString()
    router.push(query ? `/shop/search?${query}` : '/shop/search')
  }

  const products = resultQuery.data?.content ?? []
  const total = resultQuery.data?.totalElements ?? 0
  const pageCount = Math.max(1, resultQuery.data?.totalPages ?? 1)
  const label = keyword
    ? `“${keyword}” 的搜索结果`
    : categoryId
      ? '分类商品'
      : brandId
        ? '品牌商品'
        : '全部商品'

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-[var(--color-border-secondary)] bg-white p-5">
        <div className="flex flex-wrap items-center gap-2">
          <form
            className="flex flex-1 items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault()
              goto({ keyword: draft.trim() || undefined })
            }}
          >
            <div className="relative min-w-[200px] flex-1">
              <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-[var(--color-text-tertiary)]" />
              <Input
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="搜索商品名称、编码或品牌"
                className="h-10 pl-9"
              />
            </div>
            <Button type="submit" variant="outline" className="h-10">
              搜索
            </Button>
          </form>

          <Select
            value={categoryId ?? ALL}
            onValueChange={(value) =>
              goto({
                keyword: keyword || undefined,
                categoryId: value === ALL ? undefined : value,
              })
            }
          >
            <SelectTrigger className="h-10 w-44">
              <SelectValue placeholder="全部分类" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部分类</SelectItem>
              {categories.map((category) => (
                <SelectItem key={category.id} value={category.id}>
                  {category.categoryName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex items-end justify-between gap-3">
        <div>
          <h1 className="text-[20px] font-semibold tracking-[-0.01em]">{label}</h1>
          <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
            共 {total} 件商品
          </p>
        </div>
      </div>

      {resultQuery.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, index) => (
            <div
              key={index}
              className="h-64 animate-pulse rounded-xl border border-[var(--color-border-secondary)] bg-[var(--color-bg-sunken)]"
            />
          ))}
        </div>
      ) : resultQuery.isError ? (
        <StateBlock text="商品查询失败，请确认 scm-product 服务已启动。" />
      ) : products.length === 0 ? (
        <StateBlock text="没有找到匹配的商品。" />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {pageCount > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 1}
            onClick={() => goto({ keyword: keyword || undefined, categoryId, brandId, page: String(page - 1) })}
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
            onClick={() => goto({ keyword: keyword || undefined, categoryId, brandId, page: String(page + 1) })}
          >
            下一页
            <ChevronRight />
          </Button>
        </div>
      )}
    </div>
  )
}

function StateBlock({ text }: { text: string }) {
  return (
    <div className="rounded-xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-16 text-center text-[12px] text-[var(--color-text-secondary)]">
      {text}
    </div>
  )
}
