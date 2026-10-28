"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { Eye, Pencil, Plus, Search, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import {
  useCategoryList,
  useDeleteProduct,
  useProductList,
} from "../hooks";
import type { Product } from "../types";

const PAGE_SIZE = 20;
const ALL = "__all__";

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, Product>();
const emptyRows: Product[] = [];

const statusConfig: Record<
  string,
  { label: string; variant: "success" | "destructive" | "secondary" }
> = {
  on_sale: { label: "在售", variant: "success" },
  off_sale: { label: "下架", variant: "destructive" },
  draft: { label: "草稿", variant: "secondary" },
};

/** Colour stripe on the pinned first column, like the provider marks on a leaderboard. */
const statusAccent: Record<Product["status"], string> = {
  on_sale: "var(--color-success)",
  off_sale: "var(--color-error)",
  draft: "var(--color-border-strong)",
};

const NUMERIC_COLUMNS = new Set(["price", "skuCount"]);

export default function ProductList() {
  const router = useRouter();
  const [current, setCurrent] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [categoryId, setCategoryId] = useState<string>();
  const [status, setStatus] = useState<string>();
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);

  const { data, isLoading } = useProductList({
    current,
    pageSize: PAGE_SIZE,
    keyword: keyword || undefined,
    categoryId,
    status,
  });
  const { data: categories } = useCategoryList();
  const deleteProduct = useDeleteProduct();

  const categoryOptions = useMemo(
    () =>
      categories?.flatMap((category) => [
        { label: category.name, value: category.id },
        ...(category.children ?? []).map((child) => ({
          label: `　${child.name}`,
          value: child.id,
        })),
      ]) ?? [],
    [categories],
  );

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.display({
          id: "product",
          header: "商品",
          cell: ({ row }) => {
            const product = row.original;
            return (
              <div
                className="border-l-4 pl-2"
                style={{ borderLeftColor: statusAccent[product.status] }}
              >
                <button
                  type="button"
                  className="text-foreground block max-w-[320px] truncate text-left text-[12px] font-semibold hover:text-[var(--color-primary-text)] hover:underline"
                  onClick={() => router.push(`/product/${product.id}`)}
                >
                  {product.name}
                </button>
                <span className="text-muted-foreground mt-0.5 block font-mono text-[10px]">
                  {product.code}
                </span>
              </div>
            );
          },
        }),
        columnHelper.accessor("categoryName", {
          header: "分类",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("brandName", {
          header: "品牌",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("price", {
          header: "价格",
          cell: (info) => `¥${info.getValue().toLocaleString()}`,
        }),
        columnHelper.accessor((row) => row.skus?.length ?? 0, {
          id: "skuCount",
          header: "SKU 数",
          cell: (info) => info.getValue(),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const config = statusConfig[info.getValue()];
            return config ? (
              <Badge variant={config.variant}>{config.label}</Badge>
            ) : (
              info.getValue()
            );
          },
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
                    aria-label="查看"
                    onClick={() => router.push(`/product/${row.original.id}`)}
                  >
                    <Eye />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>查看</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="编辑"
                    onClick={() =>
                      router.push(`/product/${row.original.id}/edit`)
                    }
                  >
                    <Pencil />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>编辑</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="删除"
                    className="text-destructive hover:text-destructive"
                    onClick={() => setDeleteTarget(row.original)}
                  >
                    <Trash2 />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>删除</TooltipContent>
              </Tooltip>
            </div>
          ),
        }),
      ]),
    [router],
  );

  const productTable = useTable({
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
        eyebrow="商品中心"
        title="商品管理"
        description="维护 SPU 主数据、价格与上下架状态"
        actions={
          <Button onClick={() => router.push("/product/create")}>
            <Plus />
            新建商品
          </Button>
        }
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
              placeholder="搜索商品名称 / 编码"
              className="h-8 w-56 text-xs"
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
              value={categoryId ?? ALL}
              onValueChange={(value) => {
                setCategoryId(value === ALL ? undefined : value);
                setCurrent(1);
              }}
            >
              <SelectTrigger size="sm" className="w-44 text-xs">
                <span className="text-muted-foreground">分类</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部分类</SelectItem>
                {categoryOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

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
                <SelectItem value="on_sale">在售</SelectItem>
                <SelectItem value="off_sale">下架</SelectItem>
                <SelectItem value="draft">草稿</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : productTable.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无商品</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {productTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={NUMERIC_COLUMNS.has(header.column.id)}
                      className={cn(
                        header.column.id === "product" && "min-w-[220px]",
                        header.column.id === "status" && "text-center",
                        header.column.id === "actions" && "text-right",
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <productTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {productTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={NUMERIC_COLUMNS.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "product" && "min-w-[220px]",
                        cell.column.id === "status" && "text-center",
                        cell.column.id === "actions" && "text-right",
                      )}
                    >
                      <productTable.FlexRender cell={cell} />
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

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除商品</DialogTitle>
            <DialogDescription>
              确定删除「{deleteTarget?.name}」？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteProduct.isPending}
              onClick={() => {
                if (!deleteTarget) return;
                deleteProduct.mutate(deleteTarget.id, {
                  onSettled: () => setDeleteTarget(null),
                });
              }}
            >
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
