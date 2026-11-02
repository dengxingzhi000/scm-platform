import dayjs from "dayjs";

import type {
  SupplierBankAccountDTO,
  SupplierCertificateDTO,
  SupplierCategoryDTO,
  SupplierChangeLogDTO,
  SupplierContactDTO,
  SupplierDTO,
  SupplierEvaluationDTO,
  SupplierPage,
  SupplierPerformanceDTO,
  SupplierProfileDTO,
  SupplierStatsDTO,
} from "@/lib/api/contracts/supplier";
import type {
  PageResult,
  Supplier,
  SupplierBankAccount,
  SupplierCertificate,
  SupplierCategory,
  SupplierChangeLog,
  SupplierContact,
  SupplierEvaluation,
  SupplierPerformance,
  SupplierProfile,
  SupplierStats,
} from "../types";

export const formatDate = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "-";

/**
 * 供应商档案没有单一 rating 字段：后端回填了 `overallScore` 就用它，
 * 否则退回质量/交付/服务三项评分的均值，两者都按 5 星折算并保留 1 位小数。
 */
const toRating = (dto: SupplierDTO): number => {
  if (typeof dto.overallScore === "number") {
    return Math.round((dto.overallScore / 20) * 10) / 10;
  }
  const scores = [dto.qualityScore, dto.deliveryScore, dto.serviceScore].filter(
    (score): score is number => typeof score === "number"
  );
  if (scores.length === 0) return 0;
  const average = scores.reduce((sum, score) => sum + score, 0) / scores.length;
  return Math.round((average / 20) * 10) / 10;
};

/**
 * 掩掉除后 4 位以外的所有数字，每 4 位一组用 "****" 表示。
 * "6222021234567890"（16 位）→ 掩掉 12 位 → 3 组 → "**** **** **** 7890"
 */
export const maskBankAccount = (account?: string | null): string => {
  if (!account) return "-";
  if (account.length <= 4) return "****";
  const maskedLength = account.length - 4;
  const groups = Math.ceil(maskedLength / 4);
  const mask = Array.from({ length: groups }, () => "****").join(" ");
  return `${mask} ${account.slice(-4)}`;
};

/**
 * 距到期天数：没有到期日返回 null，已经过期返回负数。
 */
export const daysToExpiryOf = (
  validTo?: string | null,
  today?: Date
): number | null => {
  if (!validTo) return null;
  return dayjs(validTo).startOf("day").diff(dayjs(today).startOf("day"), "day");
};

export const toSupplier = (dto: SupplierDTO): Supplier => ({
  id: dto.id,
  name: dto.supplierName ?? "-",
  code: dto.supplierCode ?? "-",
  contactPerson: dto.contactName ?? "-",
  contactPhone: dto.contactPhone ?? "-",
  email: dto.contactEmail ?? undefined,
  address: dto.contactAddress ?? undefined,
  status: dto.enabled === false ? "disabled" : "active",
  rating: toRating(dto),
  overallScore: dto.overallScore ?? undefined,
  supplierLevel: dto.supplierLevel ?? undefined,
  auditStatus: dto.auditStatus ?? undefined,
  riskLevel: dto.riskLevel ?? undefined,
  blacklisted: dto.blacklisted ?? false,
  cooperationStatus: dto.cooperationStatus ?? undefined,
  creditRating: dto.creditRating ?? undefined,
  taxNo: dto.taxNo ?? undefined,
  supplierType: dto.supplierType ?? undefined,
  remark: dto.remark ?? undefined,
  createdAt: formatDate(dto.createTime),
});

export const toSupplierPage = (
  page: SupplierPage | null | undefined
): PageResult<Supplier> => ({
  records: (page?.records ?? []).map(toSupplier),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});

export const toSupplierContact = (dto: SupplierContactDTO): SupplierContact => ({
  id: dto.id ?? "",
  supplierId: dto.supplierId ?? undefined,
  contactName: dto.contactName ?? undefined,
  contactTitle: dto.contactTitle ?? undefined,
  contactRole: dto.contactRole ?? undefined,
  contactPhone: dto.contactPhone ?? undefined,
  contactEmail: dto.contactEmail ?? undefined,
  contactAddress: dto.contactAddress ?? undefined,
  isPrimary: dto.isPrimary ?? undefined,
  enabled: dto.enabled ?? undefined,
  sortOrder: dto.sortOrder ?? undefined,
  createTime: dto.createTime ?? undefined,
  remark: dto.remark ?? undefined,
});

/**
 * 银行账号保留原值，脱敏在展示层用 `maskBankAccount` 做 —— 表单编辑时还要用原值回填。
 */
export const toSupplierBankAccount = (
  dto: SupplierBankAccountDTO
): SupplierBankAccount => ({
  id: dto.id ?? "",
  supplierId: dto.supplierId ?? undefined,
  bankName: dto.bankName ?? undefined,
  bankBranch: dto.bankBranch ?? undefined,
  bankAccount: dto.bankAccount ?? undefined,
  accountName: dto.accountName ?? undefined,
  currency: dto.currency ?? undefined,
  swiftCode: dto.swiftCode ?? undefined,
  isDefault: dto.isDefault ?? undefined,
  enabled: dto.enabled ?? undefined,
  sortOrder: dto.sortOrder ?? undefined,
  createTime: dto.createTime ?? undefined,
  remark: dto.remark ?? undefined,
});

export const toSupplierCertificate = (
  dto: SupplierCertificateDTO
): SupplierCertificate => ({
  id: dto.id ?? "",
  supplierId: dto.supplierId ?? undefined,
  certificateType: dto.certificateType ?? undefined,
  certificateName: dto.certificateName ?? undefined,
  certificateNo: dto.certificateNo ?? undefined,
  issuingAuthority: dto.issuingAuthority ?? undefined,
  validFrom: dto.validFrom ?? undefined,
  validTo: dto.validTo ?? undefined,
  fileUrl: dto.fileUrl ?? undefined,
  status: dto.status ?? undefined,
  verifiedBy: dto.verifiedBy ?? undefined,
  verifiedAt: dto.verifiedAt ?? undefined,
  createTime: dto.createTime ?? undefined,
  remark: dto.remark ?? undefined,
  daysToExpiry: dto.daysToExpiry ?? daysToExpiryOf(dto.validTo),
});

export const toSupplierCategory = (
  dto: SupplierCategoryDTO
): SupplierCategory => ({
  id: dto.id ?? "",
  categoryCode: dto.categoryCode ?? undefined,
  categoryName: dto.categoryName ?? undefined,
  parentId: dto.parentId ?? undefined,
  level: dto.level ?? undefined,
  enabled: dto.enabled ?? undefined,
  sortOrder: dto.sortOrder ?? undefined,
  createTime: dto.createTime ?? undefined,
  remark: dto.remark ?? undefined,
});

export const toSupplierEvaluation = (
  dto: SupplierEvaluationDTO
): SupplierEvaluation => ({
  id: dto.id ?? "",
  supplierId: dto.supplierId ?? undefined,
  evaluationPeriod: dto.evaluationPeriod ?? undefined,
  startDate: dto.startDate ?? undefined,
  endDate: dto.endDate ?? undefined,
  qualityScore: dto.qualityScore ?? undefined,
  deliveryScore: dto.deliveryScore ?? undefined,
  serviceScore: dto.serviceScore ?? undefined,
  priceScore: dto.priceScore ?? undefined,
  totalScore: dto.totalScore ?? undefined,
  purchaseCount: dto.purchaseCount ?? undefined,
  totalAmount: dto.totalAmount ?? undefined,
  onTimeDeliveryRate: dto.onTimeDeliveryRate ?? undefined,
  qualityPassRate: dto.qualityPassRate ?? undefined,
  evaluatorId: dto.evaluatorId ?? undefined,
  evaluatorName: dto.evaluatorName ?? undefined,
  evaluatedAt: dto.evaluatedAt ?? undefined,
  improvementSuggestions: dto.improvementSuggestions ?? undefined,
  createTime: dto.createTime ?? undefined,
  remark: dto.remark ?? undefined,
});

export const toSupplierChangeLog = (
  dto: SupplierChangeLogDTO
): SupplierChangeLog => ({
  id: dto.id ?? "",
  supplierId: dto.supplierId ?? undefined,
  changeType: dto.changeType ?? undefined,
  changeContent: dto.changeContent ?? undefined,
  reason: dto.reason ?? undefined,
  operatorId: dto.operatorId ?? undefined,
  operatorName: dto.operatorName || "系统",
  createTime: dto.createTime ?? undefined,
});

export const toSupplierStats = (dto: SupplierStatsDTO): SupplierStats => ({
  total: dto.total,
  enabled: dto.enabled,
  cooperating: dto.cooperating,
  pendingAudit: dto.pendingAudit,
  blacklisted: dto.blacklisted,
  expiringCertificates: dto.expiringCertificates,
  byCooperationStatus: dto.byCooperationStatus,
  byAuditStatus: dto.byAuditStatus,
});

export const toSupplierPerformance = (
  dto: SupplierPerformanceDTO
): SupplierPerformance => ({
  supplierId: dto.supplierId,
  supplierName: dto.supplierName ?? undefined,
  supplierLevel: dto.supplierLevel ?? undefined,
  overallScore: dto.overallScore ?? undefined,
  qualityScore: dto.qualityScore ?? undefined,
  deliveryScore: dto.deliveryScore ?? undefined,
  serviceScore: dto.serviceScore ?? undefined,
  priceScore: dto.priceScore ?? undefined,
  evaluationCount: dto.evaluationCount,
  latestPeriod: dto.latestPeriod ?? undefined,
  latestTotalScore: dto.latestTotalScore ?? undefined,
  latestEvaluatedAt: dto.latestEvaluatedAt ?? undefined,
  suggestedLevel: dto.suggestedLevel ?? undefined,
  recentEvaluations: (dto.recentEvaluations ?? []).map(toSupplierEvaluation),
});

export const toSupplierProfile = (dto: SupplierProfileDTO): SupplierProfile => ({
  supplier: toSupplier(dto.supplier),
  contacts: (dto.contacts ?? []).map(toSupplierContact),
  bankAccounts: (dto.bankAccounts ?? []).map(toSupplierBankAccount),
  certificates: (dto.certificates ?? []).map(toSupplierCertificate),
  categories: (dto.categories ?? []).map(toSupplierCategory),
  changeLogs: (dto.changeLogs ?? []).map(toSupplierChangeLog),
});
