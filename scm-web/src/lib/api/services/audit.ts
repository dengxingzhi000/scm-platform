import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  AuditLogQuery,
  SensitiveLogQuery,
  SysAuditLogPage,
  SysSensitiveOperationLogPage,
} from "../contracts/audit";

const AUDIT_BASE = "/scm-audit";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

export const auditApiService = {
  pageAuditLogs: (query: AuditLogQuery) =>
    get<SysAuditLogPage>(`${AUDIT_BASE}/sys-audit-log/page`, query),
  pageSensitiveLogs: (query: SensitiveLogQuery) =>
    get<SysSensitiveOperationLogPage>(
      `${AUDIT_BASE}/sys-sensitive-operation-log/page`,
      query,
    ),
};
