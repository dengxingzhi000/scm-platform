import { mallApi } from '@/lib/api/services/ecommerce'
import { productApiService } from '@/lib/api/services/product'
import type { CartItem } from '@/lib/api/contracts/ecommerce'
import type { ProdSkuDTO, ProdSpuDTO } from '@/lib/api/contracts/product'

export interface ShopCartLine {
  item: CartItem
  spu?: ProdSpuDTO
  sku?: ProdSkuDTO
  name: string
  skuName?: string
  image?: string
  price: number
}

export function skuImage(spu: ProdSpuDTO | undefined, sku: ProdSkuDTO | undefined) {
  const candidates = [sku?.imageUrl, spu?.mainImage, ...(spu?.images ?? '').split(',')]
  return candidates.find((value) => !!value && value.trim())?.trim()
}

/**
 * Cart rows only carry ids, so names/prices are joined from scm-product in
 * parallel. A missing product (deleted SPU) is tolerated instead of failing
 * the whole cart.
 */
export async function loadCartLines(userId: string): Promise<ShopCartLine[]> {
  const items = await mallApi.cart.get(userId)
  if (items.length === 0) return []

  const uniqueProductIds = [...new Set(items.map((item) => item.productId))]
  const products = new Map<number, { spu: ProdSpuDTO | null; skus: ProdSkuDTO[] }>()

  await Promise.all(
    uniqueProductIds.map(async (productId) => {
      try {
        const [spu, skus] = await Promise.all([
          productApiService.getSpu(String(productId)),
          productApiService.listSkusBySpu(String(productId)).catch(() => []),
        ])
        products.set(productId, { spu, skus })
      } catch {
        products.set(productId, { spu: null, skus: [] })
      }
    })
  )

  return items.map((item) => {
    const entry = products.get(item.productId)
    const sku = entry?.skus.find((candidate) => String(candidate.id) === String(item.skuId))
    const price = sku?.sellingPrice ?? entry?.spu?.minPrice ?? 0
    return {
      item,
      spu: entry?.spu ?? undefined,
      sku,
      name: entry?.spu?.spuName ?? `商品 ${item.productId}`,
      skuName: sku?.skuName ?? undefined,
      image: skuImage(entry?.spu ?? undefined, sku),
      price,
    }
  })
}

export function cartTotal(lines: ShopCartLine[]) {
  return lines.reduce((sum, line) => sum + line.price * line.item.quantity, 0)
}
