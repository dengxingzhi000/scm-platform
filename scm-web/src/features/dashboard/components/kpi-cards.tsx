"use client";

import {
  ArrowDownRight,
  ArrowUpRight,
  DollarSign,
  Inbox,
  Minus,
  ShoppingCart,
  Users,
  type LucideIcon,
} from "lucide-react";

import Sparkline from "@/components/charts/sparkline";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useThemeTokens } from "@/hooks/use-theme-tokens";
import { cn } from "@/lib/utils";
import type { KPIData } from "../types";

const iconMap: Record<string, LucideIcon> = {
  DollarOutlined: DollarSign,
  ShoppingCartOutlined: ShoppingCart,
  InboxOutlined: Inbox,
  TeamOutlined: Users,
};

const TREND_CLASS = {
  up: "bg-[var(--color-success-soft)] text-[var(--color-success-text)]",
  down: "bg-[var(--color-error-soft)] text-[var(--color-error-text)]",
  flat: "bg-sunken text-muted-foreground",
} as const;

interface KPICardsProps {
  kpis: KPIData[];
  loading?: boolean;
}

export default function KPICards({ kpis, loading }: KPICardsProps) {
  const tokens = useThemeTokens();

  if (loading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <Skeleton key={index} className="h-[148px] w-full rounded-lg" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {kpis.map((kpi, index) => {
        const Icon = iconMap[kpi.icon] ?? DollarSign;
        const color = tokens.chart[index % tokens.chart.length];
        const direction = kpi.trend?.direction ?? "flat";
        const TrendIcon =
          direction === "up"
            ? ArrowUpRight
            : direction === "down"
              ? ArrowDownRight
              : Minus;

        return (
          <Card key={`${kpi.title}-${index}`} className="gap-0 overflow-hidden py-0">
            <CardContent className="px-5 pt-4 pb-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-muted-foreground text-[11px] font-medium tracking-[0.04em]">
                    {kpi.title}
                  </p>
                  <p className="mt-1.5 flex items-baseline gap-1 text-[30px] leading-none font-semibold tracking-[-0.03em] tabular-nums">
                    <span className="truncate">
                      {kpi.prefix}
                      {kpi.value}
                    </span>
                    {kpi.suffix && (
                      <span className="text-muted-foreground text-[12px] font-medium">
                        {kpi.suffix}
                      </span>
                    )}
                  </p>
                </div>
                <span
                  className="flex size-9 shrink-0 items-center justify-center rounded-lg"
                  style={{ backgroundColor: `${color}14`, color }}
                >
                  <Icon className="size-4" />
                </span>
              </div>

              <div className="mt-2.5 flex min-h-[18px] flex-wrap items-center gap-1.5 text-[11px]">
                {kpi.trend && (
                  <span
                    className={cn(
                      "inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[10px] font-medium tabular-nums",
                      TREND_CLASS[direction],
                    )}
                  >
                    <TrendIcon className="size-3" />
                    <span className="sr-only">
                      {direction === "up"
                        ? "上升"
                        : direction === "down"
                          ? "下降"
                          : "持平"}
                    </span>
                    {kpi.trend.value}%
                  </span>
                )}
                {kpi.trend && (
                  <span className="text-muted-foreground text-[10px]">
                    {kpi.trend.period}
                  </span>
                )}
                {kpi.hint && (
                  <span className="text-muted-foreground truncate text-[10px]">
                    {kpi.trend ? "· " : ""}
                    {kpi.hint}
                  </span>
                )}
              </div>
            </CardContent>

            {kpi.sparkline && kpi.sparkline.length > 1 && (
              <Sparkline
                data={kpi.sparkline}
                color={color}
                className="pointer-events-none h-10 w-full opacity-60"
              />
            )}
          </Card>
        );
      })}
    </div>
  );
}
