import { purchaseApiService } from "@/lib/api/services/purchase";

import {
  purchaseStatusToApi,
  toPurchaseOrder,
  toPurchaseOrderPage,
} from "../lib/mappers";
import type {
  PageResult,
  PurchaseOrder,
  PurchaseSearchParams,
} from "../types";

const statusMap: Record<string, { label: string; color: string }> = {
  draft: { label: "草稿", color: "default" },
  pending: { label: "待审批", color: "warning" },
  approved: { label: "已审批", color: "processing" },
  ordered: { label: "已下单", color: "blue" },
  received: { label: "已收货", color: "purple" },
  completed: { label: "已完成", color: "success" },
  cancelled: { label: "已取消", color: "error" },
  exception: { label: "异常", color: "error" },
};

const loadItems = async (orderId: string) =>
  purchaseApiService.listItems(orderId).catch(() => []);

export const purchaseService = {
  list: async (
    params?: PurchaseSearchParams
  ): Promise<PageResult<PurchaseOrder>> => {
    const page = await purchaseApiService.page({
      page: params?.current ?? 1,
      size: params?.pageSize ?? 20,
      keyword: params?.keyword || undefined,
      status: purchaseStatusToApi(params?.status),
      supplierId: params?.supplierId || undefined,
    });

    const itemEntries = await Promise.all(
      page.records.map(async (record) => [record.id, await loadItems(record.id)] as const)
    );

    return toPurchaseOrderPage(page, Object.fromEntries(itemEntries));
  },

  getById: async (id: string): Promise<PurchaseOrder> => {
    const [order, items] = await Promise.all([
      purchaseApiService.getById(id),
      loadItems(id),
    ]);
    return toPurchaseOrder(order, items);
  },

  getStatusMap: () => statusMap,
};
