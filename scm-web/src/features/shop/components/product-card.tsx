import Link from 'next/link'

import { cn } from '@/lib/utils'
import type { ProductSearchResult } from '@/lib/api/contracts/product'

function priceLabel(product: ProductSearchResult) {
  const min = product.minPrice
  const max = product.maxPrice
  if (min === null || min === undefined) return '价格待定'
  if (max !== null && max !== undefined && max !== min) {
    return `¥${Number(min).toFixed(2)} - ¥${Number(max).toFixed(2)}`
  }
  return `¥${Number(min).toFixed(2)}`
}

export function ProductCard({
  product,
  className,
}: {
  product: ProductSearchResult
  className?: string
}) {
  return (
    <Link
      href={`/shop/product/${product.id}`}
      className={cn(
        'group flex flex-col overflow-hidden rounded-xl border border-[var(--color-border-secondary)] bg-white transition-all hover:-translate-y-0.5 hover:border-[var(--color-primary)]/40 hover:shadow-[0_10px_30px_-18px_rgba(108,27,253,0.35)]',
        className
      )}
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-[var(--color-bg-sunken)]">
        {product.mainImage ? (
          <img
            src={product.mainImage}
            alt={product.spuName ?? '商品'}
            className="size-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex size-full items-center justify-center bg-[linear-gradient(135deg,var(--color-primary-soft),var(--color-bg-sunken))]">
            <span className="font-serif text-4xl text-[var(--color-primary-text)]/40">
              {(product.spuName ?? 'S').slice(0, 1)}
            </span>
          </div>
        )}
        {product.totalStock === 0 && (
          <span className="absolute top-2 left-2 rounded-full bg-black/70 px-2 py-0.5 text-[10px] text-white">
            缺货
          </span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-1.5 p-3.5">
        <h3 className="line-clamp-2 min-h-[38px] text-[13px] leading-[1.45] font-medium text-foreground group-hover:text-[var(--color-primary-text)]">
          {product.spuName ?? '未命名商品'}
        </h3>
        <div className="text-[11px] text-[var(--color-text-tertiary)]">
          {product.brandName || product.categoryName || product.spuCode || ' '}
        </div>
        <div className="mt-auto flex items-end justify-between gap-2 pt-1">
          <span className="text-[15px] font-semibold text-[var(--color-primary-text)]">
            {priceLabel(product)}
          </span>
          <span className="text-[10px] text-[var(--color-text-tertiary)]">
            已售 {product.totalSales ?? 0}
          </span>
        </div>
      </div>
    </Link>
  )
}
