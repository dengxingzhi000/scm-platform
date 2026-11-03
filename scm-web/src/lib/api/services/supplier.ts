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
 * 供应商端点覆盖表（scm-supplier 共 62 个端点，其中 48 个在范围内；
 * 对账 /api/v1/settlements 与采购明细 /api/v1/purchase-order-items 属财务与采购域，不在本模块）。
 *
 * 调用点按实际组件 / hook 填写。本模块无界面调用点共 5 行：2 个是 spec 裁定的设计例外，
 * 3 个是契约与服务层已暴露、但补齐范围内未安排 UI（useSupplierSearch、
 * useSupplierEvaluationDetail、supplierApiService.evaluations.bySupplier 均无组件消费）
 * —— 不臆断调用点，留档待裁决（见 SDD ledger Task 15 rulings）。
 *
 * | 端点 | 调用点 |
 * |---|---|
 * | GET  /suppliers | useSupplierList（列表页 /supplier；审核队列 /supplier/audit） |
 * | GET  /suppliers/active | 仅暴露契约与服务层，供跨模块的供应商选择器取用，本模块无界面调用点（设计例外 1/2） |
 * | GET  /suppliers/search | 仅暴露契约与服务层（useSupplierSearch）；列表页用 useSupplierList 的 keyword 过滤，本模块无界面调用点 |
 * | GET  /suppliers/{id} | useSupplierFormDetail（编辑页初值 /supplier/[id]/edit）；useSupplierDetail 未被组件消费 |
 * | GET  /suppliers/{id}/profile | useSupplierProfile（概览分区 /supplier/[id]） |
 * | GET  /suppliers/{id}/performance | useSupplierPerformance（绩效评价分区 /supplier/[id]） |
 * | GET  /suppliers/{id}/change-logs | useSupplierChangeLogs（变更留痕分区 /supplier/[id]） |
 * | GET  /suppliers/stats | useSupplierStats（列表页 KPI；审核工作台已驳回计数） |
 * | GET  /suppliers/{id}/categories | useSupplierAssignedCategories（分类分区 /supplier/[id]） |
 * | POST /suppliers | useCreateSupplier（新建 /supplier/new） |
 * | PUT  /suppliers/{id} | useUpdateSupplier（编辑保存 /supplier/[id]/edit） |
 * | DELETE /suppliers/{id} | useDeleteSupplier（列表页删除 /supplier；详情页删除 /supplier/[id]） |
 * | PUT  /suppliers/{id}/enable | useEnableSupplier（启停；/supplier） |
 * | PUT  /suppliers/{id}/disable | useDisableSupplier（启停；/supplier） |
 * | POST /suppliers/{id}/submit-audit | useSubmitSupplierAudit（提交审核；/supplier、/supplier/[id]） |
 * | PUT  /suppliers/{id}/audit | useAuditSupplier（审核工作台 /supplier/audit；列表行内） |
 * | PUT  /suppliers/{id}/blacklist | useBlacklistSupplier（黑名单；/supplier、/supplier/[id]） |
 * | PUT  /suppliers/{id}/unblacklist | useUnblacklistSupplier（移出黑名单；/supplier、/supplier/[id]） |
 * | PUT  /suppliers/{id}/risk-level | useChangeSupplierRiskLevel（风险级别；/supplier、/supplier/[id]） |
 * | PUT  /suppliers/{id}/cooperation-status | useChangeCooperationStatus（合作状态；/supplier、/supplier/[id]） |
 * | PUT  /suppliers/{id}/categories | useAssignSupplierCategories（分类分区确认保存） |
 * | GET  /supplier-evaluations | useSupplierEvaluations（绩效评价分区 /supplier/[id]） |
 * | GET  /supplier-evaluations/{id} | 仅暴露契约与服务层（useSupplierEvaluationDetail），绩效评价分区改用带 supplierId 的分页列表，本模块无界面调用点 |
 * | POST /supplier-evaluations | useCreateSupplierEvaluation（评价新建 / 评价弹窗） |
 * | PUT  /supplier-evaluations/{id} | useUpdateSupplierEvaluation（评价编辑） |
 * | DELETE /supplier-evaluations/{id} | useDeleteSupplierEvaluation（评价删除） |
 * | GET  /supplier-evaluations/supplier/{supplierId} | 仅暴露契约与服务层（supplierApiService.evaluations.bySupplier），前端用 ?supplierId= 分页代替，本模块无界面调用点 |
 * | GET  /supplier-evaluations/supplier/{supplierId}/average-score | useSupplierAverageScore（平均分 / 绩效评价分区） |
 * | GET  /suppliers/{supplierId}/contacts | useSupplierContacts（联系人分区 /supplier/[id]） |
 * | POST /suppliers/{supplierId}/contacts | useCreateSupplierContact（联系人新建） |
 * | PUT  /suppliers/{supplierId}/contacts/{id} | useUpdateSupplierContact（联系人编辑） |
 * | DELETE /suppliers/{supplierId}/contacts/{id} | useDeleteSupplierContact（联系人删除） |
 * | PUT  /suppliers/{supplierId}/contacts/{id}/primary | useSetPrimarySupplierContact（设为主要联系人） |
 * | GET  /suppliers/{supplierId}/bank-accounts | useSupplierBankAccounts（银行账户分区 /supplier/[id]） |
 * | POST /suppliers/{supplierId}/bank-accounts | useCreateSupplierBankAccount（银行账户新建） |
 * | PUT  /suppliers/{supplierId}/bank-accounts/{id} | useUpdateSupplierBankAccount（银行账户编辑） |
 * | DELETE /suppliers/{supplierId}/bank-accounts/{id} | useDeleteSupplierBankAccount（银行账户删除） |
 * | PUT  /suppliers/{supplierId}/bank-accounts/{id}/default | useSetDefaultSupplierBankAccount（设为默认账户） |
 * | GET  /suppliers/{supplierId}/certificates | useSupplierCertificates（证照分区 /supplier/[id]） |
 * | POST /suppliers/{supplierId}/certificates | useCreateSupplierCertificate（证照新建） |
 * | PUT  /suppliers/{supplierId}/certificates/{id} | useUpdateSupplierCertificate（证照编辑） |
 * | PUT  /suppliers/{supplierId}/certificates/{id}/verify | useVerifySupplierCertificate（证照核验） |
 * | DELETE /suppliers/{supplierId}/certificates/{id} | useDeleteSupplierCertificate（证照删除） |
 * | GET  /supplier-certificates/expiring | useExpiringCertificates（审核工作台到期预警 /supplier/audit） |
 * | GET  /supplier-categories | useSupplierCategories（列表筛选 /supplier；分类字典 /supplier/category） |
 * | POST /supplier-categories | useCreateSupplierCategory（分类字典新建） |
 * | PUT  /supplier-categories/{id} | useUpdateSupplierCategory（分类字典编辑） |
 * | DELETE /supplier-categories/{id} | 仅暴露契约与服务层；分类字典页不暴露删除按钮（设计例外 2/2） |
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
