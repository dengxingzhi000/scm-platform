export interface Supplier {
  id: string
  name: string
  code: string
  contactPerson: string
  contactPhone: string
  email?: string
  address?: string
  status: 'active' | 'disabled'
  rating: number // 0–5
  overallScore?: number // 0–100
  supplierLevel?: number
  auditStatus?: number
  riskLevel?: number
  blacklisted: boolean
  cooperationStatus?: number
  creditRating?: string
  taxNo?: string
  supplierType?: number
  createdAt: string
  remark?: string
}

export interface SupplierSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}

/**
 * 子实体的 UI 模型：字段名与类型沿用后端 DTO，日期字段保留原始 ISO 串，
 * 由展示层用 `dayjs` 格式化。`id` 统一归一成非空字符串。
 */
export interface SupplierContact {
  id: string
  supplierId?: string | null
  contactName?: string | null
  contactTitle?: string | null
  contactRole?: string | null
  contactPhone?: string | null
  contactEmail?: string | null
  contactAddress?: string | null
  isPrimary?: boolean | null
  enabled?: boolean | null
  sortOrder?: number | null
  createTime?: string | null
  remark?: string | null
}

export interface SupplierBankAccount {
  id: string
  supplierId?: string | null
  bankName?: string | null
  bankBranch?: string | null
  bankAccount?: string | null
  accountName?: string | null
  currency?: string | null
  swiftCode?: string | null
  isDefault?: boolean | null
  enabled?: boolean | null
  sortOrder?: number | null
  createTime?: string | null
  remark?: string | null
}

export interface SupplierCertificate {
  id: string
  supplierId?: string | null
  certificateType?: string | null
  certificateName?: string | null
  certificateNo?: string | null
  issuingAuthority?: string | null
  validFrom?: string | null
  validTo?: string | null
  fileUrl?: string | null
  status?: number | null
  verifiedBy?: string | null
  verifiedAt?: string | null
  createTime?: string | null
  remark?: string | null
  /** 距到期天数；后端会回填，缺失时前端按 `validTo` 计算。 */
  daysToExpiry: number | null
}

export interface SupplierCategory {
  id: string
  categoryCode?: string | null
  categoryName?: string | null
  parentId?: string | null
  level?: number | null
  enabled?: boolean | null
  sortOrder?: number | null
  createTime?: string | null
  remark?: string | null
}

export interface SupplierEvaluation {
  id: string
  supplierId?: string | null
  evaluationPeriod?: string | null
  startDate?: string | null
  endDate?: string | null
  qualityScore?: number | null
  deliveryScore?: number | null
  serviceScore?: number | null
  priceScore?: number | null
  totalScore?: number | null
  purchaseCount?: number | null
  totalAmount?: number | null
  onTimeDeliveryRate?: number | null
  qualityPassRate?: number | null
  evaluatorId?: string | null
  evaluatorName?: string | null
  evaluatedAt?: string | null
  improvementSuggestions?: string | null
  createTime?: string | null
  remark?: string | null
}

export interface SupplierChangeLog {
  id: string
  supplierId?: string | null
  changeType?: string | null
  changeContent?: string | null
  reason?: string | null
  operatorId?: string | null
  /** 后端没写操作人时回落为「系统」。 */
  operatorName: string
  createTime?: string | null
}

export interface SupplierStats {
  total: number
  enabled: number
  cooperating: number
  pendingAudit: number
  blacklisted: number
  expiringCertificates: number
  byCooperationStatus: Record<number, number>
  byAuditStatus: Record<number, number>
}

export interface SupplierPerformance {
  supplierId: string
  supplierName?: string | null
  supplierLevel?: number | null
  overallScore?: number | null
  qualityScore?: number | null
  deliveryScore?: number | null
  serviceScore?: number | null
  priceScore?: number | null
  evaluationCount: number
  latestPeriod?: string | null
  latestTotalScore?: number | null
  latestEvaluatedAt?: string | null
  suggestedLevel?: number | null
  recentEvaluations: SupplierEvaluation[]
}

export interface SupplierProfile {
  supplier: Supplier
  contacts: SupplierContact[]
  bankAccounts: SupplierBankAccount[]
  certificates: SupplierCertificate[]
  categories: SupplierCategory[]
  changeLogs: SupplierChangeLog[]
}
