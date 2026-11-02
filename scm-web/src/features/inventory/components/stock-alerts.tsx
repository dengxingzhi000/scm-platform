"use client";

import { RefreshCw } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { useResolveStockAlert, useScanStockAlerts, useStockAlerts } from "../hooks";
import type { StockAlert } from "../types";

const statusConfig: Record<
  StockAlert["status"],
  {
    label: string;
    variant: "warning" | "secondary";
    accent: string;
    bar: string;
  }
> = {
  low: {
    label: "偏低",
    variant: "warning",
    accent: "var(--color-warning)",
    bar: "bg-[var(--color-warning)]",
  },
  out: {
    label: "缺货",
    variant: "secondary",
    accent: "var(--color-border-strong)",
    bar: "bg-muted-foreground",
  },
};

export default function StockAlerts() {
  const { data: alerts, isLoading, refetch } = useStockAlerts();
  const scan = useScanStockAlerts();
  const resolve = useResolveStockAlert();
  const rows = alerts ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="库存运营"
        title="库存预警"
        description="库存扫描任务按可用库存与安全库存生成告警，可在此处理"
        actions={
          <div className="flex gap-2">
            <Button
              variant="outline"
              disabled={isLoading || scan.isPending}
              onClick={() => scan.mutate()}
            >
              扫描告警
            </Button>
            <Button
              variant="outline"
              disabled={isLoading}
              onClick={() => refetch()}
            >
              <RefreshCw />
              刷新
            </Button>
          </div>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            告警由后端扫描任务生成；库存恢复后会自动解除，也可手动处理
          </span>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {rows.length} 条未处理告警
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>当前没有偏低或缺货的 SKU</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[240px]">SKU</TableHead>
                <TableHead>仓库 / 库位</TableHead>
                <TableHead numeric>当前库存</TableHead>
                <TableHead numeric>安全库存</TableHead>
                <TableHead numeric>缺口</TableHead>
                <TableHead>水位</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((alert) => {
                const config = statusConfig[alert.status];
                const percent =
                  alert.threshold > 0
                    ? Math.round((alert.currentStock / alert.threshold) * 100)
                    : 100;
                const gap = Math.max(alert.threshold - alert.currentStock, 0);

                return (
                  <TableRow key={alert.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{ borderLeftColor: config.accent }}
                      >
                        <div className="font-mono text-[11px] font-semibold">
                          {alert.skuId}
                        </div>
                        <div className="text-muted-foreground mt-0.5 text-[10px]">
                          库位 {alert.locationCode || "-"}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-[11px]">
                      {alert.warehouseId}
                    </TableCell>
                    <TableCell numeric>{alert.currentStock}</TableCell>
                    <TableCell numeric>{alert.threshold}</TableCell>
                    <TableCell
                      numeric
                      className={
                        gap > 0
                          ? "text-[var(--color-error-text)]"
                          : undefined
                      }
                    >
                      {gap}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div className="bg-muted h-1.5 w-20 overflow-hidden rounded-full">
                          <div
                            className={cn("h-full rounded-full", config.bar)}
                            style={{ width: `${Math.min(percent, 100)}%` }}
                          />
                        </div>
                        <span className="text-muted-foreground text-[10px] tabular-nums">
                          {percent}%
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {alert.createdAt ?? "-"}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={resolve.isPending}
                        onClick={() => resolve.mutate(alert.id)}
                      >
                        处理
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </TablePanel>
    </div>
  );
}
