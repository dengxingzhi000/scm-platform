import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  TmsCarrierDTO,
  TmsCarrierPage,
  TmsCarrierQuery,
  TmsTrackingDTO,
  TmsTrackingPage,
  TmsTrackingQuery,
  TmsWaybillPage,
} from "../contracts/logistics";

const LOGISTICS_BASE = "/scm-logistics/api/v1";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.post(url, body));

const put = <T>(url: string, body?: unknown, params?: object) =>
  unwrap<T>(apiClient.put(url, body, { params }));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const logisticsApiService = {
  pageWaybills: (page: number, size: number) =>
    get<TmsWaybillPage>(`${LOGISTICS_BASE}/tms-waybill/list`, { page, size }),
  listTracking: (waybillId: string) =>
    get<TmsTrackingDTO[]>(`${LOGISTICS_BASE}/tms-tracking/waybill/${waybillId}`),

  pageTracking: (query: TmsTrackingQuery) =>
    get<TmsTrackingPage>(`${LOGISTICS_BASE}/tms-tracking/list`, query),
  createTracking: (entity: Partial<TmsTrackingDTO>) =>
    post<TmsTrackingDTO>(`${LOGISTICS_BASE}/tms-tracking`, entity),

  pageCarriers: (query: TmsCarrierQuery) =>
    get<TmsCarrierPage>(`${LOGISTICS_BASE}/tms-carrier/list`, query),
  createCarrier: (entity: Partial<TmsCarrierDTO>) =>
    post<TmsCarrierDTO>(`${LOGISTICS_BASE}/tms-carrier`, entity),
  updateCarrier: (id: string, entity: Partial<TmsCarrierDTO>) =>
    put<TmsCarrierDTO>(`${LOGISTICS_BASE}/tms-carrier/${id}`, entity),
  deleteCarrier: (id: string) =>
    del<void>(`${LOGISTICS_BASE}/tms-carrier/${id}`),
  setCarrierEnabled: (id: string, enabled: boolean) =>
    put<void>(`${LOGISTICS_BASE}/tms-carrier/${id}/enable`, undefined, {
      enabled,
    }),
};
