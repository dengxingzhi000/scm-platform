"use client";

import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { Eye, Pencil } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TablePagination,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import { useSupplierCategories, useSupplierList } from "../hooks";
import {
  AUDIT_STATUSES,
  COOPERATION_STATUSES,
  RISK_LEVELS,
  SUPPLIER_LEVELS,
  labelOf,
  variantOf,
} from "../lib/constants";
import type { Supplier, SupplierSearchParams } from "../types";
import RatingStars from "./rating-stars";
import SupplierActions from "./supplier-actions";
import SupplierFilters from "./supplier-filters";
import SupplierStats from "./supplier-stats";

const PAGE_SIZE = 20;

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, Supplier>();
const emptyRows: Supplier[] = [];

const NUMERIC_COLUMNS = new Set(["score"]);

/** 供应商名左侧的色条 —— 按合作状态着色。 */
const cooperationAccent: Record<number, string> = {
  0: "var(--color-border-strong)",
  1: "var(--color-primary)",
  2: "var(--color-warning)",
  3: "var(--color-error)",
};

/** query 里的数字参数 —— 空串或非法值都当作没传。 */
const numberParam = (value: string | null): number | undefined => {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

/** query 里的布尔参数。 */
const booleanParam = (value: string | null): boolean | undefined => {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
};

export default function SupplierList() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // 初始筛选来自 URL —— 分享链接能带着筛选条件打开。
  const [filters, setFilters] = useState<SupplierSearchParams>(() => ({
    current: Math.max(1, numberParam(searchParams.get("page")) ?? 1),
    keyword: searchParams.get("keyword") ?? undefined,
    status: searchParams.get("status") ?? undefined,
    supplierType: numberParam(searchParams.get("supplierType")),
    cooperationStatus: numberParam(searchParams.get("cooperationStatus")),
    auditStatus: numberParam(searchParams.get("auditStatus")),
    supplierLevel: numberParam(searchParams.get("supplierLevel")),
    riskLevel: numberParam(searchParams.get("riskLevel")),
    blacklisted: booleanParam(searchParams.get("blacklisted")),
    categoryId: searchParams.get("categoryId") ?? undefined,
  }));

  const { data, isLoading, refetch } = useSupplierList({
    ...filters,
    pageSize: PAGE_SIZE,
  });
  const { data: categories } = useSupplierCategories();

  /**
   * 把筛选写回 query。用 `replace` 而不是 `push` —— 筛选是连续的调整动作，
   * 不该在历史栈里留下几十条记录。href 只带 query，路径由浏览器相对解析，
   * 因此不用再引一次 `usePathname`。
   */
  const syncUrl = (next: SupplierSearchParams) => {
    const params = new URLSearchParams();
    if (next.keyword) params.set("keyword", next.keyword);
    if (next.status) params.set("status", next.status);
    if (next.supplierType !== undefined)
      params.set("supplierType", String(next.supplierType));
    if (next.cooperationStatus !== undefined)
      params.set("cooperationStatus", String(next.cooperationStatus));
    if (next.auditStatus !== undefined)
      params.set("auditStatus", String(next.auditStatus));
    if (next.supplierLevel !== undefined)
      params.set("supplierLevel", String(next.supplierLevel));
    if (next.riskLevel !== undefined)
      params.set("riskLevel", String(next.riskLevel));
    if (next.blacklisted !== undefined)
      params.set("blacklisted", String(next.blacklisted));
    if (next.categoryId) params.set("categoryId", next.categoryId);
    params.set("page", String(next.current ?? 1));
    router.replace(`?${params.toString()}`);
  };

  /**
   * 应用筛选条件。空 patch 表示「清空筛选」——供应商总数卡片用的就是这个。
   * 筛选一变，页码回到第一页。
   */
  const applyFilters = (patch: Partial<SupplierSearchParams>) => {
    const next: SupplierSearchParams = {
      ...(Object.keys(patch).length === 0 ? {} : filters),
      ...patch,
      current: 1,
    };
    setFilters(next);
    syncUrl(next);
  };

  const changePage = (page: number) => {
    const next = { ...filters, current: page };
    setFilters(next);
    syncUrl(next);
  };

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.display({
          id: "supplier",
          header: "供应商",
          cell: ({ row }) => {
            const supplier = row.original;
            return (
              <div
                className="border-l-[3px] pl-2"
                style={{
                  borderLeftColor:
                    cooperationAccent[supplier.cooperationStatus ?? 0],
                }}
              >
                <button
                  type="button"
                  className="text-foreground block max-w-[280px] truncate text-left text-[12px] font-semibold hover:text-[var(--color-primary-text)] hover:underline"
                  onClick={() => router.push(`/supplier/${supplier.id}`)}
                >
                  {supplier.name}
                </button>
                <span className="text-muted-foreground mt-0.5 block font-mono text-[10px]">
                  {supplier.code}
                </span>
              </div>
            );
          },
        }),
        columnHelper.accessor("supplierLevel", {
          header: "分级",
          cell: (info) => (
            <Badge variant={variantOf(SUPPLIER_LEVELS, info.getValue())}>
              {labelOf(SUPPLIER_LEVELS, info.getValue())}
            </Badge>
          ),
        }),
        columnHelper.accessor("auditStatus", {
          header: "准入状态",
          cell: (info) => (
            <Badge variant={variantOf(AUDIT_STATUSES, info.getValue())}>
              {labelOf(AUDIT_STATUSES, info.getValue())}
            </Badge>
          ),
        }),
        columnHelper.accessor("riskLevel", {
          header: "风险等级",
          cell: (info) => (
            <Badge variant={variantOf(RISK_LEVELS, info.getValue())}>
              {labelOf(RISK_LEVELS, info.getValue())}
            </Badge>
          ),
        }),
        columnHelper.display({
          id: "score",
          header: "综合得分",
          cell: ({ row }) => (
            <div className="flex items-center justify-center gap-2">
              <span className="font-semibold tabular-nums">
                {row.original.overallScore ?? "-"}
              </span>
              <RatingStars value={row.original.rating} showValue />
            </div>
          ),
        }),
        columnHelper.display({
          id: "contact",
          header: "联系人",
          cell: ({ row }) => (
            <div className="leading-4">
              <div className="text-[12px]">{row.original.contactPerson || "-"}</div>
              <div className="text-muted-foreground font-mono text-[10px]">
                {row.original.contactPhone || "-"}
              </div>
            </div>
          ),
        }),
        columnHelper.accessor("cooperationStatus", {
          header: "合作状态",
          cell: (info) => (
            <Badge variant={variantOf(COOPERATION_STATUSES, info.getValue())}>
              {labelOf(COOPERATION_STATUSES, info.getValue())}
            </Badge>
          ),
        }),
        columnHelper.accessor("status", {
          header: "启用状态",
          cell: (info) =>
            info.getValue() === "active" ? (
              <Badge variant="success">启用</Badge>
            ) : (
              <Badge variant="destructive">禁用</Badge>
            ),
        }),
        columnHelper.display({
          id: "actions",
          header: "操作",
          cell: ({ row }) => (
            <div className="flex items-center justify-end gap-1">
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="查看供应商"
                    onClick={() => router.push(`/supplier/${row.original.id}`)}
                  >
                    <Eye />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>查看详情</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="编辑供应商"
                    onClick={() =>
                      router.push(`/supplier/${row.original.id}/edit`)
                    }
                  >
                    <Pencil />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>编辑</TooltipContent>
              </Tooltip>
              <SupplierActions
                supplier={row.original}
                onDone={() => {
                  void refetch();
                }}
              />
            </div>
          ),
        }),
      ]),
    [router, refetch],
  );

  const supplierTable = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  });

  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="供应链协同"
        title="供应商管理"
        description="供应商主数据、准入状态、风险等级、证照与绩效的一体化管理台"
        actions={
          <Button onClick={() => router.push("/supplier/new")}>新建供应商</Button>
        }
      />

      <SupplierStats onFilter={applyFilters} />

      <TablePanel>
        <TableToolbar>
          <SupplierFilters
            value={filters}
            categories={categories ?? []}
            onChange={applyFilters}
          />

          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 家供应商
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : supplierTable.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无供应商</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {supplierTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={NUMERIC_COLUMNS.has(header.column.id)}
                      className={cn(
                        header.column.id === "supplier" && "min-w-[220px]",
                        header.column.id === "actions" && "text-right",
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <supplierTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {supplierTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={NUMERIC_COLUMNS.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "supplier" && "min-w-[220px]",
                        cell.column.id === "actions" && "text-right",
                      )}
                    >
                      <supplierTable.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={filters.current ?? 1}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={changePage}
        />
      </TablePanel>
    </div>
  );
}
