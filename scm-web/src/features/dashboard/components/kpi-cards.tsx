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
  if (loading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <Skeleton key={index} className="h-[132px] w-full rounded-xl" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {kpis.map((kpi, index) => {
        const Icon = iconMap[kpi.icon] ?? DollarSign;
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
                  backgroundColor: `${kpi.color}14`,
                  color: kpi.color,
                }}
              >
                <Icon className="size-5" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-muted-foreground mb-1 text-xs">{kpi.title}</p>
                <p className="text-2xl leading-none font-semibold tracking-tight">
                  {kpi.prefix}
                  {kpi.value}
                  {kpi.suffix}
                </p>
                {kpi.trend && (
                  <p
                    className={cn(
                      "mt-1 flex items-center gap-1 text-[11px] whitespace-nowrap",
                      kpi.trend.direction === "up" && "text-emerald-600",
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
                  color={kpi.color}
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
