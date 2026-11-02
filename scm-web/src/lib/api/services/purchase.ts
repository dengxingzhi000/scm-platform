import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  PurOrderDTO,
  PurOrderItemDTO,
  PurOrderPage,
  PurOrderQuery,
  PurQuotationDTO,
  PurQuotationItemDTO,
  PurQuotationPage,
  PurQuotationQuery,
  PurRequestConvertPayload,
  PurRequestDetailDTO,
  PurRequestDTO,
  PurRequestPage,
  PurRequestQuery,
  PurRequestUpsertPayload,
  PurRfqDTO,
  PurRfqItemDTO,
  PurRfqPage,
  PurRfqQuery,
} from "../contracts/purchase";

/**
 * scm-purchase 通过网关的 Nacos discovery locator 访问
 * （`/scm-purchase/**`，第一段被剥离后服务看到 `/api/v1/**`）。
 */
const PURCHASE_BASE = "/scm-purchase/api/v1/pur-order";
const REQUEST_BASE = "/scm-purchase/pur-request";
const RFQ_BASE = "/scm-purchase/pur-rfq";
const QUOTATION_BASE = "/scm-purchase/pur-quotation";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown, params?: object) =>
  unwrap<T>(apiClient.post(url, body, { params }));

const put = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.put(url, body));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const purchaseApiService = {
  page: (query: PurOrderQuery) =>
    get<PurOrderPage>(`${PURCHASE_BASE}/page`, query),
  getById: (id: string) => get<PurOrderDTO>(`${PURCHASE_BASE}/${id}`),
  listItems: (id: string) =>
    get<PurOrderItemDTO[]>(`${PURCHASE_BASE}/${id}/items`),

  pageRequests: (query: PurRequestQuery) =>
    get<PurRequestPage>(`${REQUEST_BASE}/page`, query),
  getRequest: (id: string) =>
    get<PurRequestDTO>(`${REQUEST_BASE}/${id}`),
  getRequestDetail: (id: string) =>
    get<PurRequestDetailDTO>(`${REQUEST_BASE}/${id}/detail`),
  createRequest: (payload: PurRequestUpsertPayload) =>
    post<PurRequestDTO>(`${REQUEST_BASE}/full`, payload),
  updateRequest: (id: string, payload: PurRequestUpsertPayload) =>
    put<PurRequestDTO>(`${REQUEST_BASE}/${id}/full`, payload),
  deleteRequest: (id: string) => del<boolean>(`${REQUEST_BASE}/${id}`),
  submitRequest: (id: string) =>
    post<boolean>(`${REQUEST_BASE}/${id}/submit`),
  approveRequest: (id: string) =>
    post<boolean>(`${REQUEST_BASE}/${id}/approve`),
  rejectRequest: (id: string, reason: string) =>
    post<boolean>(`${REQUEST_BASE}/${id}/reject`, undefined, { reason }),
  closeRequest: (id: string) =>
    post<boolean>(`${REQUEST_BASE}/${id}/close`),
  convertRequest: (id: string, payload: PurRequestConvertPayload) =>
    post<PurOrderDTO>(`${REQUEST_BASE}/${id}/convert`, payload),

  pageRfqs: (query: PurRfqQuery) =>
    get<PurRfqPage>(`${RFQ_BASE}/page`, query),
  createRfq: (entity: Partial<PurRfqDTO>) =>
    post<boolean>(RFQ_BASE, entity),
  updateRfq: (entity: Partial<PurRfqDTO>) => put<boolean>(RFQ_BASE, entity),
  deleteRfq: (id: string) => del<boolean>(`${RFQ_BASE}/${id}`),
  publishRfq: (id: string) => post<boolean>(`${RFQ_BASE}/${id}/publish`),
  closeRfq: (id: string) => post<boolean>(`${RFQ_BASE}/${id}/close`),
  listRfqItems: (rfqId: string) =>
    get<PurRfqItemDTO[]>(`/scm-purchase/pur-rfq-item/list/${rfqId}`),
  createRfqItem: (entity: Partial<PurRfqItemDTO>) =>
    post<boolean>("/scm-purchase/pur-rfq-item", entity),
  deleteRfqItem: (id: string) =>
    del<boolean>(`/scm-purchase/pur-rfq-item/${id}`),

  pageQuotations: (query: PurQuotationQuery) =>
    get<PurQuotationPage>(`${QUOTATION_BASE}/page`, query),
  createQuotation: (entity: Partial<PurQuotationDTO>) =>
    post<boolean>(QUOTATION_BASE, entity),
  submitQuotation: (id: string) =>
    post<boolean>(`${QUOTATION_BASE}/${id}/submit`),
  selectQuotation: (id: string, selectedBy: string, selectedByName: string) =>
    post<boolean>(`${QUOTATION_BASE}/${id}/select`, undefined, {
      selectedBy,
      selectedByName,
    }),
  deleteQuotation: (id: string) => del<boolean>(`${QUOTATION_BASE}/${id}`),
  listQuotationItems: (quotationId: string) =>
    get<PurQuotationItemDTO[]>(
      `/scm-purchase/pur-quotation-item/list/${quotationId}`,
    ),
};
