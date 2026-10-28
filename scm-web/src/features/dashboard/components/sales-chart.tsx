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
import type { SalesTrendData } from "../types";

interface SalesChartProps {
  data: SalesTrendData;
  loading?: boolean;
}

export default function SalesChart({ data, loading }: SalesChartProps) {
  const tokens = useThemeTokens();

  const option = useMemo(
    () => ({
      tooltip: {
        trigger: "axis" as const,
        backgroundColor: tokens.bgElevated,
        borderColor: tokens.borderSecondary,
        textStyle: { color: tokens.text },
      },
      legend: {
        data: data.series.map((series) => series.name),
        bottom: 0,
        textStyle: { color: tokens.textSecondary },
      },
      grid: { left: "3%", right: "4%", bottom: "12%", containLabel: true },
      xAxis: {
        type: "category" as const,
        boundaryGap: false,
        data: data.dates,
        axisLine: { lineStyle: { color: tokens.borderSecondary } },
        axisLabel: { color: tokens.textSecondary },
      },
      yAxis: {
        type: "value" as const,
        splitLine: {
          lineStyle: { color: tokens.borderSecondary, type: "dashed" as const },
        },
        axisLabel: { color: tokens.textSecondary },
      },
      series: data.series.map((series, index) => {
        // Series colours come from the theme so they flip with dark mode;
        // the data layer stays colour-agnostic.
        const color = tokens.chart[index % tokens.chart.length];
        return {
          name: series.name,
          type: "line",
          smooth: true,
          data: series.data,
          itemStyle: { color },
          areaStyle: {
            color: {
              type: "linear" as const,
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: `${color}33` },
                { offset: 1, color: `${color}05` },
              ],
            },
          },
        };
      }),
    }),
    [data, tokens],
  );

  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>销售趋势</CardTitle>
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
