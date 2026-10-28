import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  ProdAttributeTemplateDTO,
  ProdAttributeTemplatePayload,
  ProdAttributeTemplateQuery,
  ProdBrandDTO,
  ProdBrandPayload,
  ProdBrandQuery,
  ProdCategoryDTO,
  ProdCategoryPayload,
  ProdCategoryQuery,
  ProdSkuDTO,
  ProdSkuPayload,
  ProdSkuQuery,
  ProdSpuDTO,
  ProdSpuPayload,
  ProdSpuQuery,
  ProductPage,
  ProductSearchRequest,
  ProductSearchResult,
} from "../contracts/product";
import type { SpringPage } from "../contracts/page";

/**
 * scm-product is reached through the gateway's Nacos discovery locator
 * (`/scm-<service>/**` with the first segment stripped).
 */
const PRODUCT_BASE = "/scm-product/api/v1";

/** One large page keeps the category/brand catalogs in a single round trip. */
const CATALOG_PAGE_SIZE = 1000;

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.post(url, data));

const put = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.put(url, data));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const productApiService = {
  querySpus: (query: ProdSpuQuery) =>
    get<ProductPage<ProdSpuDTO>>(`${PRODUCT_BASE}/prod-spu`, query),
  getSpu: (id: string) =>
    get<ProdSpuDTO | null>(`${PRODUCT_BASE}/prod-spu/${id}`),
  createSpu: (payload: ProdSpuPayload) =>
    post<void>(`${PRODUCT_BASE}/prod-spu`, payload),
  updateSpu: (id: string, payload: ProdSpuPayload) =>
    put<void>(`${PRODUCT_BASE}/prod-spu/${id}`, payload),
  deleteSpu: (id: string) => del<void>(`${PRODUCT_BASE}/prod-spu/${id}`),

  listSkusBySpu: (spuId: string) =>
    get<ProdSkuDTO[]>(`${PRODUCT_BASE}/prod-sku/spu/${spuId}`),

  listSpusByCategory: (categoryId: string) =>
    get<ProdSpuDTO[]>(`${PRODUCT_BASE}/prod-spu/category/${categoryId}`),

  listSpusByBrand: (brandId: string) =>
    get<ProdSpuDTO[]>(`${PRODUCT_BASE}/prod-spu/brand/${brandId}`),

  /** `/prod-category/tree` only returns the roots, so the full catalog is fetched instead. */
  listCategories: () =>
    get<ProductPage<ProdCategoryDTO>>(`${PRODUCT_BASE}/prod-category`, {
      page: 1,
      size: CATALOG_PAGE_SIZE,
    }),

  listBrands: () =>
    get<ProductPage<ProdBrandDTO>>(`${PRODUCT_BASE}/prod-brand`, {
      page: 1,
      size: CATALOG_PAGE_SIZE,
    }),

  sku: {
    list: (query: ProdSkuQuery) =>
      get<ProductPage<ProdSkuDTO>>(`${PRODUCT_BASE}/prod-sku`, query),
    get: (id: string) => get<ProdSkuDTO>(`${PRODUCT_BASE}/prod-sku/${id}`),
    create: (payload: ProdSkuPayload) =>
      post<void>(`${PRODUCT_BASE}/prod-sku`, payload),
    update: (id: string, payload: ProdSkuPayload) =>
      put<void>(`${PRODUCT_BASE}/prod-sku/${id}`, payload),
    delete: (id: string) => del<void>(`${PRODUCT_BASE}/prod-sku/${id}`),
  },

  category: {
    list: (query: ProdCategoryQuery) =>
      get<ProductPage<ProdCategoryDTO>>(`${PRODUCT_BASE}/prod-category`, query),
    get: (id: string) =>
      get<ProdCategoryDTO>(`${PRODUCT_BASE}/prod-category/${id}`),
    create: (payload: ProdCategoryPayload) =>
      post<void>(`${PRODUCT_BASE}/prod-category`, payload),
    update: (id: string, payload: ProdCategoryPayload) =>
      put<void>(`${PRODUCT_BASE}/prod-category/${id}`, payload),
    delete: (id: string) => del<void>(`${PRODUCT_BASE}/prod-category/${id}`),
    /** Returns root categories only; nested levels are not attached. */
    tree: () => get<ProdCategoryDTO[]>(`${PRODUCT_BASE}/prod-category/tree`),
    search: (name: string) =>
      get<ProdCategoryDTO[]>(`${PRODUCT_BASE}/prod-category/search`, { name }),
    children: (parentId: string) =>
      get<ProdCategoryDTO[]>(
        `${PRODUCT_BASE}/prod-category/children/${parentId}`
      ),
  },

  brand: {
    list: (query: ProdBrandQuery) =>
      get<ProductPage<ProdBrandDTO>>(`${PRODUCT_BASE}/prod-brand`, query),
    get: (id: string) => get<ProdBrandDTO>(`${PRODUCT_BASE}/prod-brand/${id}`),
    create: (payload: ProdBrandPayload) =>
      post<void>(`${PRODUCT_BASE}/prod-brand`, payload),
    update: (id: string, payload: ProdBrandPayload) =>
      put<void>(`${PRODUCT_BASE}/prod-brand/${id}`, payload),
    delete: (id: string) => del<void>(`${PRODUCT_BASE}/prod-brand/${id}`),
    search: (name: string) =>
      get<ProdBrandDTO[]>(`${PRODUCT_BASE}/prod-brand/search`, { name }),
    featured: () => get<ProdBrandDTO[]>(`${PRODUCT_BASE}/prod-brand/featured`),
  },

  attributeTemplate: {
    list: (query: ProdAttributeTemplateQuery) =>
      get<ProductPage<ProdAttributeTemplateDTO>>(
        `${PRODUCT_BASE}/prod-attribute-template`,
        query
      ),
    get: (id: string) =>
      get<ProdAttributeTemplateDTO>(
        `${PRODUCT_BASE}/prod-attribute-template/${id}`
      ),
    create: (payload: ProdAttributeTemplatePayload) =>
      post<void>(`${PRODUCT_BASE}/prod-attribute-template`, payload),
    update: (id: string, payload: ProdAttributeTemplatePayload) =>
      put<void>(`${PRODUCT_BASE}/prod-attribute-template/${id}`, payload),
    delete: (id: string) =>
      del<void>(`${PRODUCT_BASE}/prod-attribute-template/${id}`),
    byCategory: (categoryId: string) =>
      get<ProdAttributeTemplateDTO[]>(
        `${PRODUCT_BASE}/prod-attribute-template/category/${categoryId}`
      ),
  },

  /**
   * Search is served by a different code path than the CRUD endpoints and
   * returns a Spring Data `Page` (`content`/`totalElements`) rather than the
   * service's own `PageResult`.
   */
  search: {
    quick: (params: {
      keyword: string;
      page?: number;
      size?: number;
      sortBy?: string;
      sortOrder?: 'asc' | 'desc';
    }) =>
      get<SpringPage<ProductSearchResult>>(
        `${PRODUCT_BASE}/products/search`,
        params
      ),
    advanced: (request: ProductSearchRequest) =>
      post<SpringPage<ProductSearchResult>>(
        `${PRODUCT_BASE}/products/search`,
        request
      ),
    latest: (params?: { page?: number; size?: number }) =>
      get<SpringPage<ProductSearchResult>>(
        `${PRODUCT_BASE}/products/search/latest`,
        params
      ),
    hot: (params?: { page?: number; size?: number }) =>
      get<SpringPage<ProductSearchResult>>(
        `${PRODUCT_BASE}/products/search/hot`,
        params
      ),
    byCategory: (categoryId: string, params?: { page?: number; size?: number }) =>
      get<SpringPage<ProductSearchResult>>(
        `${PRODUCT_BASE}/products/search/category/${categoryId}`,
        params
      ),
    byBrand: (brandId: string, params?: { page?: number; size?: number }) =>
      get<SpringPage<ProductSearchResult>>(
        `${PRODUCT_BASE}/products/search/brand/${brandId}`,
        params
      ),
  },
};
