import type { MpPage } from "./page";

/** `com.scmcloud.supplier.domain.entity.SupSupplier`. */
export interface SupplierDTO {
  id: string;
  tenantId?: string | null;
  supplierCode?: string | null;
  supplierName?: string | null;
  supplierNameEn?: string | null;
  supplierType?: number | null;
  businessType?: string | null;
  legalPerson?: string | null;
  registeredCapital?: number | null;
  registrationNo?: string | null;
  taxNo?: string | null;
  establishmentDate?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  contactAddress?: string | null;
  bankName?: string | null;
  bankAccount?: string | null;
  bankAccountName?: string | null;
  creditRating?: string | null;
  qualityScore?: number | null;
  deliveryScore?: number | null;
  serviceScore?: number | null;
  cooperationStartDate?: string | null;
  cooperationStatus?: number | null;
  paymentTerms?: string | null;
  paymentMethod?: string | null;
  businessLicense?: string | null;
  certificates?: string | null;
  website?: string | null;
  supplierLevel?: number | null;
  auditStatus?: number | null;
  auditRemark?: string | null;
  auditedBy?: string | null;
  auditedAt?: string | null;
  riskLevel?: number | null;
  blacklisted?: boolean | null;
  blacklistReason?: string | null;
  blacklistedAt?: string | null;
  overallScore?: number | null;
  lastEvaluationAt?: string | null;
  enabled?: boolean | null;
  sortOrder?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

export type SupplierPage = MpPage<SupplierDTO>;

/**
 * The ten filters `GET /suppliers` accepts — every one of them is a query
 * param on the backend, none of them is a request body field.
 */
export type SupplierPageQuery = {
  page: number;
  size: number;
  keyword?: string;
  supplierType?: number;
  cooperationStatus?: number;
  enabled?: boolean;
  auditStatus?: number;
  supplierLevel?: number;
  riskLevel?: number;
  blacklisted?: boolean;
  categoryId?: string;
};

/** Legacy alias — kept so the existing `SupplierQuery` call sites keep compiling. */
export type SupplierQuery = SupplierPageQuery;

/** The actor behind a change; every mutating endpoint records it. */
export interface OperatorParams {
  operatorId?: string;
  operatorName?: string;
}

/** `com.scmcloud.supplier.dto.SupplierAuditRequest` — the only body of `PUT /{id}/audit`. */
export interface SupplierAuditPayload {
  action: "APPROVE" | "REJECT";
  operatorId?: string;
  operatorName?: string;
  remark?: string;
}

/**
 * `com.scmcloud.supplier.dto.SupplierUpsertRequest` — the body of `POST /` and
 * `PUT /{id}`. Server-managed fields (level, audit status, blacklist, scores)
 * are deliberately absent.
 */
export interface SupplierUpsertPayload {
  supplierCode?: string;
  supplierName: string;
  supplierNameEn?: string;
  supplierType?: number;
  businessType?: string;
  legalPerson?: string;
  registeredCapital?: number;
  registrationNo?: string;
  taxNo?: string;
  establishmentDate?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  contactAddress?: string;
  bankName?: string;
  bankAccount?: string;
  bankAccountName?: string;
  creditRating?: string;
  cooperationStartDate?: string;
  paymentTerms?: string;
  paymentMethod?: string;
  businessLicense?: string;
  certificates?: string;
  website?: string;
  sortOrder?: number;
  remark?: string;
}

/** `com.scmcloud.supplier.domain.entity.SupSupplierContact`. */
export interface SupplierContactDTO {
  id?: string | null;
  supplierId?: string | null;
  contactName?: string | null;
  contactTitle?: string | null;
  contactRole?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  contactAddress?: string | null;
  isPrimary?: boolean | null;
  enabled?: boolean | null;
  sortOrder?: number | null;
  createTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.supplier.domain.entity.SupSupplierBankAccount`. */
export interface SupplierBankAccountDTO {
  id?: string | null;
  supplierId?: string | null;
  bankName?: string | null;
  bankBranch?: string | null;
  bankAccount?: string | null;
  accountName?: string | null;
  currency?: string | null;
  swiftCode?: string | null;
  isDefault?: boolean | null;
  enabled?: boolean | null;
  sortOrder?: number | null;
  createTime?: string | null;
  remark?: string | null;
}

/**
 * `com.scmcloud.supplier.domain.entity.SupSupplierCertificate`.
 * `daysToExpiry` and `supplierName` are transient — the backend fills them in
 * on the way out.
 */
export interface SupplierCertificateDTO {
  id?: string | null;
  supplierId?: string | null;
  certificateType?: string | null;
  certificateName?: string | null;
  certificateNo?: string | null;
  issuingAuthority?: string | null;
  validFrom?: string | null;
  validTo?: string | null;
  fileUrl?: string | null;
  status?: number | null;
  verifiedBy?: string | null;
  verifiedAt?: string | null;
  createTime?: string | null;
  remark?: string | null;
  daysToExpiry?: number | null;
  supplierName?: string | null;
}

/** `com.scmcloud.supplier.domain.entity.SupSupplierCategory`. */
export interface SupplierCategoryDTO {
  id?: string | null;
  categoryCode?: string | null;
  categoryName?: string | null;
  parentId?: string | null;
  level?: number | null;
  enabled?: boolean | null;
  sortOrder?: number | null;
  createTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.supplier.domain.entity.SupSupplierEvaluation`. */
export interface SupplierEvaluationDTO {
  id?: string | null;
  supplierId?: string | null;
  evaluationPeriod?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  qualityScore?: number | null;
  deliveryScore?: number | null;
  serviceScore?: number | null;
  priceScore?: number | null;
  totalScore?: number | null;
  purchaseCount?: number | null;
  totalAmount?: number | null;
  onTimeDeliveryRate?: number | null;
  qualityPassRate?: number | null;
  evaluatorId?: string | null;
  evaluatorName?: string | null;
  evaluatedAt?: string | null;
  improvementSuggestions?: string | null;
  createTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.supplier.domain.entity.SupSupplierChangeLog`. */
export interface SupplierChangeLogDTO {
  id?: string | null;
  supplierId?: string | null;
  changeType?: string | null;
  changeContent?: string | null;
  reason?: string | null;
  operatorId?: string | null;
  operatorName?: string | null;
  createTime?: string | null;
}

/** `com.scmcloud.supplier.dto.SupplierProfileVO` — the envelope of `GET /{id}/profile`. */
export interface SupplierProfileDTO {
  supplier: SupplierDTO;
  contacts: SupplierContactDTO[];
  bankAccounts: SupplierBankAccountDTO[];
  certificates: SupplierCertificateDTO[];
  categories: SupplierCategoryDTO[];
  changeLogs: SupplierChangeLogDTO[];
}

/** `com.scmcloud.supplier.dto.SupplierStatsVO` — the envelope of `GET /stats`. */
export interface SupplierStatsDTO {
  total: number;
  enabled: number;
  cooperating: number;
  pendingAudit: number;
  blacklisted: number;
  expiringCertificates: number;
  byCooperationStatus: Record<number, number>;
  byAuditStatus: Record<number, number>;
}

/** `com.scmcloud.supplier.dto.SupplierPerformanceVO` — the envelope of `GET /{id}/performance`. */
export interface SupplierPerformanceDTO {
  supplierId: string;
  supplierName?: string | null;
  supplierLevel?: number | null;
  overallScore?: number | null;
  qualityScore?: number | null;
  deliveryScore?: number | null;
  serviceScore?: number | null;
  priceScore?: number | null;
  evaluationCount: number;
  latestPeriod?: string | null;
  latestTotalScore?: number | null;
  latestEvaluatedAt?: string | null;
  suggestedLevel?: number | null;
  recentEvaluations: SupplierEvaluationDTO[];
}
