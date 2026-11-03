import ShopProductDetail from '@/features/shop/components/shop-product-detail'

export default async function ShopProductPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return <ShopProductDetail productId={id} />
}
