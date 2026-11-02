import type { MpPage } from "./page";

/** `com.scmcloud.audit.domain.entity.SysAuditLog`. */
export interface SysAuditLogDTO {
  id: string;
  userId?: string | null;
  username?: string | null;
  realName?: string | null;
  deptId?: string | null;
  operationType?: string | null;
  operationModule?: string | null;
  operationDesc?: string | null;
  requestUri?: string | null;
  requestMethod?: string | null;
  requestParams?: string | null;
  responseData?: string | null;
  responseStatus?: number | null;
  ipAddress?: string | null;
  location?: string | null;
  userAgent?: string | null;
  businessType?: string | null;
  businessId?: string | null;
  oldValue?: string | null;
  newValue?: string | null;
  riskLevel?: number | null;
  status?: number | null;
  errorMsg?: string | null;
  executeTime?: number | null;
  createTime?: string | null;
}

/** `com.scmcloud.audit.domain.entity.SysSensitiveOperationLog`. */
export interface SysSensitiveOperationLogDTO {
  id: string;
  userId?: string | null;
  username?: string | null;
  operationType?: string | null;
  operationModule?: string | null;
  sensitiveDataType?: string | null;
  dataFingerprint?: string | null;
  affectedCount?: number | null;
  targetTable?: string | null;
  targetIds?: string | null;
  operationDetail?: string | null;
  approvalRequired?: boolean | null;
  approvalId?: string | null;
  riskScore?: number | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  deviceFingerprint?: string | null;
  location?: string | null;
  createTime?: string | null;
  realName?: string | null;
  deptName?: string | null;
}

export type SysAuditLogPage = MpPage<SysAuditLogDTO>;
export type SysSensitiveOperationLogPage = MpPage<SysSensitiveOperationLogDTO>;

export interface AuditLogQuery {
  page: number;
  size: number;
  userId?: string;
  operationType?: string;
  module?: string;
  businessType?: string;
  riskLevel?: number;
}

export interface SensitiveLogQuery {
  page: number;
  size: number;
  userId?: string;
  operationType?: string;
  riskScore?: number;
}

export const RISK_LEVEL: Record<
  number,
  { label: string; variant: "secondary" | "info" | "warning" | "destructive" }
> = {
  1: { label: "低风险", variant: "secondary" },
  2: { label: "中风险", variant: "info" },
  3: { label: "高风险", variant: "warning" },
  4: { label: "严重", variant: "destructive" },
};
