"use client";

import { useEffect, useRef } from "react";

import { echarts, type EChartsCoreOption } from "@/lib/charts/echarts-core";
import { cn } from "@/lib/utils";

interface EChartClientProps {
  option: EChartsCoreOption;
  className?: string;
  style?: React.CSSProperties;
  notMerge?: boolean;
}

/**
 * Imperative ECharts renderer. Always loaded through `@/components/charts/echart`
 * (dynamic + `ssr: false`) so the chart engine stays out of the server bundle.
 */
export default function EChartClient({
  option,
  className,
  style,
  notMerge = true,
}: EChartClientProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<ReturnType<typeof echarts.init> | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const chart = echarts.init(container);
    chartRef.current = chart;

    const observer = new ResizeObserver(() => chart.resize());
    observer.observe(container);

    return () => {
      observer.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    chartRef.current?.setOption(option, { notMerge });
  }, [option, notMerge]);

  return (
    <div
      ref={containerRef}
      className={cn("h-80 w-full", className)}
      style={style}
    />
  );
}
