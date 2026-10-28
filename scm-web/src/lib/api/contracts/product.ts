/**
 * scm-product returns its own `PageResult` (not the MyBatis-Plus `Page` echo
 * used by scm-order/scm-inventory): the page number field is `pageNum`.
 */
export interface ProductPage<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
  records: T[];
}

/** SPU/SKU status codes: 0 draft, 1 on sale, 2 off sale, 3 deleted. */
export type ProductStatus = 0 | 1 | 2 | 3;

export interface ProdSpuDTO {
  id: string;
  spuCode?: string | null;
  spuName?: string | null;
  categoryId?: string | null;
  brandId?: string | null;
  description?: string | null;
  detailHtml?: string | null;
  images?: string | null;
  mainImage?: string | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  totalStock?: number | null;
  totalSales?: number | null;
  status?: number | null;
  publishedAt?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

export interface ProdSkuDTO {
  id: string;
  spuId?: string | null;
  skuCode?: string | null;
  skuName?: string | null;
  /** Stored as a JSON object string. */
  attributes?: string | null;
  originalPrice?: number | null;
  sellingPrice?: number | null;
  costPrice?: number | null;
  stock?: number | null;
  availableStock?: number | null;
  lockedStock?: number | null;
  salesCount?: number | null;
  weight?: number | null;
  volume?: number | null;
  barcode?: string | null;
  imageUrl?: string | null;
  /** Comma-separated image URLs. */
  images?: string | null;
  status?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProdCategoryDTO {
  id: string;
  categoryCode?: string | null;
  categoryName?: string | null;
  /** Root categories use the sentinel `"0"`. */
  parentId?: string | null;
  level?: number | null;
  sortOrder?: number | null;
  iconUrl?: string | null;
  imageUrl?: string | null;
  description?: string | null;
  isLeaf?: boolean | null;
  enabled?: boolean | null;
  seoTitle?: string | null;
  seoKeywords?: string | null;
  seoDescription?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProdBrandDTO {
  id: string;
  brandCode?: string | null;
  brandName?: string | null;
  brandNameEn?: string | null;
  logoUrl?: string | null;
  description?: string | null;
  website?: string | null;
  country?: string | null;
  establishedYear?: number | null;
  featured?: boolean | null;
  sortOrder?: number | null;
  enabled?: boolean | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/**
 * `attributes` is a JSON array string of `{name, type, required, options}`
 * descriptors; the backend stores it opaquely and never parses it.
 */
export interface ProdAttributeTemplateDTO {
  id: string;
  templateName?: string | null;
  categoryId?: string | null;
  attributes?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProdSpuQuery {
  page: number;
  size: number;
  keyword?: string;
  categoryId?: string;
  brandId?: string;
  status?: ProductStatus;
}

export interface ProdSkuQuery {
  page: number;
  size: number;
  spuId?: string;
}

export interface ProdCategoryQuery {
  page: number;
  size: number;
  name?: string;
}

export interface ProdBrandQuery {
  page: number;
  size: number;
  name?: string;
}

export interface ProdAttributeTemplateQuery {
  page: number;
  size: number;
  categoryId?: string;
}

/** Body of `POST /products/search` — price range and sorting are server-side. */
export interface ProductSearchRequest {
  keyword?: string;
  categoryId?: string;
  brandId?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

/** Flattened SPU row returned by the search endpoints, with names already joined. */
export interface ProductSearchResult {
  id: string;
  spuCode?: string | null;
  spuName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  brandId?: string | null;
  brandName?: string | null;
  description?: string | null;
  mainImage?: string | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  totalStock?: number | null;
  totalSales?: number | null;
  sortOrder?: number | null;
  status?: number | null;
  publishedAt?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/**
 * Create/update body — only the SPU columns the product screen edits. The
 * backend fills id/tenant/timestamps itself.
 */
export interface ProdSpuPayload {
  spuCode: string;
  spuName: string;
  categoryId: string;
  brandId?: string;
  description?: string;
  minPrice: number;
  maxPrice: number;
  status: ProductStatus;
}

/** `POST/PUT /prod-sku` body — the writable subset of {@link ProdSkuDTO}. */
export interface ProdSkuPayload {
  spuId: string;
  skuCode: string;
  skuName: string;
  attributes?: string;
  originalPrice?: number;
  sellingPrice: number;
  costPrice?: number;
  stock?: number;
  barcode?: string;
  imageUrl?: string;
  images?: string;
  weight?: number;
  volume?: number;
  status: ProductStatus;
}

/** `POST/PUT /prod-category` body — the writable subset of {@link ProdCategoryDTO}. */
export interface ProdCategoryPayload {
  categoryCode: string;
  categoryName: string;
  parentId?: string;
  level?: number;
  sortOrder?: number;
  iconUrl?: string;
  imageUrl?: string;
  description?: string;
  isLeaf?: boolean;
  enabled?: boolean;
}

/** `POST/PUT /prod-brand` body — the writable subset of {@link ProdBrandDTO}. */
export interface ProdBrandPayload {
  brandCode: string;
  brandName: string;
  brandNameEn?: string;
  logoUrl?: string;
  description?: string;
  website?: string;
  country?: string;
  establishedYear?: number;
  featured?: boolean;
  sortOrder?: number;
  enabled?: boolean;
}

/** `POST/PUT /prod-attribute-template` body. */
export interface ProdAttributeTemplatePayload {
  templateName: string;
  categoryId: string;
  /** JSON array string, see {@link ProdAttributeTemplateDTO.attributes}. */
  attributes: string;
  remark?: string;
}
