"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { Eye, Search } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { useSupplierList } from "../hooks";
import type { Supplier } from "../types";
import RatingStars from "./rating-stars";

const PAGE_SIZE = 20;
const ALL = "__all__";

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, Supplier>();
const emptyRows: Supplier[] = [];

const NUMERIC_COLUMNS = new Set(["rating"]);

const statusAccent: Record<Supplier["status"], string> = {
  active: "var(--color-success)",
  disabled: "var(--color-error)",
};

export default function SupplierList() {
  const router = useRouter();
  const [current, setCurrent] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string>();

  const { data, isLoading } = useSupplierList({
    current,
    pageSize: PAGE_SIZE,
    keyword: keyword || undefined,
    status,
  });

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
                className="border-l-4 pl-2"
                style={{ borderLeftColor: statusAccent[supplier.status] }}
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
        columnHelper.accessor("contactPerson", {
          header: "联系人",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.display({
          id: "contact",
          header: "联系方式",
          cell: ({ row }) => (
            <div className="leading-4">
              <div className="font-mono text-[11px]">
                {row.original.contactPhone || "-"}
              </div>
              <div className="text-muted-foreground mt-0.5 text-[10px]">
                {row.original.email || "-"}
              </div>
            </div>
          ),
        }),
        columnHelper.accessor("address", {
          header: "地址",
          cell: (info) => (
            <span
              className="text-muted-foreground block max-w-[220px] truncate"
              title={info.getValue() || undefined}
            >
              {info.getValue() || "-"}
            </span>
          ),
        }),
        columnHelper.accessor("rating", {
          header: "评级",
          cell: (info) => <RatingStars value={info.getValue()} showValue />,
        }),
        columnHelper.accessor("status", {
          header: "状态",
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
            <div className="flex items-center justify-end">
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
            </div>
          ),
        }),
      ]),
    [router],
  );

  const supplierTable = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  });

  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const applyKeyword = (value: string) => {
    setKeyword(value.trim());
    setCurrent(1);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="供应链协同"
        title="供应商管理"
        description="维护供应商档案、联系人、评级与准入状态"
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              applyKeyword(keywordDraft);
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
              placeholder="搜索供应商名称 / 编码"
              className="h-8 w-64 text-xs"
            />
            <Button
              type="submit"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label="搜索"
            >
              <Search />
            </Button>
          </form>

          <TableToolbarEnd>
            <Select
              value={status ?? ALL}
              onValueChange={(value) => {
                setStatus(value === ALL ? undefined : value);
                setCurrent(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                <SelectItem value="active">启用</SelectItem>
                <SelectItem value="disabled">禁用</SelectItem>
              </SelectContent>
            </Select>
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
                        header.column.id === "status" && "text-center",
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
                        cell.column.id === "status" && "text-center",
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
          page={current}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setCurrent}
        />
      </TablePanel>
    </div>
  );
}
