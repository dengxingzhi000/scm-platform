"use client";

import { useMemo, useState, type ComponentType, type ReactNode } from "react";
import { InputNumber, Select, message } from "antd";
import {
  BarChart3,
  ChartColumn,
  ChartNoAxesCombined,
  Download,
  LineChart,
  Play,
  Radar,
  RefreshCw,
  ScatterChart,
  Table2,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { cn } from "@/lib/utils";
import AnalyticsChart, {
  type AnalyticsChartType,
} from "@/features/analytics/components/analytics-chart";
import AnalyticsResultTable, {
  downloadCsv,
  toCsv,
} from "@/features/analytics/components/analytics-result-table";
import AnalyticsSummary from "@/features/analytics/components/analytics-summary";
import {
  useAnalyticsQuery,
  useDatasetFields,
  useDatasets,
} from "@/features/analytics/hooks";

function Segmented<T extends string>({
  value,
  options,
  onChange,
  className,
}: {
  value: T;
  options: Array<{
    value: T;
    label: string;
    icon?: ComponentType<{ className?: string }>;
    disabled?: boolean;
    title?: string;
  }>;
  onChange: (value: T) => void;
  className?: string;
}) {
  return (
    <div
      role="tablist"
      className={cn(
        "border-[var(--color-border-secondary)] bg-[var(--color-brand-blue-light)] inline-flex items-center gap-0.5 rounded-lg border p-0.5",
        className,
      )}
    >
      {options.map((item) => {
        const Icon = item.icon;
        return (
          <button
            key={item.value}
            type="button"
            role="tab"
            aria-selected={value === item.value}
            disabled={item.disabled}
            title={item.title}
            onClick={() => onChange(item.value)}
            className={cn(
              "inline-flex h-7 items-center gap-1 rounded-md px-2 text-[11px] font-medium transition-colors",
              value === item.value
                ? "bg-card text-foreground shadow-xs"
                : "text-muted-foreground hover:text-foreground",
              item.disabled && "cursor-not-allowed opacity-40",
            )}
          >
            {Icon && <Icon className="size-3.5" />}
            {item.label}
          </button>
        );
      })}
    </div>
  );
}

function Field({
  label,
  children,
  className,
}: {
  label: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <div className="text-muted-foreground mb-1.5 text-[10px] font-medium tracking-[0.12em] uppercase">
        {label}
      </div>
      {children}
    </div>
  );
}

export default function AnalyticsPage() {
  const datasetsQuery = useDatasets();
  const [datasetCode, setDatasetCode] = useState<string>();
  const [dimensions, setDimensions] = useState<string[]>([]);
  const [metrics, setMetrics] = useState<string[]>([]);
  const [selectionDatasetId, setSelectionDatasetId] = useState<string>();
  const [limit, setLimit] = useState(1000);
  const [view, setView] = useState<"chart" | "table">("chart");
  const [chartType, setChartType] = useState<AnalyticsChartType>("bar");
  const queryMutation = useAnalyticsQuery();

  const datasets = useMemo(
    () => datasetsQuery.data?.records ?? [],
    [datasetsQuery.data],
  );
  const activeDatasetCode = datasetCode ?? datasets[0]?.datasetCode;
  const selectedDataset = useMemo(
    () =>
      datasets.find((dataset) => dataset.datasetCode === activeDatasetCode),
    [datasets, activeDatasetCode],
  );
  const fieldsQuery = useDatasetFields(selectedDataset?.id);
  const fields = useMemo(() => fieldsQuery.data ?? [], [fieldsQuery.data]);

  const dimensionOptions = useMemo(
    () =>
      fields
        .filter((field) => !field.isHidden && field.fieldType !== "METRIC")
        .map((field) => ({
          label: `${field.fieldName} (${field.fieldCode})`,
          value: field.fieldCode,
        })),
    [fields],
  );
  const metricOptions = useMemo(
    () =>
      fields
        .filter((field) => !field.isHidden && field.fieldType === "METRIC")
        .map((field) => ({
          label: `${field.fieldName} (${field.fieldCode})`,
          value: field.fieldCode,
        })),
    [fields],
  );
  const labels = useMemo(
    () =>
      Object.fromEntries(
        fields.map((field) => [field.fieldCode, field.fieldName]),
      ),
    [fields],
  );

  const hasExplicitSelection = selectionDatasetId === selectedDataset?.id;
  const effectiveDimensions = hasExplicitSelection
    ? dimensions
    : dimensionOptions[0]
      ? [dimensionOptions[0].value]
      : [];
  const effectiveMetrics = hasExplicitSelection
    ? metrics
    : metricOptions[0]
      ? [metricOptions[0].value]
      : [];

  const resolvedChartType: AnalyticsChartType =
    (chartType === "radar" && effectiveMetrics.length < 3) ||
    (chartType === "scatter" && effectiveMetrics.length < 2)
      ? "bar"
      : chartType;

  const handleDatasetChange = (code: string) => {
    setDatasetCode(code);
    setSelectionDatasetId(undefined);
    setDimensions([]);
    setMetrics([]);
    queryMutation.reset();
  };

  const handleDimensionsChange = (values: string[]) => {
    // Keep the auto-selected metrics when the user first touches the
    // dimension picker, otherwise `hasExplicitSelection` would drop them.
    if (!hasExplicitSelection) {
      setMetrics(effectiveMetrics);
    }
    setSelectionDatasetId(selectedDataset?.id);
    setDimensions(values);
  };

  const handleMetricsChange = (values: string[]) => {
    // Mirror of `handleDimensionsChange`: preserve the default dimension.
    if (!hasExplicitSelection) {
      setDimensions(effectiveDimensions);
    }
    setSelectionDatasetId(selectedDataset?.id);
    setMetrics(values);
  };

  const runQuery = () => {
    if (!selectedDataset) {
      message.warning("请先选择数据集");
      return;
    }
    if (effectiveDimensions.length === 0 && effectiveMetrics.length === 0) {
      message.warning("至少选择一个维度或指标");
      return;
    }
    queryMutation.mutate({
      datasetCode: selectedDataset.datasetCode,
      dimensions: effectiveDimensions,
      metrics: effectiveMetrics,
      filters: [],
      orderBy: [],
      limit,
    });
  };

  const refreshMetadata = () => {
    datasetsQuery.refetch();
    fieldsQuery.refetch();
  };

  const error = datasetsQuery.error ?? fieldsQuery.error ?? queryMutation.error;
  const result = queryMutation.data;

  const exportCsv = () => {
    if (!result || result.rows.length === 0) return;
    downloadCsv(
      `${result.datasetCode}-${new Date().toISOString().slice(0, 10)}.csv`,
      toCsv(result, labels),
    );
  };

  const dimensionText = effectiveDimensions.length
    ? effectiveDimensions.map((code) => labels[code] ?? code).join("、")
    : "全部维度";
  const metricText = effectiveMetrics.length
    ? effectiveMetrics.map((code) => labels[code] ?? code).join("、")
    : "全部指标";

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="数据洞察"
        title="数据分析"
        description="探索数据集、维度与指标，生成可分享的分析视图"
      />

      <Card>
        <CardHeader className="pb-0">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <CardTitle>查询设置</CardTitle>
              <CardDescription>
                选择数据集、维度与指标，运行即席查询
              </CardDescription>
            </div>
            <Badge variant="secondary">{datasets.length} 个数据集</Badge>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-12">
            <Field label="数据集" className="xl:col-span-3">
              <Select
                style={{ width: "100%" }}
                placeholder="选择数据集"
                loading={datasetsQuery.isLoading}
                showSearch
                optionFilterProp="label"
                value={activeDatasetCode}
                onChange={handleDatasetChange}
                options={datasets.map((dataset) => ({
                  label: `${dataset.datasetName} (${dataset.datasetCode})`,
                  value: dataset.datasetCode,
                  disabled: !dataset.enabled,
                }))}
              />
            </Field>
            <Field label="维度" className="xl:col-span-3">
              <Select
                mode="multiple"
                style={{ width: "100%" }}
                placeholder="选择维度"
                loading={fieldsQuery.isLoading}
                value={effectiveDimensions}
                onChange={handleDimensionsChange}
                options={dimensionOptions}
                maxTagCount="responsive"
              />
            </Field>
            <Field label="指标" className="xl:col-span-3">
              <Select
                mode="multiple"
                style={{ width: "100%" }}
                placeholder="选择指标"
                loading={fieldsQuery.isLoading}
                value={effectiveMetrics}
                onChange={handleMetricsChange}
                options={metricOptions}
                maxTagCount="responsive"
              />
            </Field>
            <Field label="行数上限" className="xl:col-span-1">
              <InputNumber
                style={{ width: "100%" }}
                min={1}
                max={10000}
                step={100}
                value={limit}
                onChange={(value) => setLimit(value ?? 1000)}
              />
            </Field>
            <div className="flex items-end gap-2 xl:col-span-2">
              <Button
                size="sm"
                disabled={queryMutation.isPending}
                onClick={runQuery}
              >
                <Play />
                运行查询
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="size-8"
                aria-label="刷新元数据"
                onClick={refreshMetadata}
              >
                <RefreshCw />
              </Button>
            </div>
          </div>

          {error && (
            <div
              role="alert"
              className="border-[var(--color-error)]/25 bg-[var(--color-error-soft)] text-[var(--color-error-text)] mt-3 rounded-lg border px-3 py-2 text-[11px]"
            >
              查询失败：{error.message}
            </div>
          )}
        </CardContent>
      </Card>

      {result ? (
        <>
          <AnalyticsSummary
            result={result}
            metrics={effectiveMetrics}
            labels={labels}
          />

          <Card className="gap-0 overflow-hidden py-0">
            <div className="border-[var(--color-border-secondary)] flex flex-wrap items-start justify-between gap-3 border-b px-4 py-3">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    aria-hidden
                    className="bg-primary size-2.5 rounded-[3px]"
                  />
                  <h3 className="text-[13px] font-semibold">分析结果</h3>
                  <Badge variant="secondary">{result.rowCount} 行</Badge>
                  <Badge variant="outline">{result.elapsedMs} ms</Badge>
                </div>
                <p className="text-muted-foreground mt-1 text-[11px]">
                  按 {dimensionText} 分组 · 指标 {metricText} · 数据集{" "}
                  {result.datasetCode}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <Segmented
                  value={view}
                  onChange={setView}
                  options={[
                    { value: "chart", label: "图表", icon: ChartColumn },
                    { value: "table", label: "表格", icon: Table2 },
                  ]}
                />
                {view === "chart" && (
                  <Segmented
                    value={resolvedChartType}
                    onChange={setChartType}
                    options={[
                      {
                        value: "bar",
                        label: "柱状",
                        icon: BarChart3,
                      },
                      {
                        value: "line",
                        label: "折线",
                        icon: LineChart,
                      },
                      {
                        value: "radar",
                        label: "雷达",
                        icon: Radar,
                        disabled: effectiveMetrics.length < 3,
                        title: "雷达图至少需要 3 个指标",
                      },
                      {
                        value: "scatter",
                        label: "散点",
                        icon: ScatterChart,
                        disabled: effectiveMetrics.length < 2,
                        title: "散点图至少需要 2 个指标",
                      },
                    ]}
                  />
                )}
                <Button
                  variant="outline"
                  size="sm"
                  disabled={result.rows.length === 0}
                  onClick={exportCsv}
                >
                  <Download />
                  导出 CSV
                </Button>
              </div>
            </div>

            {view === "chart" ? (
              <div className="p-4">
                <AnalyticsChart
                  result={result}
                  dimensions={effectiveDimensions}
                  metrics={effectiveMetrics}
                  labels={labels}
                  type={resolvedChartType}
                  loading={queryMutation.isPending}
                />
              </div>
            ) : (
              <AnalyticsResultTable
                key={`${result.datasetCode}-${result.rowCount}-${result.elapsedMs}`}
                result={result}
                labels={labels}
                loading={queryMutation.isPending}
              />
            )}
          </Card>
        </>
      ) : (
        <Card className="py-16">
          <div className="text-muted-foreground flex flex-col items-center gap-2 text-center">
            <div className="bg-[var(--color-brand-blue-light)] flex size-12 items-center justify-center rounded-lg">
              <ChartNoAxesCombined className="size-5" />
            </div>
            <p className="text-foreground text-sm font-medium">
              选择数据集与字段后运行查询
            </p>
            <p className="text-xs">图表或表格会显示在这里</p>
          </div>
        </Card>
      )}
    </div>
  );
}
