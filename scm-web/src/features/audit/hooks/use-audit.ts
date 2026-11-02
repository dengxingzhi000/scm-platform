"use client";

import { useQuery } from "@tanstack/react-query";

import { auditApiService } from "@/lib/api/services/audit";
import type { AuditLogQuery, SensitiveLogQuery } from "@/lib/api/contracts/audit";

export function useAuditLogPage(query: AuditLogQuery) {
  return useQuery({
    queryKey: ["audit", "logs", query],
    queryFn: () => auditApiService.pageAuditLogs(query),
  });
}

export function useSensitiveLogPage(query: SensitiveLogQuery) {
  return useQuery({
    queryKey: ["audit", "sensitive", query],
    queryFn: () => auditApiService.pageSensitiveLogs(query),
  });
}
