"use client";

import dayjs from "dayjs";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";

import { PageHeader } from "@/components/ui/page-header";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import AttentionStrip from "@/features/dashboard/components/attention-strip";
import InventoryAlerts from "@/features/dashboard/components/inventory-alerts";
import KPICards from "@/features/dashboard/components/kpi-cards";
import OrderStatusBreakdown from "@/features/dashboard/components/order-status-breakdown";
import RecentOrders from "@/features/dashboard/components/recent-orders";
import SalesChart from "@/features/dashboard/components/sales-chart";
import { useDashboardStats } from "@/features/dashboard/hooks";
import type { SalesTrendPoint } from "@/features/dashboard/types";
import type {
  OrderDashboardCompare,
  OrderDashboardRange,
} from "@/lib/api/contracts/order";
import { cn } from "@/lib/utils";
import { useState } from "react";

const RANGES: Array<{ value: OrderDashboardRange; label: string }> = [
  { value: "TODAY", label: "今日" },
  { value: "LAST_7_DAYS", label: "近 7 天" },
  { value: "LAST_30_DAYS", label: "近 30 天" },
];

const COMPARES: Array<{ value: OrderDashboardCompare; label: string }> = [
  { value: "PREVIOUS_PERIOD", label: "环比" },
  { value: "YEAR_OVER_YEAR", label: "同比" },
  { value: "NONE", label: "不对比" },
];

export default function DashboardClient() {
  const router = useRouter();
  const locale = useLocale();
  const [range, setRange] = useState<OrderDashboardRange>("LAST_7_DAYS");
  const [compare, setCompare] =
    useState<OrderDashboardCompare>("PREVIOUS_PERIOD");

  const {
    data,
    isLoading,
    isError,
    isFetching,
    isPlaceholderData,
    refetch,
    dataUpdatedAt,
  } = useDashboardStats(range, compare);

  const drillIntoPoint = (point: SalesTrendPoint) => {
    const start = dayjs(point.startTime).format("YYYY-MM-DDTHH:mm:ss");
    // Bucket ends are exclusive; the order query uses an inclusive upper bound.
    const end = dayjs(point.endTime)
      .subtract(1, "second")
      .format("YYYY-MM-DDTHH:mm:ss");
    router.push(
      `/${locale}/order?startTime=${encodeURIComponent(start)}&endTime=${encodeURIComponent(end)}`
    );
  };

  return (
    <div
      className={cn(
        "flex flex-col gap-4 transition-opacity",
        isPlaceholderData && "opacity-60"
      )}
    >
      <PageHeader
        eyebrow="运营驾驶舱"
        title="仪表盘"
        description="关键经营指标、履约进度与库存异常，可切换时间范围并下钻到订单"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <div
              role="group"
              aria-label="时间范围"
              className="inline-flex rounded-md border border-[var(--color-border-secondary)] p-0.5"
            >
              {RANGES.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  aria-pressed={range === item.value}
                  onClick={() => setRange(item.value)}
                  className={cn(
                    "rounded px-2.5 py-1 text-[12px] transition-colors",
                    range === item.value
                      ? "bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]"
                      : "text-muted-foreground hover:bg-sunken"
                  )}
                >
                  {item.label}
                </button>
              ))}
            </div>

            <Select
              value={compare}
              onValueChange={(value) =>
                setCompare(value as OrderDashboardCompare)
              }
            >
              <SelectTrigger size="sm" className="w-24 text-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {COMPARES.map((item) => (
                  <SelectItem key={item.value} value={item.value}>
                    {item.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <span className="text-muted-foreground text-[11px] tabular-nums">
              {dataUpdatedAt
                ? `更新于 ${dayjs(dataUpdatedAt).format("HH:mm:ss")}`
                : "尚未更新"}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void refetch()}
              disabled={isFetching}
            >
              <RefreshCw className={isFetching ? "animate-spin" : undefined} />
              刷新
            </Button>
          </div>
        }
      />

      {isError && !data ? (
        <div
          role="alert"
          className="flex flex-col items-center gap-2 rounded-lg border border-[var(--color-error)]/30 bg-[var(--color-error-soft)] px-4 py-10 text-center"
        >
          <AlertTriangle className="size-6 text-[var(--color-error-text)]" />
          <p className="text-[14px] font-medium text-[var(--color-error-text)]">
            仪表盘数据加载失败
          </p>
          <p className="text-[12px] text-[var(--color-error-text)]/80">
            请检查网络连接或稍后重试
          </p>
          <Button size="sm" variant="outline" onClick={() => void refetch()}>
            重新加载
          </Button>
        </div>
      ) : (
        <>
          {data && data.dataIssues.length > 0 && (
            <div
              role="status"
              className="flex flex-wrap items-center gap-2 rounded-lg border border-[var(--color-warning)]/30 bg-[var(--color-warning-soft)] px-3 py-2 text-[12px] text-[var(--color-warning-text)]"
            >
              <AlertTriangle className="size-4 shrink-0" />
              <span>
                部分数据未能加载：{data.dataIssues.join("、")}
                ，相关指标可能不完整。
              </span>
              <button
                type="button"
                className="ml-auto font-medium underline underline-offset-2"
                onClick={() => void refetch()}
              >
                重试
              </button>
            </div>
          )}

          <AttentionStrip
            attention={data?.attention}
            loading={isLoading}
            issues={data?.dataIssues}
          />

          <KPICards kpis={data?.kpis ?? []} loading={isLoading} />

          <div className="grid gap-4 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <SalesChart
                data={
                  data?.salesTrend ?? { granularity: "DAY", points: [] }
                }
                rangeLabel={data?.rangeLabel ?? "近 7 天"}
                loading={isLoading}
                onPointClick={drillIntoPoint}
              />
            </div>
            <OrderStatusBreakdown
              data={data?.orderStatus ?? []}
              loading={isLoading}
            />
          </div>

          <div className="grid gap-4 xl:grid-cols-12">
            <div className="xl:col-span-7">
              <RecentOrders
                orders={data?.recentOrders ?? []}
                loading={isLoading}
              />
            </div>
            <div className="xl:col-span-5">
              <InventoryAlerts
                alerts={data?.inventoryAlerts ?? []}
                loading={isLoading}
                available={data?.alertsAvailable ?? true}
              />
            </div>
          </div>
        </>
      )}
    </div>
  );
}
