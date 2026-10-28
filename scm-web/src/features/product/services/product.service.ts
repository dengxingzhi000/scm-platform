import { productApiService } from "@/lib/api/services/product";

import {
  spuStatusToApi,
  toBrand,
  toCategoryTree,
  toProduct,
  toProductPage,
  toSpuPayload,
} from "../lib/mappers";
import type { ProductFormPayload } from "../lib/product-form-schema";
import type {
  Brand,
  Category,
  PageResult,
  Product,
  ProductSearchParams,
} from "../types";

export const productService = {
  list: async (params?: ProductSearchParams): Promise<PageResult<Product>> => {
    const [page, categories, brands] = await Promise.all([
      productApiService.querySpus({
        page: params?.current ?? 1,
        size: params?.pageSize ?? 20,
        keyword: params?.keyword || undefined,
        categoryId: params?.categoryId || undefined,
        brandId: params?.brandId || undefined,
        status: spuStatusToApi(params?.status),
      }),
      productApiService.listCategories(),
      productApiService.listBrands(),
    ]);

    return toProductPage(page, {
      categories: categories.records,
      brands: brands.records,
    });
  },

  /** SPU rows carry no line items, so the SKU list is a second request. */
  getById: async (id: string): Promise<Product> => {
    const [spu, skus, categories, brands] = await Promise.all([
      productApiService.getSpu(id),
      productApiService.listSkusBySpu(id).catch(() => []),
      productApiService.listCategories(),
      productApiService.listBrands(),
    ]);

    if (!spu) throw new Error("商品不存在");

    return toProduct(spu, {
      skus,
      categories: categories.records,
      brands: brands.records,
    });
  },

  create: async (data: ProductFormPayload): Promise<void> => {
    await productApiService.createSpu(toSpuPayload(data));
  },

  update: async (id: string, data: ProductFormPayload): Promise<void> => {
    await productApiService.updateSpu(id, toSpuPayload(data));
  },

  delete: (id: string): Promise<void> => productApiService.deleteSpu(id),

  listCategories: async (): Promise<Category[]> =>
    toCategoryTree((await productApiService.listCategories()).records),

  listBrands: async (): Promise<Brand[]> =>
    (await productApiService.listBrands()).records.map(toBrand),
};
