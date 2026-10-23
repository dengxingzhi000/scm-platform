"use client";

import { AlertTriangle } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardAction,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import type { InventoryAlert } from "../types";

interface InventoryAlertsProps {
  alerts: InventoryAlert[];
  loading?: boolean;
}

const statusConfig = {
  critical: { label: "紧急", badge: "destructive" as const, bar: "bg-destructive" },
  low: { label: "偏低", badge: "warning" as const, bar: "bg-amber-500" },
  out: { label: "缺货", badge: "secondary" as const, bar: "bg-muted-foreground" },
};

export default function InventoryAlerts({
  alerts,
  loading,
}: InventoryAlertsProps) {
  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>库存预警</CardTitle>
        <CardAction>
          <AlertTriangle className="size-4 text-amber-500" />
        </CardAction>
      </CardHeader>
      <CardContent>
        {loading ? (
          <Skeleton className="h-64 w-full" />
        ) : (
          <div className="divide-y">
            {alerts.map((alert) => {
              const config =
                statusConfig[alert.status] ?? statusConfig.low;
              const percent = Math.round(
                (alert.currentStock / alert.threshold) * 100,
              );

              return (
                <div
                  key={alert.id}
                  className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
                >
                  <div className="min-w-0">
                    <p className="flex items-center gap-2 truncate text-sm font-medium">
                      {alert.productName}
                      <Badge variant={config.badge}>{config.label}</Badge>
                    </p>
                    <p className="text-muted-foreground mt-1 text-xs">
                      {alert.skuCode} · 库存 {alert.currentStock} / 阈值{" "}
                      {alert.threshold}
                    </p>
                  </div>
                  <div className="bg-muted h-1.5 w-24 shrink-0 overflow-hidden rounded-full">
                    <div
                      className={cn("h-full rounded-full", config.bar)}
                      style={{ width: `${Math.min(percent, 100)}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
