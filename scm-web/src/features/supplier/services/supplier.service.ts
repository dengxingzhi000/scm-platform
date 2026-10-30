import { supplierApiService } from "@/lib/api/services/supplier";

import { toSupplier, toSupplierPage } from "../lib/mappers";
import type { PageResult, Supplier, SupplierSearchParams } from "../types";

export const supplierService = {
  list: async (params?: SupplierSearchParams): Promise<PageResult<Supplier>> =>
    toSupplierPage(
      await supplierApiService.list({
        page: params?.current ?? 1,
        size: params?.pageSize ?? 20,
        keyword: params?.keyword || undefined,
        enabled:
          params?.status === "active"
            ? true
            : params?.status === "disabled"
              ? false
              : undefined,
      })
    ),

  getById: async (id: string): Promise<Supplier> =>
    toSupplier(await supplierApiService.getById(id)),
};
