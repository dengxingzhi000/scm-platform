import { v4 as uuidv4 } from 'uuid'

import { inventoryApiService } from '@/lib/api/services/inventory'
import { toInventoryItem, toInventoryPage, toStockAlerts, toStockStatus } from '../lib/mappers'
import type {
  InventoryItem,
  InventorySearchParams,
  PageResult,
  StockAdjustment,
  StockAlert,
} from '../types'

const ALERT_PAGE_SIZE = 200

export const inventoryService = {
  list: async (params?: InventorySearchParams): Promise<PageResult<InventoryItem>> =>
    toInventoryPage(
      await inventoryApiService.query({
        page: params?.current ?? 1,
        size: params?.pageSize ?? 20,
        skuIds: params?.skuIds?.length ? params.skuIds : undefined,
        warehouseIds: params?.warehouseIds?.length ? params.warehouseIds : undefined,
        locationCode: params?.locationCode || undefined,
        stockStatus: toStockStatus(params?.status),
        sortBy: params?.sortBy ?? 'update_time',
        sortOrder: params?.sortOrder ?? 'DESC',
      })
    ),

  /** `InvAlertController` has no endpoints, so warnings are computed from the stock query. */
  getAlerts: async (): Promise<StockAlert[]> => {
    const [low, out] = await Promise.all([
      inventoryApiService.query({ stockStatus: 'LOW_STOCK', page: 1, size: ALERT_PAGE_SIZE }),
      inventoryApiService.query({ stockStatus: 'OUT_OF_STOCK', page: 1, size: ALERT_PAGE_SIZE }),
    ]);
    return toStockAlerts([...toInventoryPage(low).records, ...toInventoryPage(out).records]);
  },

  getStock: (skuId: string, warehouseId: string) =>
    inventoryApiService.getInventory(skuId, warehouseId).then((dto) => (dto ? toInventoryItem(dto) : null)),

  /** `referenceNo` is the server-side 24h idempotency key, so every submission gets its own. */
  adjustStock: async (adjustment: StockAdjustment): Promise<InventoryItem> =>
    toInventoryItem(
      await inventoryApiService.adjust({
        skuId: adjustment.skuId,
        warehouseId: adjustment.warehouseId,
        quantity: adjustment.quantity,
        adjustType: adjustment.adjustType,
        referenceNo: uuidv4(),
        remark: adjustment.remark,
      })
    ),

  transfer: (request: {
    skuId: string;
    fromWarehouseId: string;
    toWarehouseId: string;
    quantity: number;
    remark?: string;
  }) =>
    inventoryApiService.transfer({
      ...request,
      transferNo: uuidv4(),
    }),

  stats: () => inventoryApiService.stats(),
};
