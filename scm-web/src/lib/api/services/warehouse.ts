import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  WmsInboundDTO,
  WmsInboundItemDTO,
  WmsInboundPage,
  WmsOutboundDTO,
  WmsOutboundItemDTO,
  WmsOutboundPage,
  WmsWarehouseDTO,
  WmsWarehousePage,
  WmsWavePickingDTO,
  WmsWavePickingPage,
  WmsWavePickingQuery,
} from "../contracts/warehouse";

/**
 * scm-warehouse 的控制器没有 `/api/v1` 前缀，路径直接挂在服务根上。
 */
const WAREHOUSE_BASE = "/scm-warehouse";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.post(url, body));

const put = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.put(url, undefined, { params }));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const warehouseApiService = {
  pageWarehouses: (page: number, size: number, enabled?: boolean) =>
    get<WmsWarehousePage>(`${WAREHOUSE_BASE}/wms-warehouse/page`, {
      page,
      size,
      enabled,
    }),
  listWarehouses: () =>
    get<WmsWarehouseDTO[]>(`${WAREHOUSE_BASE}/wms-warehouse/list`),

  pageInbound: (page: number, size: number) =>
    get<WmsInboundPage>(`${WAREHOUSE_BASE}/wms-inbound/page`, { page, size }),
  listInboundItems: (inboundId: string) =>
    get<WmsInboundItemDTO[]>(`${WAREHOUSE_BASE}/wms-inbound-item/list`, {
      inboundId,
    }),

  pageOutbound: (page: number, size: number) =>
    get<WmsOutboundPage>(`${WAREHOUSE_BASE}/wms-outbound/page`, { page, size }),
  listOutboundItems: (outboundId: string) =>
    get<WmsOutboundItemDTO[]>(`${WAREHOUSE_BASE}/wms-outbound-item/list`, {
      outboundId,
    }),

  pageWavePicking: (query: WmsWavePickingQuery) =>
    get<WmsWavePickingPage>(`${WAREHOUSE_BASE}/wms-wave-picking/page`, query),
  createWavePicking: (entity: Partial<WmsWavePickingDTO>) =>
    post<WmsWavePickingDTO>(`${WAREHOUSE_BASE}/wms-wave-picking`, entity),
  startPicking: (id: string, pickerId: string, pickerName: string) =>
    put<void>(`${WAREHOUSE_BASE}/wms-wave-picking/${id}/start`, {
      pickerId,
      pickerName,
    }),
  completePicking: (id: string, operatorId: string) =>
    put<void>(`${WAREHOUSE_BASE}/wms-wave-picking/${id}/complete`, {
      operatorId,
    }),
  cancelWavePicking: (id: string, operatorId: string) =>
    put<void>(`${WAREHOUSE_BASE}/wms-wave-picking/${id}/cancel`, { operatorId }),
  removeWavePicking: (id: string) =>
    del<void>(`${WAREHOUSE_BASE}/wms-wave-picking/${id}`),
};

export type { WmsInboundDTO, WmsOutboundDTO };
