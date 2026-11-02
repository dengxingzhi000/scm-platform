"use client";

import { useMemo } from "react";
import dayjs from "dayjs";

import EChart from "@/components/charts/echart";
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
import { useThemeTokens } from "@/hooks/use-theme-tokens";
import type { SalesTrendData, SalesTrendPoint } from "../types";

interface SalesChartProps {
  data: SalesTrendData;
  rangeLabel: string;
  loading?: boolean;
  onPointClick?: (point: SalesTrendPoint) => void;
}

const compactMoney = (value: number): string => {
  if (value >= 10000) {
    const wan = value / 10000;
    return `${wan.toFixed(wan >= 100 ? 0 : 1)}万`;
  }
  return Math.round(value).toLocaleString("zh-CN");
};

export default function SalesChart({
  data,
  rangeLabel,
  loading,
  onPointClick,
}: SalesChartProps) {
  const tokens = useThemeTokens();
  const isHourly = data.granularity === "HOUR";

  const labels = useMemo(
    () =>
      data.points.map((point) =>
        dayjs(point.startTime).format(isHourly ? "HH:00" : "MM-DD")
      ),
    [data.points, isHourly]
  );

  const events = useMemo(
    () => ({
      click: (params: unknown) => {
        const index = (params as { dataIndex?: number }).dataIndex;
        if (typeof index !== "number") return;
        const point = data.points[index];
        if (point && onPointClick) onPointClick(point);
      },
    }),
    [data.points, onPointClick]
  );

  const option = useMemo(() => {
    const revenueColor = tokens.chart[0];
    const orderColor = tokens.chart[1];

    return {
      tooltip: {
        trigger: "axis" as const,
        backgroundColor: tokens.bgElevated,
        borderColor: tokens.borderSecondary,
        textStyle: { color: tokens.text, fontSize: 12 },
        axisPointer: {
          type: "line" as const,
          lineStyle: { color: tokens.borderSecondary, type: "dashed" as const },
        },
        formatter: (params: unknown) => {
          const items = params as Array<{
            axisValueLabel?: string;
            marker?: string;
            seriesName?: string;
            value?: number;
          }>;
          if (!Array.isArray(items) || items.length === 0) return "";
          const lines = [
            `<div style="margin-bottom:4px;font-weight:600">${items[0].axisValueLabel ?? ""}</div>`,
          ];
          for (const item of items) {
            const value = Number(item.value ?? 0);
            const formatted =
              item.seriesName === "销售额"
                ? `¥${value.toLocaleString("zh-CN")}`
                : `${value} 单`;
            lines.push(
              `<div style="display:flex;justify-content:space-between;gap:16px">` +
                `<span>${item.marker ?? ""}${item.seriesName ?? ""}</span>` +
                `<span style="font-variant-numeric:tabular-nums">${formatted}</span>` +
                `</div>`
            );
          }
          return lines.join("");
        },
      },
      legend: {
        data: ["销售额", "订单量"],
        bottom: 0,
        icon: "circle",
        itemWidth: 8,
        itemHeight: 8,
        itemGap: 16,
        textStyle: { color: tokens.textSecondary, fontSize: 11 },
      },
      grid: { left: 4, right: 4, top: 28, bottom: 36, containLabel: true },
      xAxis: {
        type: "category" as const,
        data: labels,
        axisLine: { lineStyle: { color: tokens.borderSecondary } },
        axisTick: { show: false },
        axisLabel: { color: tokens.textSecondary, fontSize: 11 },
      },
      yAxis: [
        {
          type: "value" as const,
          name: "营收 (¥)",
          nameTextStyle: { color: tokens.textTertiary, fontSize: 10 },
          axisLabel: {
            color: tokens.textSecondary,
            fontSize: 11,
            formatter: compactMoney,
          },
          splitLine: {
            lineStyle: { color: tokens.borderSecondary, type: "dashed" as const },
          },
        },
        {
          type: "value" as const,
          name: "订单 (单)",
          nameTextStyle: { color: tokens.textTertiary, fontSize: 10 },
          minInterval: 1,
          axisLabel: { color: tokens.textSecondary, fontSize: 11 },
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: "销售额",
          type: "line",
          yAxisIndex: 0,
          smooth: true,
          showSymbol: false,
          symbolSize: 5,
          data: data.points.map((point) => point.revenue),
          itemStyle: { color: revenueColor },
          lineStyle: { width: 2, color: revenueColor },
          emphasis: { focus: "series" as const },
          cursor: "pointer",
          areaStyle: {
            color: {
              type: "linear" as const,
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: `${revenueColor}38` },
                { offset: 1, color: `${revenueColor}00` },
              ],
            },
          },
        },
        {
          name: "订单量",
          type: "bar",
          yAxisIndex: 1,
          data: data.points.map((point) => point.orders),
          barMaxWidth: 14,
          cursor: "pointer",
          itemStyle: {
            color: `${orderColor}66`,
            borderRadius: [3, 3, 0, 0],
          },
          emphasis: {
            focus: "series" as const,
            itemStyle: { color: `${orderColor}cc` },
          },
        },
      ],
    };
  }, [data.points, labels, tokens]);

  const hasData =
    data.points.length > 0 &&
    data.points.some((point) => point.revenue > 0 || point.orders > 0);

  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>销售趋势</CardTitle>
        <CardDescription>
          {rangeLabel}
          {isHourly ? "分时" : "每日"}营收与订单量；营收仅统计已付款及之后状态，点击数据点查看订单
        </CardDescription>
        <CardAction>
          <Badge variant="secondary">{rangeLabel}</Badge>
        </CardAction>
      </CardHeader>
      <CardContent>
        {loading ? (
          <Skeleton className="h-[300px] w-full" />
        ) : hasData ? (
          <div
            role="img"
            aria-label={`${rangeLabel}销售额与订单量趋势图`}
          >
            <EChart
              option={option}
              className="h-[300px]"
              onEvents={onPointClick ? events : undefined}
            />
          </div>
        ) : (
          <div className="text-muted-foreground flex h-[300px] items-center justify-center text-[13px]">
            暂无趋势数据
          </div>
        )}
      </CardContent>
    </Card>
  );
}
