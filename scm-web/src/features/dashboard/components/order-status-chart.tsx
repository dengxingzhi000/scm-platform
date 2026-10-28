"use client";

import { useMemo } from "react";

import EChart from "@/components/charts/echart";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useThemeTokens } from "@/hooks/use-theme-tokens";
import type { OrderStatusData } from "../types";

interface OrderStatusChartProps {
  data: OrderStatusData[];
  loading?: boolean;
}

export default function OrderStatusChart({
  data,
  loading,
}: OrderStatusChartProps) {
  const tokens = useThemeTokens();

  const option = useMemo(
    () => ({
      tooltip: {
        trigger: "item" as const,
        formatter: "{b}: {c} ({d}%)",
        backgroundColor: tokens.bgElevated,
        borderColor: tokens.borderSecondary,
        textStyle: { color: tokens.text },
      },
      legend: {
        bottom: 0,
        data: data.map((item) => item.status),
        textStyle: { color: tokens.textSecondary },
      },
      series: [
        {
          type: "pie",
          radius: ["40%", "70%"],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: tokens.bgContainer,
            borderWidth: 2,
          },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: "bold" as const },
          },
          data: data.map((item, index) => ({
            name: item.status,
            value: item.count,
            itemStyle: { color: tokens.chart[index % tokens.chart.length] },
          })),
        },
      ],
    }),
    [data, tokens],
  );

  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>订单状态分布</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <Skeleton className="h-[300px] w-full" />
        ) : (
          <EChart option={option} className="h-[300px]" />
        )}
      </CardContent>
    </Card>
  );
}
