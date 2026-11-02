import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  InvoicePage,
  ReconciliationQuery,
  ReconciliationRecordDTO,
  ReconciliationRecordPage,
  SettlementOrderPage,
} from "../contracts/finance";

const FINANCE_BASE = "/scm-finance";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown, params?: object) =>
  unwrap<T>(apiClient.post(url, body, { params }));

const put = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.put(url, body));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const financeApiService = {
  listSettlements: (page: number, size: number) =>
    get<SettlementOrderPage>(`${FINANCE_BASE}/settlement-order/list`, {
      page,
      size,
    }),
  pageInvoices: (pageNum: number, pageSize: number) =>
    get<InvoicePage>(`${FINANCE_BASE}/invoice/page`, { pageNum, pageSize }),

  pageReconciliations: (query: ReconciliationQuery) =>
    get<ReconciliationRecordPage>(`${FINANCE_BASE}/reconciliation-record/page`, query),

  getReconciliation: (id: string) =>
    get<ReconciliationRecordDTO>(`${FINANCE_BASE}/reconciliation-record/${id}`),
  createReconciliation: (entity: Partial<ReconciliationRecordDTO>) =>
    post<ReconciliationRecordDTO>(
      `${FINANCE_BASE}/reconciliation-record`,
      entity,
    ),
  updateReconciliation: (
    id: string,
    entity: Partial<ReconciliationRecordDTO>,
  ) =>
    put<ReconciliationRecordDTO>(
      `${FINANCE_BASE}/reconciliation-record/${id}`,
      entity,
    ),
  deleteReconciliation: (id: string) =>
    del<void>(`${FINANCE_BASE}/reconciliation-record/${id}`),
  reconcile: (id: string, reconcilerId: string, reconcilerName: string) =>
    post<ReconciliationRecordDTO>(
      `${FINANCE_BASE}/reconciliation-record/${id}/reconcile`,
      undefined,
      { reconcilerId, reconcilerName },
    ),
  markReconciliationDiff: (id: string, diffReason: string) =>
    post<ReconciliationRecordDTO>(
      `${FINANCE_BASE}/reconciliation-record/${id}/mark-diff`,
      undefined,
      { diffReason },
    ),
  confirmReconciliation: (
    id: string,
    confirmerId: string,
    confirmerName: string,
  ) =>
    post<ReconciliationRecordDTO>(
      `${FINANCE_BASE}/reconciliation-record/${id}/confirm`,
      undefined,
      { confirmerId, confirmerName },
    ),
};
