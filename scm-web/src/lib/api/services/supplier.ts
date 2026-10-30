import apiClient from "../client";
import { unwrap } from "../envelope";
import type { MpPage } from "../contracts/page";
import type {
  OperatorParams,
  SupplierAuditPayload,
  SupplierBankAccountDTO,
  SupplierCategoryDTO,
  SupplierCertificateDTO,
  SupplierChangeLogDTO,
  SupplierContactDTO,
  SupplierDTO,
  SupplierEvaluationDTO,
  SupplierPage,
  SupplierPageQuery,
  SupplierPerformanceDTO,
  SupplierProfileDTO,
  SupplierStatsDTO,
  SupplierUpsertPayload,
} from "../contracts/supplier";

const SUPPLIER_BASE = "/scm-supplier/api/v1/suppliers";
const CATEGORY_BASE = "/scm-supplier/api/v1/supplier-categories";
const EVALUATION_BASE = "/scm-supplier/api/v1/supplier-evaluations";
const CERTIFICATE_EXPIRY_BASE = "/scm-supplier/api/v1/supplier-certificates";

/**
 * Drops the `undefined` / `null` entries so a query string never carries
 * `?keyword=undefined`.
 */
const clean = <T extends Record<string, unknown>>(input: T): T =>
  Object.fromEntries(
    Object.entries(input).filter(([, value]) => value !== undefined && value !== null)
  ) as T;

/**
 * The scm-supplier controllers split their parameters across the query string
 * and the request body. Only `POST /`, `PUT /{id}`, `PUT /{id}/audit` and the
 * sub-resource writes take a body; everything else reads query params — and
 * `PUT /{id}/categories` takes a *bare* id array, not `{ categoryIds: [...] }`.
 */
export const supplierApiService = {
  // ---- 已有，保持兼容 ----
  list: (query: SupplierPageQuery): Promise<SupplierPage> =>
    unwrap<SupplierPage>(apiClient.get(SUPPLIER_BASE, { params: clean(query) })),

  getById: (id: string): Promise<SupplierDTO> =>
    unwrap<SupplierDTO>(apiClient.get(`${SUPPLIER_BASE}/${id}`, { params: clean({}) })),

  // ---- 主档 ----
  stats: (): Promise<SupplierStatsDTO> =>
    unwrap<SupplierStatsDTO>(apiClient.get(`${SUPPLIER_BASE}/stats`, { params: clean({}) })),

  profile: (id: string): Promise<SupplierProfileDTO> =>
    unwrap<SupplierProfileDTO>(
      apiClient.get(`${SUPPLIER_BASE}/${id}/profile`, { params: clean({}) })
    ),

  performance: (id: string): Promise<SupplierPerformanceDTO> =>
    unwrap<SupplierPerformanceDTO>(
      apiClient.get(`${SUPPLIER_BASE}/${id}/performance`, { params: clean({}) })
    ),

  changeLogs: (id: string, limit?: number): Promise<SupplierChangeLogDTO[]> =>
    unwrap<SupplierChangeLogDTO[]>(
      apiClient.get(`${SUPPLIER_BASE}/${id}/change-logs`, { params: clean({ limit }) })
    ),

  active: (): Promise<SupplierDTO[]> =>
    unwrap<SupplierDTO[]>(apiClient.get(`${SUPPLIER_BASE}/active`, { params: clean({}) })),

  search: (name: string): Promise<SupplierDTO[]> =>
    unwrap<SupplierDTO[]>(
      apiClient.get(`${SUPPLIER_BASE}/search`, { params: clean({ name }) })
    ),

  create: (payload: SupplierUpsertPayload): Promise<SupplierDTO> =>
    unwrap<SupplierDTO>(apiClient.post(SUPPLIER_BASE, payload)),

  update: (id: string, payload: SupplierUpsertPayload): Promise<SupplierDTO> =>
    unwrap<SupplierDTO>(apiClient.put(`${SUPPLIER_BASE}/${id}`, payload)),

  remove: (id: string, operator?: OperatorParams): Promise<void> =>
    unwrap<void>(
      apiClient.delete(`${SUPPLIER_BASE}/${id}`, {
        params: clean({
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  // ---- 准入 / 启停 / 风险 / 合作状态 ----
  enable: (id: string): Promise<void> =>
    unwrap<void>(apiClient.put(`${SUPPLIER_BASE}/${id}/enable`)),

  disable: (id: string): Promise<void> =>
    unwrap<void>(apiClient.put(`${SUPPLIER_BASE}/${id}/disable`)),

  submitAudit: (id: string, operator?: OperatorParams): Promise<void> =>
    unwrap<void>(
      apiClient.post(`${SUPPLIER_BASE}/${id}/submit-audit`, undefined, {
        params: clean({
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  audit: (id: string, payload: SupplierAuditPayload): Promise<void> =>
    unwrap<void>(apiClient.put(`${SUPPLIER_BASE}/${id}/audit`, payload)),

  blacklist: (id: string, reason: string, operator?: OperatorParams): Promise<void> =>
    unwrap<void>(
      apiClient.put(`${SUPPLIER_BASE}/${id}/blacklist`, undefined, {
        params: clean({
          reason,
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  unblacklist: (id: string, operator?: OperatorParams): Promise<void> =>
    unwrap<void>(
      apiClient.put(`${SUPPLIER_BASE}/${id}/unblacklist`, undefined, {
        params: clean({
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  changeRiskLevel: (
    id: string,
    level: number,
    reason?: string,
    operator?: OperatorParams
  ): Promise<void> =>
    unwrap<void>(
      apiClient.put(`${SUPPLIER_BASE}/${id}/risk-level`, undefined, {
        params: clean({
          level,
          reason,
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  changeCooperationStatus: (
    id: string,
    status: number,
    operator?: OperatorParams
  ): Promise<void> =>
    unwrap<void>(
      apiClient.put(`${SUPPLIER_BASE}/${id}/cooperation-status`, undefined, {
        params: clean({
          status,
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  // ---- 分类关联 ----
  getCategories: (id: string): Promise<SupplierCategoryDTO[]> =>
    unwrap<SupplierCategoryDTO[]>(
      apiClient.get(`${SUPPLIER_BASE}/${id}/categories`, { params: clean({}) })
    ),

  /** The body is a bare id array — the backend reads a `List<String>`. */
  assignCategories: (
    id: string,
    categoryIds: string[],
    operator?: OperatorParams
  ): Promise<void> =>
    unwrap<void>(
      apiClient.put(`${SUPPLIER_BASE}/${id}/categories`, categoryIds, {
        params: clean({
          operatorId: operator?.operatorId,
          operatorName: operator?.operatorName,
        }),
      })
    ),

  // ---- 联系人 ----
  contacts: {
    list: (supplierId: string): Promise<SupplierContactDTO[]> =>
      unwrap<SupplierContactDTO[]>(
        apiClient.get(`${SUPPLIER_BASE}/${supplierId}/contacts`, { params: clean({}) })
      ),

    create: (supplierId: string, data: SupplierContactDTO): Promise<SupplierContactDTO> =>
      unwrap<SupplierContactDTO>(
        apiClient.post(`${SUPPLIER_BASE}/${supplierId}/contacts`, data)
      ),

    update: (
      supplierId: string,
      id: string,
      data: SupplierContactDTO
    ): Promise<SupplierContactDTO> =>
      unwrap<SupplierContactDTO>(
        apiClient.put(`${SUPPLIER_BASE}/${supplierId}/contacts/${id}`, data)
      ),

    remove: (supplierId: string, id: string): Promise<void> =>
      unwrap<void>(apiClient.delete(`${SUPPLIER_BASE}/${supplierId}/contacts/${id}`)),

    setPrimary: (supplierId: string, id: string): Promise<void> =>
      unwrap<void>(apiClient.put(`${SUPPLIER_BASE}/${supplierId}/contacts/${id}/primary`)),
  },

  // ---- 银行账户 ----
  bankAccounts: {
    list: (supplierId: string): Promise<SupplierBankAccountDTO[]> =>
      unwrap<SupplierBankAccountDTO[]>(
        apiClient.get(`${SUPPLIER_BASE}/${supplierId}/bank-accounts`, { params: clean({}) })
      ),

    create: (
      supplierId: string,
      data: SupplierBankAccountDTO
    ): Promise<SupplierBankAccountDTO> =>
      unwrap<SupplierBankAccountDTO>(
        apiClient.post(`${SUPPLIER_BASE}/${supplierId}/bank-accounts`, data)
      ),

    update: (
      supplierId: string,
      id: string,
      data: SupplierBankAccountDTO
    ): Promise<SupplierBankAccountDTO> =>
      unwrap<SupplierBankAccountDTO>(
        apiClient.put(`${SUPPLIER_BASE}/${supplierId}/bank-accounts/${id}`, data)
      ),

    remove: (supplierId: string, id: string): Promise<void> =>
      unwrap<void>(apiClient.delete(`${SUPPLIER_BASE}/${supplierId}/bank-accounts/${id}`)),

    setDefault: (supplierId: string, id: string): Promise<void> =>
      unwrap<void>(apiClient.put(`${SUPPLIER_BASE}/${supplierId}/bank-accounts/${id}/default`)),
  },

  // ---- 资质证照 ----
  certificates: {
    list: (supplierId: string): Promise<SupplierCertificateDTO[]> =>
      unwrap<SupplierCertificateDTO[]>(
        apiClient.get(`${SUPPLIER_BASE}/${supplierId}/certificates`, { params: clean({}) })
      ),

    create: (
      supplierId: string,
      data: SupplierCertificateDTO
    ): Promise<SupplierCertificateDTO> =>
      unwrap<SupplierCertificateDTO>(
        apiClient.post(`${SUPPLIER_BASE}/${supplierId}/certificates`, data)
      ),

    update: (
      supplierId: string,
      id: string,
      data: SupplierCertificateDTO
    ): Promise<SupplierCertificateDTO> =>
      unwrap<SupplierCertificateDTO>(
        apiClient.put(`${SUPPLIER_BASE}/${supplierId}/certificates/${id}`, data)
      ),

    verify: (
      supplierId: string,
      id: string,
      params: {
        approved: boolean;
        operatorId?: string;
        operatorName?: string;
        remark?: string;
      }
    ): Promise<void> =>
      unwrap<void>(
        apiClient.put(`${SUPPLIER_BASE}/${supplierId}/certificates/${id}/verify`, undefined, {
          params: clean({
            approved: params.approved,
            operatorId: params.operatorId,
            operatorName: params.operatorName,
            remark: params.remark,
          }),
        })
      ),

    remove: (supplierId: string, id: string): Promise<void> =>
      unwrap<void>(apiClient.delete(`${SUPPLIER_BASE}/${supplierId}/certificates/${id}`)),
  },

  // ---- 资质到期预警 ----
  certificateExpiry: {
    expiring: (query: {
      page: number;
      size: number;
      days: number;
    }): Promise<MpPage<SupplierCertificateDTO>> =>
      unwrap<MpPage<SupplierCertificateDTO>>(
        apiClient.get(`${CERTIFICATE_EXPIRY_BASE}/expiring`, { params: clean({ ...query }) })
      ),
  },

  // ---- 分类字典 ----
  categories: {
    list: (query?: { keyword?: string; enabled?: boolean }): Promise<SupplierCategoryDTO[]> =>
      unwrap<SupplierCategoryDTO[]>(
        apiClient.get(CATEGORY_BASE, {
          params: clean({ keyword: query?.keyword, enabled: query?.enabled }),
        })
      ),

    create: (data: SupplierCategoryDTO): Promise<SupplierCategoryDTO> =>
      unwrap<SupplierCategoryDTO>(apiClient.post(CATEGORY_BASE, data)),

    update: (id: string, data: SupplierCategoryDTO): Promise<SupplierCategoryDTO> =>
      unwrap<SupplierCategoryDTO>(apiClient.put(`${CATEGORY_BASE}/${id}`, data)),

    remove: (id: string): Promise<void> =>
      unwrap<void>(apiClient.delete(`${CATEGORY_BASE}/${id}`)),
  },

  // ---- 绩效评价 ----
  evaluations: {
    page: (query: {
      page: number;
      size: number;
      supplierId?: string;
      evaluationPeriod?: string;
    }): Promise<MpPage<SupplierEvaluationDTO>> =>
      unwrap<MpPage<SupplierEvaluationDTO>>(
        apiClient.get(EVALUATION_BASE, { params: clean({ ...query }) })
      ),

    getById: (id: string): Promise<SupplierEvaluationDTO> =>
      unwrap<SupplierEvaluationDTO>(
        apiClient.get(`${EVALUATION_BASE}/${id}`, { params: clean({}) })
      ),

    create: (data: SupplierEvaluationDTO): Promise<SupplierEvaluationDTO> =>
      unwrap<SupplierEvaluationDTO>(apiClient.post(EVALUATION_BASE, data)),

    update: (id: string, data: SupplierEvaluationDTO): Promise<SupplierEvaluationDTO> =>
      unwrap<SupplierEvaluationDTO>(apiClient.put(`${EVALUATION_BASE}/${id}`, data)),

    remove: (id: string): Promise<void> =>
      unwrap<void>(apiClient.delete(`${EVALUATION_BASE}/${id}`)),

    bySupplier: (supplierId: string): Promise<SupplierEvaluationDTO[]> =>
      unwrap<SupplierEvaluationDTO[]>(
        apiClient.get(`${EVALUATION_BASE}/supplier/${supplierId}`, { params: clean({}) })
      ),

    averageScore: (supplierId: string): Promise<number> =>
      unwrap<number>(
        apiClient.get(`${EVALUATION_BASE}/supplier/${supplierId}/average-score`, {
          params: clean({}),
        })
      ),
  },
};
