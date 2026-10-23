"use client";

import { useMemo } from "react";

import EChart from "@/components/charts/echart";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useThemeTokens } from "@/hooks/use-theme-tokens";
import type { DatasetQueryColumn, DatasetQueryResult } from "../types";

interface AnalyticsChartProps {
  result?: DatasetQueryResult;
  dimensions: string[];
  metrics: string[];
  loading?: boolean;
}

function labelOf(columns: DatasetQueryColumn[], code: string) {
  return columns.find((column) => column.code === code)?.code ?? code;
}

export default function AnalyticsChart({
  result,
  dimensions,
  metrics,
  loading,
}: AnalyticsChartProps) {
  const tokens = useThemeTokens();

  const option = useMemo(() => {
    if (!result || result.rows.length === 0) return null;

    const xField = dimensions[0];
    const categories = result.rows.map((row, index) =>
      xField ? String(row[xField] ?? "") : `#${index + 1}`,
    );
    const series = metrics.map((metric) => ({
      name: labelOf(result.columns, metric),
      type: "bar" as const,
      data: result.rows.map((row) => {
        const value = row[metric];
        return typeof value === "number" ? value : Number(value ?? 0);
      }),
    }));

    return {
      tooltip: {
        trigger: "axis" as const,
        backgroundColor: tokens.bgElevated,
        borderColor: tokens.borderSecondary,
        textStyle: { color: tokens.text },
      },
      legend: {
        data: series.map((item) => item.name),
        bottom: 0,
        textStyle: { color: tokens.textSecondary },
      },
      grid: { left: "3%", right: "4%", bottom: "12%", containLabel: true },
      xAxis: {
        type: "category" as const,
        data: categories,
        axisLine: { lineStyle: { color: tokens.borderSecondary } },
        axisLabel: {
          color: tokens.textSecondary,
          rotate: categories.length > 8 ? 30 : 0,
        },
      },
      yAxis: {
        type: "value" as const,
        splitLine: {
          lineStyle: { color: tokens.borderSecondary, type: "dashed" as const },
        },
        axisLabel: { color: tokens.textSecondary },
      },
      series,
    };
  }, [dimensions, metrics, result, tokens]);

  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>趋势图</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="text-muted-foreground flex h-80 items-center justify-center text-sm">
            加载中…
          </div>
        ) : option ? (
          <EChart option={option} className="h-80" />
        ) : (
          <div className="text-muted-foreground flex h-80 items-center justify-center text-sm">
            运行查询后展示图表
          </div>
        )}
      </CardContent>
    </Card>
  );
}
