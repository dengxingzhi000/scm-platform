"use client";

import { AlertTriangle } from "lucide-react";
import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { InventoryAlert } from "../types";

interface InventoryAlertsProps {
  alerts: InventoryAlert[];
  loading?: boolean;
  /** False when the alert source failed — distinct from "no SKU is short". */
  available?: boolean;
}

const statusConfig = {
  critical: {
    label: "紧急",
    badge: "destructive" as const,
    accent: "var(--color-error)",
    bar: "bg-destructive",
  },
  low: {
    label: "偏低",
    badge: "warning" as const,
    accent: "var(--color-warning)",
    bar: "bg-[var(--color-warning)]",
  },
  out: {
    label: "缺货",
    badge: "secondary" as const,
    accent: "var(--color-border-strong)",
    bar: "bg-muted-foreground",
  },
};

export default function InventoryAlerts({
  alerts,
  loading,
  available = true,
}: InventoryAlertsProps) {
  const router = useRouter();
  const locale = useLocale();
  const severity: Record<InventoryAlert["status"], number> = {
    out: 0,
    critical: 1,
    low: 2,
  };
  const rows = [...alerts].sort(
    (a, b) => severity[a.status] - severity[b.status],
  );

  return (
    <Card className="gap-3 overflow-hidden">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <AlertTriangle className="size-4 text-[var(--color-warning)]" />
          库存预警
        </CardTitle>
        <CardDescription>缺货与低库存 SKU，按严重度排序</CardDescription>
        <CardAction>
          <div className="flex items-center gap-2">
            <Badge variant={alerts.length > 0 ? "warning" : "secondary"}>
              {available ? `${alerts.length} 个 SKU` : "数据不可用"}
            </Badge>
            <button
              type="button"
              className="text-primary text-sm hover:underline"
              onClick={() => router.push(`/${locale}/inventory/alerts`)}
            >
              查看全部
            </button>
          </div>
        </CardAction>
      </CardHeader>
      <CardContent className="px-0">
        {loading ? (
          <div className="px-6">
            <Skeleton className="h-56 w-full" />
          </div>
        ) : !available ? (
          <div className="text-muted-foreground px-6 py-10 text-center text-[13px]">
            库存预警数据不可用，暂时无法判断缺货与低库存
          </div>
        ) : rows.length === 0 ? (
          <div className="text-muted-foreground px-6 py-10 text-center text-[13px]">
            当前没有偏低或缺货的 SKU
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>商品 / SKU</TableHead>
                <TableHead numeric>库存</TableHead>
                <TableHead numeric>阈值</TableHead>
                <TableHead>水位</TableHead>
                <TableHead className="text-center">状态</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((alert) => {
                const config = statusConfig[alert.status] ?? statusConfig.low;
                const percent = Math.round(
                  (alert.currentStock / alert.threshold) * 100,
                );

                return (
                  <TableRow key={alert.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{ borderLeftColor: config.accent }}
                      >
                        <div className="max-w-[180px] truncate font-semibold">
                          {alert.productName}
                        </div>
                        <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                          {alert.skuCode}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell numeric>{alert.currentStock}</TableCell>
                    <TableCell numeric>{alert.threshold}</TableCell>
                    <TableCell>
                      <div className="bg-muted h-1.5 w-20 overflow-hidden rounded-full">
                        <div
                          className={cn("h-full rounded-full", config.bar)}
                          style={{ width: `${Math.min(percent, 100)}%` }}
                        />
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.badge}>{config.label}</Badge>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
