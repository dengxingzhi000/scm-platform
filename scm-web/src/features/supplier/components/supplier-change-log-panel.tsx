"use client";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useSupplierChangeLogs } from "../hooks";
import { CHANGE_TYPE_LABELS } from "../lib/constants";
import { formatDate } from "../lib/mappers";

interface SupplierChangeLogPanelProps {
  supplierId: string;
}

/** spec §5.6 —— 该分区展示最近 50 条留痕。 */
const CHANGE_LOG_LIMIT = 50;

/** 缺失值的统一占位 —— 与其他分区一致。 */
const MISSING = "-";

export default function SupplierChangeLogPanel({
  supplierId,
}: SupplierChangeLogPanelProps) {
  const { data: logs, isLoading } = useSupplierChangeLogs(
    supplierId,
    CHANGE_LOG_LIMIT,
  );

  const rows = logs ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>变更留痕</CardTitle>
        <CardDescription>
          该供应商档案的创建、更新与审核记录，由服务端按时间倒序返回。
        </CardDescription>
      </CardHeader>

      <CardContent>
        {isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-3 w-32" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-24" />
          </div>
        ) : rows.length === 0 ? (
          <p className="text-muted-foreground text-xs">暂无变更记录。</p>
        ) : (
          <ol className="ml-1 border-l border-[var(--color-border-secondary)] pl-4">
            {rows.map((log) => (
              <li key={log.id} className="relative pb-4 last:pb-0">
                <span
                  aria-hidden
                  className="bg-[var(--color-primary)] absolute -left-5 top-1.5 size-2 rounded-full"
                />
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant="secondary">
                    {CHANGE_TYPE_LABELS[log.changeType ?? ""] ?? MISSING}
                  </Badge>
                  <span className="text-muted-foreground font-mono text-[11px]">
                    {formatDate(log.createTime)}
                  </span>
                </div>
                <p className="mt-1 text-[12px]">{log.changeContent ?? MISSING}</p>
                {log.reason && (
                  <p className="text-muted-foreground text-[11px]">
                    原因：{log.reason}
                  </p>
                )}
                <p className="text-muted-foreground text-[11px]">
                  {log.operatorName || "系统"}
                </p>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  );
}
