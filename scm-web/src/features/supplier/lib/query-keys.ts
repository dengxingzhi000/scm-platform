/**
 * 供应商模块的 query key 工厂。
 *
 * 所有键都以资源名打头，TanStack Query 按前缀失效：`supplierKeys.all` 一失效，
 * list / detail / profile / performance / stats 全部键都会重新拉取，
 * 所以主档相关的写操作只要失效 `supplierKeys.all` 即可。
 */

/** 供应商主档及其子资源。 */
export const supplierKeys = {
  all: ["suppliers"] as const,
  list: (params?: unknown) => ["suppliers", "list", params] as const,
  detail: (id: string) => ["suppliers", "detail", id] as const,
  profile: (id: string) => ["suppliers", "profile", id] as const,
  performance: (id: string) => ["suppliers", "performance", id] as const,
  stats: ["suppliers", "stats"] as const,
  changeLogs: (id: string, limit: number) => ["suppliers", "change-logs", id, limit] as const,
  contacts: (supplierId: string) => ["suppliers", "contacts", supplierId] as const,
  bankAccounts: (supplierId: string) => ["suppliers", "bank-accounts", supplierId] as const,
  certificates: (supplierId: string) => ["suppliers", "certificates", supplierId] as const,
  categories: (supplierId: string) => ["suppliers", "categories", supplierId] as const,
  /** 名称联想（GET /suppliers/search?name=），与列表查询分开缓存。 */
  search: (name: string) => ["suppliers", "search", name] as const,
};

/** 分类字典（GET /supplier-categories）。 */
export const supplierCategoryKeys = {
  all: ["supplier-categories"] as const,
  list: (params?: unknown) => ["supplier-categories", "list", params] as const,
};

/** 绩效评价（GET /supplier-evaluations）。 */
export const supplierEvaluationKeys = {
  /** 失效前缀：评价列表、详情、平均分一起刷新。 */
  all: ["supplier-evaluations"] as const,
  page: (supplierId: string | undefined, page: number) =>
    ["supplier-evaluations", "page", supplierId, page] as const,
  bySupplier: (supplierId: string) => ["supplier-evaluations", "by-supplier", supplierId] as const,
  detail: (id: string) => ["supplier-evaluations", "detail", id] as const,
  averageScore: (supplierId: string) => ["supplier-evaluations", "average-score", supplierId] as const,
};

/** 资质到期预警（GET /supplier-certificates/expiring）。 */
export const supplierCertificateExpiryKeys = {
  /** 失效前缀：新增或核验证照后，到期预警列表要重新算。 */
  all: ["supplier-certificates"] as const,
  expiring: (days: number, page: number) => ["supplier-certificates", "expiring", days, page] as const,
};
