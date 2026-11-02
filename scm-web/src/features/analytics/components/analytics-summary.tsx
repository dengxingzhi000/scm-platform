"use client";

import { useMemo } from "react";

import { Card } from "@/components/ui/card";
import { useThemeTokens } from "@/hooks/use-theme-tokens";
import { cn } from "@/lib/utils";
import type { DatasetQueryResult } from "../types";

interface AnalyticsSummaryProps {
  result: DatasetQueryResult;
  metrics: string[];
  labels: Record<string, string>;
}

const NUMERIC_TYPES = new Set(["INT", "BIGINT", "DECIMAL", "DOUBLE"]);

const NUMBER_FORMAT = new Intl.NumberFormat("zh-CN", {
  maximumFractionDigits: 2,
});

export default function AnalyticsSummary({
  result,
  metrics,
  labels,
}: AnalyticsSummaryProps) {
  const tokens = useThemeTokens();

  const cards = useMemo(() => {
    const metricCards = metrics.slice(0, 4).map((metric) => {
      const column = result.columns.find((item) => item.code === metric);
      const numeric = column ? NUMERIC_TYPES.has(column.dataType) : false;
      const values = result.rows
        .map((row) => Number(row[metric]))
        .filter((value) => Number.isFinite(value));

      const sum = values.reduce((total, value) => total + value, 0);
      const average = values.length > 0 ? sum / values.length : 0;
      // Rates, averages and unit prices are not additive; show their mean
      // instead of a meaningless total.
      const additive = !/(avg|rate|ratio|price|cost|率|单价|均价)/i.test(
        metric,
      );
      const headline = additive ? sum : average;

      return {
        key: metric,
        label: labels[metric] ?? metric,
        value: numeric ? NUMBER_FORMAT.format(headline) : "-",
        caption: numeric
          ? additive
            ? `均值 ${NUMBER_FORMAT.format(average)} · ${values.length} 行`
            : `样本 ${values.length} 行`
          : `${values.length} 行`,
        tag: additive ? "合计" : "均值",
        numeric: true,
      };
    });

    const contextCards = [
      {
        key: "__rows",
        label: "结果行数",
        value: result.rowCount.toLocaleString("zh-CN"),
        caption: "查询返回明细",
        tag: "汇总",
        numeric: true,
      },
      {
        key: "__elapsed",
        label: "查询耗时",
        value: `${result.elapsedMs} ms`,
        caption: "服务端执行",
        tag: "汇总",
        numeric: true,
      },
      {
        key: "__dataset",
        label: "数据集",
        value: result.datasetCode,
        caption: "当前分析数据集",
        tag: "当前",
        numeric: false,
      },
    ];

    return [...metricCards, ...contextCards].slice(0, 4);
  }, [labels, metrics, result]);

  if (cards.length === 0) {
    return null;
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {cards.map((card, index) => (
        <Card key={card.key} className="gap-2 py-4">
          <div className="flex items-center justify-between gap-2 px-4">
            <div className="flex min-w-0 items-center gap-2">
              <span
                aria-hidden
                className="size-2 shrink-0 rounded-[2px]"
                style={{
                  background:
                    tokens.chart[index % tokens.chart.length] ??
                    tokens.primary,
                }}
              />
              <span className="truncate text-[11px] font-medium">
                {card.label}
              </span>
            </div>
            <span className="text-muted-foreground shrink-0 text-[10px]">
              {card.tag}
            </span>
          </div>
          <div
            className={cn(
              "truncate px-4 font-semibold tracking-tight tabular-nums",
              card.numeric ? "text-[24px] leading-none" : "font-mono text-[14px]",
            )}
          >
            {card.value}
          </div>
          <div className="text-muted-foreground px-4 text-[11px]">
            {card.caption}
          </div>
        </Card>
      ))}
    </div>
  );
}
