export interface Product {
  id: string
  name: string
  code: string
  categoryId: string
  categoryName: string
  brandId?: string
  brandName?: string
  description?: string
  status: 'on_sale' | 'off_sale' | 'draft'
  price: number
  costPrice?: number
  imageUrl?: string
  skus: SKU[]
  createdAt: string
  updatedAt: string
}

export interface SKU {
  id: string
  productId: string
  skuCode: string
  name: string
  price: number
  costPrice?: number
  stock: number
  attributes?: Record<string, string>
  status: 'active' | 'disabled'
}

export interface Category {
  id: string
  name: string
  code: string
  parentId: string | null
  sort: number
  status: 'active' | 'disabled'
  children?: Category[]
}

export interface Brand {
  id: string
  name: string
  code: string
  logo?: string
  status: 'active' | 'disabled'
}

export interface ProductSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  categoryId?: string
  brandId?: string
  status?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
