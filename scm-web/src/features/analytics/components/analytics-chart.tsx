"use client";

import { useMemo } from "react";
import { ChartColumn } from "lucide-react";

import EChart from "@/components/charts/echart";
import { Skeleton } from "@/components/ui/skeleton";
import { useThemeTokens } from "@/hooks/use-theme-tokens";
import type { EChartsCoreOption } from "@/lib/charts/echarts-core";
import type { DatasetQueryColumn, DatasetQueryResult } from "../types";

export type AnalyticsChartType = "bar" | "line" | "radar" | "scatter";

interface AnalyticsChartProps {
  result?: DatasetQueryResult;
  dimensions: string[];
  metrics: string[];
  labels: Record<string, string>;
  type: AnalyticsChartType;
  loading?: boolean;
}

function labelOf(
  columns: DatasetQueryColumn[],
  labels: Record<string, string>,
  code: string,
) {
  return labels[code] ?? columns.find((column) => column.code === code)?.code ?? code;
}

function axisMax(value: number) {
  if (value <= 0) return 1;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  return Math.ceil((value / magnitude) * 1.1) * magnitude;
}

function ChartPlaceholder({ text }: { text: string }) {
  return (
    <div className="border-border text-muted-foreground flex h-[420px] flex-col items-center justify-center gap-2 rounded-lg border border-dashed text-[12px]">
      <ChartColumn className="size-5" />
      {text}
    </div>
  );
}

export default function AnalyticsChart({
  result,
  dimensions,
  metrics,
  labels,
  type,
  loading,
}: AnalyticsChartProps) {
  const tokens = useThemeTokens();

  const option = useMemo<EChartsCoreOption | null>(() => {
    if (!result || result.rows.length === 0) return null;

    const rows = result.rows;
    const axisLabel = {
      color: tokens.textSecondary,
      fontSize: 10,
    };
    const splitLine = {
      lineStyle: { color: tokens.borderSecondary, type: "dashed" as const },
    };
    const tooltip = {
      backgroundColor: tokens.bgElevated,
      borderColor: tokens.borderSecondary,
      textStyle: { color: tokens.text, fontSize: 11 },
      extraCssText:
        "box-shadow: 0 4px 16px -6px rgba(0,0,0,0.18); border-radius: 8px;",
    };
    const legend = {
      type: "scroll" as const,
      bottom: 0,
      left: "center" as const,
      icon: "circle",
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: tokens.textSecondary, fontSize: 11 },
    };
    const dimension = dimensions[0];
    const rowLabel = (row: Record<string, unknown>, index: number) =>
      dimension ? String(row[dimension] ?? "") : `#${index + 1}`;

    if (type === "radar") {
      if (metrics.length < 3) return null;

      const indicators = metrics.map((metric) => ({
        name: labelOf(result.columns, labels, metric),
        max: axisMax(
          Math.max(
            ...rows.map((row) => Number(row[metric]) || 0),
            1,
          ) * 1.15,
        ),
      }));

      return {
        color: tokens.chart,
        tooltip: { ...tooltip, trigger: "item" as const },
        legend,
        radar: {
          center: ["50%", "44%"],
          radius: "78%",
          indicator: indicators,
          axisName: { color: tokens.textSecondary, fontSize: 10 },
          splitLine: { lineStyle: { color: tokens.borderSecondary } },
          splitArea: {
            areaStyle: { color: ["transparent"] },
          },
          axisLine: { lineStyle: { color: tokens.borderSecondary } },
        },
        series: [
          {
            type: "radar" as const,
            symbolSize: 4,
            data: rows.slice(0, 6).map((row, index) => ({
              name: rowLabel(row, index),
              value: metrics.map((metric) => Number(row[metric]) || 0),
              areaStyle: { opacity: 0.06 },
              lineStyle: { width: 1.5 },
            })),
          },
        ],
      };
    }

    if (type === "scatter") {
      if (metrics.length < 2) return null;

      const xMetric = metrics[0];
      const yMetric = metrics[1];
      const xLabel = labelOf(result.columns, labels, xMetric);
      const yLabel = labelOf(result.columns, labels, yMetric);

      return {
        color: tokens.chart,
        tooltip: {
          ...tooltip,
          trigger: "item" as const,
          formatter: (params: { data: { name: string; value: number[] } }) =>
            `${params.data.name}<br/>${xLabel}: ${params.data.value[0]}<br/>${yLabel}: ${params.data.value[1]}`,
        },
        grid: { left: 12, right: 24, bottom: 48, top: 32, containLabel: true },
        xAxis: {
          type: "value" as const,
          name: xLabel,
          nameLocation: "middle" as const,
          nameGap: 30,
          nameTextStyle: { color: tokens.textSecondary, fontSize: 10 },
          axisLabel,
          splitLine,
          axisLine: { lineStyle: { color: tokens.border } },
        },
        yAxis: {
          type: "value" as const,
          name: yLabel,
          nameTextStyle: { color: tokens.textSecondary, fontSize: 10 },
          axisLabel,
          splitLine,
          axisLine: { lineStyle: { color: tokens.border } },
        },
        series: [
          {
            type: "scatter" as const,
            symbolSize: 9,
            data: rows.map((row, index) => ({
              name: rowLabel(row, index),
              value: [Number(row[xMetric]) || 0, Number(row[yMetric]) || 0],
            })),
            itemStyle: { color: tokens.chart[0], opacity: 0.85 },
            emphasis: {
              itemStyle: { color: tokens.chart[1] },
              label: { show: true, color: tokens.text, fontSize: 10 },
            },
          },
        ],
      };
    }

    const categories = rows.map((row, index) => rowLabel(row, index));

    return {
      color: tokens.chart,
      tooltip: { ...tooltip, trigger: "axis" as const },
      legend,
      grid: { left: 8, right: 16, bottom: 44, top: 24, containLabel: true },
      xAxis: {
        type: "category" as const,
        data: categories,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: tokens.border } },
        axisLabel: {
          ...axisLabel,
          interval: 0,
          rotate: categories.length > 8 ? 30 : 0,
        },
      },
      yAxis: {
        type: "value" as const,
        axisLabel,
        splitLine,
        axisLine: { show: false },
      },
      series: metrics.map((metric) => ({
        name: labelOf(result.columns, labels, metric),
        type,
        data: rows.map((row) => Number(row[metric]) || 0),
        // A light monotone curve avoids the spline overshoot that would
        // otherwise draw values above/below the queried data.
        smooth: type === "line" ? 0.2 : false,
        smoothMonotone: type === "line" ? ("x" as const) : undefined,
        showSymbol: type === "line" && rows.length <= 30,
        symbol: "circle" as const,
        symbolSize: 5,
        barMaxWidth: 30,
        itemStyle: {
          borderRadius: type === "bar" ? [3, 3, 0, 0] : undefined,
        },
        lineStyle: { width: 2 },
      })),
    };
  }, [dimensions, labels, metrics, result, tokens, type]);

  if (loading) {
    return <Skeleton className="h-[420px] w-full" />;
  }

  if (metrics.length === 0) {
    return <ChartPlaceholder text="至少选择一个指标以展示图表" />;
  }

  if (!result || result.rows.length === 0) {
    return <ChartPlaceholder text="运行查询后展示图表" />;
  }

  if (type === "radar" && metrics.length < 3) {
    return <ChartPlaceholder text="雷达图至少需要 3 个指标" />;
  }

  if (type === "scatter" && metrics.length < 2) {
    return <ChartPlaceholder text="散点图至少需要 2 个指标" />;
  }

  if (!option) {
    return <ChartPlaceholder text="当前字段组合无法生成该图表" />;
  }

  const caption =
    type === "scatter"
      ? `散点图使用前两个指标：${labelOf(result.columns, labels, metrics[0])}（X）/ ${labelOf(result.columns, labels, metrics[1])}（Y）`
      : type === "radar"
        ? "雷达图展示前 6 行，各指标按自身量级独立缩放"
        : null;

  return (
    <div className="space-y-2">
      {caption && (
        <p className="text-muted-foreground text-[10px]">{caption}</p>
      )}
      <EChart option={option} className="h-[420px]" />
    </div>
  );
}
