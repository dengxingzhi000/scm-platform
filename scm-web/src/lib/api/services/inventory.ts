import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  InventoryAdjustRequest,
  InventoryPage,
  InventoryQueryRequest,
  InventoryReservationRequest,
  InventoryResponse,
  InventoryStatsResponse,
  InventoryTransferRequest,
} from "../contracts/inventory";

const INVENTORY_BASE = "/scm-inventory/api/v1/inventory";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, data?: unknown, params?: object) =>
  unwrap<T>(apiClient.post(url, data, params ? { params } : undefined));

const put = <T>(url: string) => unwrap<T>(apiClient.put(url));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const inventoryApiService = {
  /** Single-warehouse lookup; both ids are mandatory and a missing row answers code 404. */
  getInventory: (skuId: string, warehouseId: string) =>
    get<InventoryResponse | null>(INVENTORY_BASE, { skuId, warehouseId }),
  batchQuery: (skuIds: string[], warehouseId?: string) =>
    post<InventoryResponse[]>(`${INVENTORY_BASE}/batch`, skuIds, { warehouseId }),
  query: (request: InventoryQueryRequest) =>
    post<InventoryPage>(`${INVENTORY_BASE}/query`, request),
  adjust: (request: InventoryAdjustRequest) =>
    post<InventoryResponse>(`${INVENTORY_BASE}/adjust`, request),
  transfer: (request: InventoryTransferRequest) =>
    post<boolean>(`${INVENTORY_BASE}/transfer`, request),
  hasStock: (skuId: string, warehouseId: string, quantity: number) =>
    get<boolean>(`${INVENTORY_BASE}/check`, { skuId, warehouseId, quantity }),
  stats: () => get<InventoryStatsResponse>(`${INVENTORY_BASE}/stats`),
  initStock: (skuId: string, warehouseId: string, initialStock: number) =>
    post<InventoryResponse>(`${INVENTORY_BASE}/init`, undefined, {
      skuId,
      warehouseId,
      initialStock,
    }),

  // The reservation controller returns bare JSON literals, not ApiResponse.
  reserve: (request: InventoryReservationRequest) =>
    post<boolean>(`${INVENTORY_BASE}/reservation/reserve`, request),
  confirmReservation: (businessKey: string) =>
    put<boolean>(`${INVENTORY_BASE}/reservation/confirm/${businessKey}`),
  releaseReservation: (businessKey: string) =>
    del<boolean>(`${INVENTORY_BASE}/reservation/release/${businessKey}`),
  reservationExists: (businessKey: string) =>
    get<boolean>(`${INVENTORY_BASE}/reservation/check/${businessKey}`),
  reservationQuantity: (businessKey: string) =>
    get<number>(`${INVENTORY_BASE}/reservation/quantity/${businessKey}`),
};
