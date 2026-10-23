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
  Card,
  CardAction,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
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
        columnHelper.accessor("code", {
          header: "商品编码",
          cell: (info) => (
            <span className="font-mono text-xs">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("name", {
          header: "商品名称",
          cell: (info) => (
            <button
              type="button"
              className="text-primary text-left hover:underline"
              onClick={() => router.push(`/product/${info.row.original.id}`)}
            >
              {info.getValue()}
            </button>
          ),
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
            <div className="flex items-center gap-1">
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
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-xl font-semibold">商品管理</h1>
        <Button onClick={() => router.push("/product/create")}>
          <Plus />
          新建商品
        </Button>
      </div>

      <Card className="gap-4">
        <CardHeader>
          <CardTitle className="text-muted-foreground text-sm font-normal">
            筛选
          </CardTitle>
          <CardAction className="flex flex-wrap items-center gap-2">
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
                placeholder="搜索商品名称/编码"
                className="w-56"
              />
              <Button type="submit" variant="outline" size="icon" aria-label="搜索">
                <Search />
              </Button>
            </form>

            <Select
              value={categoryId ?? ALL}
              onValueChange={(value) => {
                setCategoryId(value === ALL ? undefined : value);
                setCurrent(1);
              }}
            >
              <SelectTrigger className="w-40">
                <SelectValue placeholder="全部分类" />
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
              <SelectTrigger className="w-28">
                <SelectValue placeholder="全部状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                <SelectItem value="on_sale">在售</SelectItem>
                <SelectItem value="off_sale">下架</SelectItem>
                <SelectItem value="draft">草稿</SelectItem>
              </SelectContent>
            </Select>
          </CardAction>
        </CardHeader>
      </Card>

      <Card className="gap-0 overflow-hidden py-0">
        {isLoading ? (
          <div className="p-6">
            <Skeleton className="h-64 w-full" />
          </div>
        ) : productTable.getRowModel().rows.length === 0 ? (
          <div className="text-muted-foreground py-16 text-center text-sm">
            暂无商品
          </div>
        ) : (
          <Table>
            <TableHeader>
              {productTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      className={cn(
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
                    <TableCell key={cell.id}>
                      <productTable.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <div className="flex items-center justify-between border-t px-4 py-3">
          <span className="text-muted-foreground text-sm">共 {total} 条</span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={current <= 1}
              onClick={() => setCurrent((value) => value - 1)}
            >
              上一页
            </Button>
            <span className="text-sm">
              {current} / {pageCount}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={current >= pageCount}
              onClick={() => setCurrent((value) => value + 1)}
            >
              下一页
            </Button>
          </div>
        </div>
      </Card>

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
