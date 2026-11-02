"use client";

import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { TableEmpty, TableLoading } from "@/components/ui/table-shell";

import { useTenantLogs } from "../hooks";
import { formatDateTime } from "../lib/format";

export function TenantLogPanel({ tenantId }: { tenantId: string }) {
  const { data, isLoading } = useTenantLogs(tenantId);
  const rows = data ?? [];

  if (isLoading) return <TableLoading />;
  if (rows.length === 0) return <TableEmpty>暂无操作日志</TableEmpty>;

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>时间</TableHead>
          <TableHead>操作类型</TableHead>
          <TableHead>模块</TableHead>
          <TableHead className="min-w-[220px]">描述</TableHead>
          <TableHead>操作人</TableHead>
          <TableHead>请求</TableHead>
          <TableHead className="text-center">状态</TableHead>
          <TableHead numeric>耗时</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((item) => (
          <TableRow key={item.id}>
            <TableCell>{formatDateTime(item.createTime)}</TableCell>
            <TableCell>{item.operationType ?? "-"}</TableCell>
            <TableCell>{item.operationModule ?? "-"}</TableCell>
            <TableCell className="max-w-[320px] truncate">
              {item.operationDesc ?? "-"}
            </TableCell>
            <TableCell>
              {item.operatorName ?? item.operatorId ?? "-"}
              {item.operatorIp && (
                <span className="text-muted-foreground ml-1 text-[10px]">
                  {item.operatorIp}
                </span>
              )}
            </TableCell>
            <TableCell className="font-mono text-[10px]">
              {item.requestMethod ?? "-"} {item.requestUrl ?? ""}
            </TableCell>
            <TableCell className="text-center">
              <Badge
                variant={
                  (item.responseStatus ?? 0) >= 200 &&
                  (item.responseStatus ?? 0) < 300
                    ? "success"
                    : "destructive"
                }
              >
                {item.responseStatus ?? "-"}
              </Badge>
            </TableCell>
            <TableCell numeric>
              {item.executionTime === null || item.executionTime === undefined
                ? "-"
                : `${item.executionTime} ms`}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
