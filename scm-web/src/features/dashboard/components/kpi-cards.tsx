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
          <Skeleton key={index} className="h-[132px] w-full rounded-lg" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {kpis.map((kpi, index) => {
        const Icon = iconMap[kpi.icon] ?? DollarSign;
        const color = tokens.chart[index % tokens.chart.length];
        const TrendIcon =
          kpi.trend?.direction === "up"
            ? ArrowUpRight
            : kpi.trend?.direction === "down"
              ? ArrowDownRight
              : Minus;

        return (
          <Card
            key={`${kpi.title}-${index}`}
            className="relative overflow-hidden py-5"
          >
            <CardContent className="flex items-center gap-4 px-5">
              <div
                className="flex size-12 shrink-0 items-center justify-center rounded-xl"
                style={{
                  backgroundColor: `${color}14`,
                  color,
                }}
              >
                <Icon className="size-5" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-muted-foreground mb-1 text-[11px] font-medium tracking-[0.04em]">
                  {kpi.title}
                </p>
                <p className="text-[28px] leading-none font-semibold tracking-[-0.02em] tabular-nums">
                  {kpi.prefix}
                  {kpi.value}
                  {kpi.suffix}
                </p>
                {kpi.trend && (
                  <p
                    className={cn(
                      "mt-1 flex items-center gap-1 text-[11px] whitespace-nowrap",
                      kpi.trend.direction === "up" && "text-[var(--color-success-text)]",
                      kpi.trend.direction === "down" && "text-destructive",
                      kpi.trend.direction === "flat" && "text-muted-foreground",
                    )}
                  >
                    <TrendIcon className="size-3.5" />
                    {kpi.trend.direction === "up"
                      ? "上升"
                      : kpi.trend.direction === "down"
                        ? "下降"
                        : "持平"}{" "}
                    {kpi.trend.value}% {kpi.trend.period}
                  </p>
                )}
              </div>

              {kpi.sparkline && kpi.sparkline.length > 1 && (
                <Sparkline
                  data={kpi.sparkline}
                  color={color}
                  className="pointer-events-none absolute right-0 bottom-0 h-12 w-24 opacity-70"
                />
              )}
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
