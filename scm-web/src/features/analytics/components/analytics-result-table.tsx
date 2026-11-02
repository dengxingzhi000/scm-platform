"use client";

import { useState } from "react";

import {
  TableEmpty,
  TableLoading,
  TablePagination,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { DatasetQueryResult } from "../types";

const PAGE_SIZE = 20;
const NUMERIC_TYPES = new Set(["INT", "BIGINT", "DECIMAL", "DOUBLE"]);

interface AnalyticsResultTableProps {
  result?: DatasetQueryResult;
  labels?: Record<string, string>;
  loading?: boolean;
  className?: string;
}

function escapeCsv(value: unknown) {
  const text = value == null ? "" : String(value);
  return `"${text.replace(/"/g, '""')}"`;
}

export function toCsv(
  result: DatasetQueryResult,
  labels?: Record<string, string>,
) {
  const header = result.columns
    .map((column) => escapeCsv(labels?.[column.code] ?? column.code))
    .join(",");
  const rows = result.rows.map((row) =>
    result.columns.map((column) => escapeCsv(row[column.code])).join(","),
  );
  return [header, ...rows].join("\n");
}

export function downloadCsv(filename: string, content: string) {
  // BOM keeps Excel from mangling UTF-8 headers.
  const blob = new Blob([`\uFEFF${content}`], {
    type: "text/csv;charset=utf-8;",
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function formatCell(value: unknown, numeric: boolean) {
  if (value == null || value === "") return "-";
  if (numeric && typeof value === "number") {
    return value.toLocaleString("zh-CN", { maximumFractionDigits: 2 });
  }
  return String(value);
}

export default function AnalyticsResultTable({
  result,
  labels,
  loading,
  className,
}: AnalyticsResultTableProps) {
  const [page, setPage] = useState(1);

  if (loading) {
    return <TableLoading />;
  }

  if (!result || result.rows.length === 0) {
    return <TableEmpty>暂无数据</TableEmpty>;
  }

  const columns = result.columns;
  const rows = result.rows;
  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const start = (safePage - 1) * PAGE_SIZE;
  const visibleRows = rows.slice(start, start + PAGE_SIZE);
  const numeric = columns.map((column) =>
    NUMERIC_TYPES.has(column.dataType),
  );

  return (
    <div className={className}>
      <Table containerClassName="max-h-[520px] overflow-y-auto">
        <TableHeader>
          <TableRow>
            {columns.map((column, index) => (
              <TableHead
                key={column.code}
                numeric={numeric[index]}
                className={cn(index === 0 && "min-w-[180px]")}
              >
                {labels?.[column.code] ?? column.code}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {visibleRows.map((row, index) => (
            <TableRow key={`${safePage}-${index}`}>
              {columns.map((column, columnIndex) => (
                <TableCell
                  key={column.code}
                  numeric={numeric[columnIndex]}
                  className={cn(
                    columnIndex === 0 && "min-w-[180px] font-medium",
                  )}
                >
                  {formatCell(row[column.code], numeric[columnIndex])}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <TablePagination
        page={safePage}
        pageSize={PAGE_SIZE}
        total={rows.length}
        pageCount={pageCount}
        onPageChange={setPage}
      />
    </div>
  );
}
