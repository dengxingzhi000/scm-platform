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

/**
 * 供应商端点覆盖表（scm-supplier 62 个端点，其中 48 个在范围内；
 * 对账 /api/v1/settlements 与采购明细 /api/v1/purchase-order-items 属财务与采购域，不在本模块）
 * | 端点 | 调用点 |
 * |---|---|
 * | GET /suppliers | useSupplierList（列表页 /supplier / 审核队列 / 分类筛选） |
 * | GET /suppliers/search | useSupplierList（列表页搜索 /supplier） |
 * | GET /suppliers/active | 仅暴露契约与服务层，供跨模块的供应商选择器取用，本模块无界面调用点（设计例外 1/2） |
 * | GET /suppliers/{id} | useSupplierFormDetail（编辑页初值 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /suppliers/{id}/profile | useSupplierFormDetail（详情概览 /supplier/[id]） |
 * | GET /suppliers/{id}/performance | useSupplierFormDetail（评价分区 /supplier/[id]） |
 * | GET /suppliers/{id}/change-logs | useSupplierFormDetail（留痕分区 /supplier/[id]） |
 * | GET /suppliers/stats | useSupplierList（仪表盘聚合 /supplier） |
 * | GET /suppliers/{id}/categories | useSupplierFormDetail（分类分区 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | POST /suppliers | useSupplierFormDetail（新建 /supplier/new、编辑保存 /supplier/[id]/edit） |
 * | PUT /suppliers/{id} | useSupplierFormDetail（编辑保存 /supplier/[id]/edit） |
 * | DELETE /suppliers/{id} | useSupplierFormDetail（列表页删除 /supplier、详情删除 /supplier/[id]） |
 * | PUT /suppliers/{id}/enable | useSupplierFormDetail（启停 /supplier/[id]/edit、列表操作 /supplier） |
 * | PUT /suppliers/{id}/disable | useSupplierFormDetail（启停 /supplier/[id]/edit、列表操作 /supplier） |
 * | PUT /suppliers/{id}/submit-audit | useSupplierFormDetail（审核提交 /supplier/audit、详情 /supplier/[id]） |
 * | PUT /suppliers/{id}/audit | useSupplierFormDetail（审核操作 /supplier/audit、详情 /supplier/[id]） |
 * | PUT /suppliers/{id}/blacklist | useSupplierFormDetail（黑名单操作 /supplier/[id]/edit、列表 /supplier） |
 * | PUT /suppliers/{id}/unblacklist | useSupplierFormDetail（黑名单撤销 /supplier/[id]/edit、列表 /supplier） |
 * | PUT /suppliers/{id}/risk-level | useSupplierFormDetail（风险级别 /supplier/[id]/edit、列表 /supplier） |
 * | PUT /suppliers/{id}/cooperation-status | useSupplierFormDetail（合作状态 /supplier/[id]/edit、列表 /supplier） |
 * | PUT /suppliers/{id}/categories | useSupplierFormDetail（分类关联 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /supplier-evaluations | useSupplierEvaluation（评价列表 /supplier/[id]） |
 * | GET /supplier-evaluations/by-supplier/{supplierId} | useSupplierEvaluation（评价列表 /supplier/[id]） |
 * | GET /supplier-evaluations/average-score/{supplierId} | useSupplierEvaluation（评价分区 /supplier/[id]、仪表盘 /supplier） |
 * | GET /supplier-evaluations/{id} | useSupplierFormDetail（评价详情 /supplier/[id]） |
 * | POST /supplier-evaluations | useSupplierFormDetail（评价新建 /supplier/[id]/edit、评价页 /supplier/[id]） |
 * | PUT /supplier-evaluations/{id} | useSupplierFormDetail（评价编辑 /supplier/[id]/edit、评价页 /supplier/[id]） |
 * | DELETE /supplier-evaluations/{id} | useSupplierFormDetail（评价删除 /supplier/[id]/edit、评价页 /supplier/[id]） |
 * | GET /suppliers/{supplierId}/contacts | useSupplierContact（联系人分区 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | POST /suppliers/{supplierId}/contacts | useSupplierContact（联系人新建 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | PUT /suppliers/{supplierId}/contacts/{id} | useSupplierContact（联系人编辑 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | DELETE /suppliers/{supplierId}/contacts/{id} | useSupplierContact（联系人删除 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /supplier-contacts/{id} | useSupplierContact（联系人详情 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /suppliers/{supplierId}/bank-accounts | useSupplierBankAccount（银行账户分区 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | POST /suppliers/{supplierId}/bank-accounts | useSupplierBankAccount（银行账户新建 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | PUT /suppliers/{supplierId}/bank-accounts/{id} | useSupplierBankAccount（银行账户编辑 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | DELETE /suppliers/{supplierId}/bank-accounts/{id} | useSupplierBankAccount（银行账户删除 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /supplier-bank-accounts/{id} | useSupplierBankAccount（银行账户详情 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /suppliers/{supplierId}/certificates | useSupplierCertificate（证照分区 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | POST /suppliers/{supplierId}/certificates | useSupplierCertificate（证照新建 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | PUT /suppliers/{supplierId}/certificates/{id} | useSupplierCertificate（证照编辑 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | DELETE /suppliers/{supplierId}/certificates/{id} | useSupplierCertificate（证照删除 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /supplier-certificates/{id} | useSupplierCertificate（证照详情 /supplier/[id]/edit、详情 /supplier/[id]） |
 * | GET /supplier-categories | useSupplierCategory（分类字典 /supplier/category、筛选 /supplier） |
 * | POST /supplier-categories | useSupplierCategory（分类新建 /supplier/category） |
 * | PUT /supplier-categories/{id} | useSupplierCategory（分类编辑 /supplier/category） |
 * | DELETE /supplier-categories/{id} | 仅暴露契约与服务层；分类字典页不暴露删除按钮（设计例外 2/2） |
 * | GET /supplier-certificates/expiring | useSupplierCertificate（证照预警 /supplier/[id]/edit、列表 /supplier） |
 */

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
