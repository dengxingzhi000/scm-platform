"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import { ImageOff, Plus, RefreshCw, Search, Sparkles, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import type { ProductDocument } from "@/lib/api/contracts/ecommerce";
import { toApiError } from "@/lib/api/envelope";
import { searchApi } from "@/lib/api/services/ecommerce";
import { cn } from "@/lib/utils";
import { formatAmount, toNumberOrUndefined } from "../lib/status";

const PAGE_SIZE = 20;
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, ProductDocument>();
const emptyRows: ProductDocument[] = [];

const DEFAULT_INDEX_JSON = '{"productId":1,"title":"..."}';

interface ProductFilters {
  keyword: string;
  categoryId?: number;
  brandId?: number;
}

interface RecommendationParams {
  userId: number;
  limit: number;
}

function productStatusMeta(status?: string) {
  if (!status) return { label: "—", tone: "secondary" as const };
  const normalized = status.toUpperCase();
  if (
    normalized === "1" ||
    normalized === "ON_SALE" ||
    normalized === "ONLINE" ||
    normalized === "ACTIVE"
  ) {
    return { label: status, tone: "success" as const };
  }
  return { label: status, tone: "secondary" as const };
}

export default function SearchConsole() {
  const queryClient = useQueryClient();

  const [keywordDraft, setKeywordDraft] = useState("");
  const [categoryDraft, setCategoryDraft] = useState("");
  const [brandDraft, setBrandDraft] = useState("");
  const [filters, setFilters] = useState<ProductFilters>({ keyword: "" });
  const [page, setPage] = useState(1);

  const [reindexOpen, setReindexOpen] = useState(false);
  const [indexOpen, setIndexOpen] = useState(false);
  const [indexJson, setIndexJson] = useState(DEFAULT_INDEX_JSON);
  const [similarTarget, setSimilarTarget] = useState<ProductDocument | null>(
    null
  );

  const [recUserId, setRecUserId] = useState("1");
  const [recLimit, setRecLimit] = useState("10");
  const [recParams, setRecParams] = useState<RecommendationParams | null>(null);

  const productsQuery = useQuery({
    queryKey: ["ecommerce", "search", "products", filters, page],
    queryFn: () =>
      searchApi.products({ ...filters, page, size: PAGE_SIZE }),
  });

  const similarQuery = useQuery({
    queryKey: [
      "ecommerce",
      "search",
      "similar",
      similarTarget?.productId,
    ],
    queryFn: () => searchApi.similar(similarTarget!.productId),
    enabled: similarTarget !== null,
  });

  const personalizedQuery = useQuery({
    queryKey: ["ecommerce", "search", "personalized", recParams],
    queryFn: () =>
      searchApi.personalized(recParams!.userId, recParams!.limit),
    enabled: recParams !== null,
  });

  const invalidateProducts = () =>
    queryClient.invalidateQueries({
      queryKey: ["ecommerce", "search", "products"],
    });

  const reindex = useMutation({
    mutationFn: () => searchApi.reindexAll(),
    onSuccess: (count) => {
      message.success(`索引重建完成，共 ${count ?? 0} 条商品`);
      setReindexOpen(false);
      void invalidateProducts();
    },
    onError: (error) => {
      setReindexOpen(false);
      message.error(toApiError(error).message);
    },
  });

  const indexProduct = useMutation({
    mutationFn: (document: ProductDocument) =>
      searchApi.indexProduct(document),
    onSuccess: () => {
      message.success("商品已写入索引");
      setIndexOpen(false);
      setIndexJson(DEFAULT_INDEX_JSON);
      void invalidateProducts();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const removeProduct = useMutation({
    mutationFn: (productId: number) => searchApi.removeProduct(productId),
    onSuccess: () => {
      message.success("商品索引已删除");
      void invalidateProducts();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const applyFilters = () => {
    const categoryId = toNumberOrUndefined(categoryDraft);
    const brandId = toNumberOrUndefined(brandDraft);
    if (categoryDraft.trim() && categoryId === undefined) {
      message.error("类目 ID 必须是数字");
      return;
    }
    if (brandDraft.trim() && brandId === undefined) {
      message.error("品牌 ID 必须是数字");
      return;
    }
    setFilters({ keyword: keywordDraft.trim(), categoryId, brandId });
    setPage(1);
  };

  const submitIndex = () => {
    let parsed: Partial<ProductDocument>;
    try {
      parsed = JSON.parse(indexJson) as Partial<ProductDocument>;
    } catch (error) {
      message.error(
        `JSON 解析失败：${error instanceof Error ? error.message : "格式错误"}`
      );
      return;
    }
    if (typeof parsed.productId !== "number" || !parsed.title) {
      message.error("productId（数字）和 title 为必填字段");
      return;
    }
    indexProduct.mutate(parsed as ProductDocument);
  };

  const submitPersonalized = () => {
    const userId = toNumberOrUndefined(recUserId);
    if (userId === undefined) {
      message.error("请填写用户 ID");
      return;
    }
    setRecParams({
      userId,
      limit: toNumberOrUndefined(recLimit) ?? 10,
    });
  };

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.display({
          id: "image",
          header: "图片",
          cell: ({ row }) =>
            row.original.mainImage ? (
              <div className="size-9 overflow-hidden rounded-md border border-[var(--color-border-secondary)]">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={row.original.mainImage}
                  alt={row.original.title}
                  className="size-full object-cover"
                />
              </div>
            ) : (
              <div className="text-muted-foreground flex size-9 items-center justify-center rounded-md border border-dashed border-[var(--color-border-secondary)]">
                <ImageOff className="size-4" aria-hidden />
              </div>
            ),
        }),
        columnHelper.accessor("title", {
          header: "商品名称",
          cell: ({ row, getValue }) => (
            <div className="min-w-[200px] max-w-[320px]">
              <span className="text-foreground block truncate text-[12px] font-semibold">
                {getValue()}
              </span>
              <span className="text-muted-foreground mt-0.5 block font-mono text-[10px]">
                ID {row.original.productId}
                {row.original.brandName ? ` · ${row.original.brandName}` : ""}
              </span>
            </div>
          ),
        }),
        columnHelper.accessor("price", {
          header: "价格",
          cell: (info) => formatAmount(info.getValue()),
        }),
        columnHelper.accessor("salesCount", {
          header: "销量",
          cell: (info) => (info.getValue() ?? 0).toLocaleString(),
        }),
        columnHelper.accessor("rating", {
          header: "评分",
          cell: (info) =>
            info.getValue() === undefined ? "—" : `★ ${info.getValue()}`,
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = productStatusMeta(info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.display({
          id: "actions",
          header: "操作",
          cell: ({ row }) => (
            <div className="flex items-center justify-end gap-1">
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => setSimilarTarget(row.original)}
              >
                <Sparkles />
                相似商品
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-destructive h-7 px-2 text-xs"
                disabled={removeProduct.isPending}
                onClick={() => removeProduct.mutate(row.original.productId)}
              >
                <Trash2 />
                删除索引
              </Button>
            </div>
          ),
        }),
      ]),
    [removeProduct.isPending, removeProduct.mutate]
  );

  const table = useTable({
    features,
    columns,
    data: productsQuery.data ?? emptyRows,
  });

  const rows = table.getRowModel().rows;
  const hasNext = (productsQuery.data?.length ?? 0) === PAGE_SIZE;
  const numericColumns = new Set(["price", "salesCount", "rating"]);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="搜索控制台"
        description="检索商品索引、维护索引文档并查看推荐结果"
        actions={
          <Button variant="outline" onClick={() => setIndexOpen(true)}>
            <Plus />
            索引商品
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex flex-wrap items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              applyFilters();
            }}
          >
            <span className="text-muted-foreground text-[11px]">商品搜索</span>
            <Input
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
              placeholder="关键词"
              className="h-8 w-48 text-xs"
            />
            <Input
              type="number"
              min={1}
              value={categoryDraft}
              onChange={(event) => setCategoryDraft(event.target.value)}
              placeholder="类目 ID"
              className="h-8 w-28 text-xs"
            />
            <Input
              type="number"
              min={1}
              value={brandDraft}
              onChange={(event) => setBrandDraft(event.target.value)}
              placeholder="品牌 ID"
              className="h-8 w-28 text-xs"
            />
            <Button type="submit" variant="outline" size="sm">
              <Search />
              搜索
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={reindex.isPending}
              onClick={() => setReindexOpen(true)}
            >
              <RefreshCw />
              重建索引
            </Button>
          </form>
        </TableToolbar>

        {productsQuery.isLoading ? (
          <TableLoading />
        ) : productsQuery.isError ? (
          <TableEmpty>
            加载失败：{toApiError(productsQuery.error).message}
          </TableEmpty>
        ) : rows.length === 0 ? (
          <TableEmpty>暂无匹配商品</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-360px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={numericColumns.has(header.column.id)}
                      className={cn(
                        header.column.id === "image" && "w-12",
                        header.column.id === "status" && "text-center",
                        header.column.id === "actions" && "text-right"
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <table.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={numericColumns.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "status" && "text-center",
                        cell.column.id === "actions" && "text-right"
                      )}
                    >
                      <table.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <div className="text-muted-foreground flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          <span className="tabular-nums">
            当前第 {page} 页 · 本页 {rows.length} 条
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="h-7 px-2 text-xs"
              disabled={page <= 1 || productsQuery.isFetching}
              onClick={() => setPage((prev) => Math.max(1, prev - 1))}
            >
              上一页
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="h-7 px-2 text-xs"
              disabled={!hasNext || productsQuery.isFetching}
              onClick={() => setPage((prev) => prev + 1)}
            >
              下一页
            </Button>
          </div>
        </div>
      </TablePanel>

      <TablePanel className="p-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-1.5 text-[15px] font-semibold">
              <Sparkles className="size-4" />
              个性化推荐
            </h2>
            <p className="text-muted-foreground mt-1 text-[11px]">
              根据用户 ID 返回推荐商品列表
            </p>
          </div>
          <form
            className="flex flex-wrap items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              submitPersonalized();
            }}
          >
            <Input
              type="number"
              min={1}
              value={recUserId}
              onChange={(event) => setRecUserId(event.target.value)}
              placeholder="用户 ID"
              className="h-8 w-32 text-xs"
            />
            <Input
              type="number"
              min={1}
              value={recLimit}
              onChange={(event) => setRecLimit(event.target.value)}
              placeholder="数量"
              className="h-8 w-24 text-xs"
            />
            <Button type="submit" variant="outline" size="sm">
              获取推荐
            </Button>
          </form>
        </div>

        {recParams === null ? (
          <p className="text-muted-foreground py-6 text-center text-xs">
            填写用户 ID 后获取个性化推荐
          </p>
        ) : personalizedQuery.isLoading ? (
          <p className="text-muted-foreground py-6 text-center text-xs">
            加载中…
          </p>
        ) : personalizedQuery.isError ? (
          <p className="text-muted-foreground py-6 text-center text-xs">
            加载失败：{toApiError(personalizedQuery.error).message}
          </p>
        ) : (personalizedQuery.data?.length ?? 0) === 0 ? (
          <p className="text-muted-foreground py-6 text-center text-xs">
            暂无推荐商品
          </p>
        ) : (
          <div className="mt-3 max-h-64 space-y-1 overflow-y-auto rounded-lg border border-[var(--color-border-secondary)] p-2">
            {personalizedQuery.data!.map((item) => (
              <div
                key={`${item.productId}-${item.id ?? ""}`}
                className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5 text-xs hover:bg-[var(--color-bg-sunken)]"
              >
                <span className="min-w-0 flex-1 truncate">{item.title}</span>
                <span className="font-mono text-[11px]">
                  {formatAmount(item.price)}
                </span>
              </div>
            ))}
          </div>
        )}
      </TablePanel>

      <ConfirmDialog
        open={reindexOpen}
        title="重建搜索索引"
        description="将重新写入全部商品索引，处理期间搜索可能返回旧数据。"
        confirmText="重建"
        pending={reindex.isPending}
        onConfirm={() => reindex.mutate()}
        onOpenChange={setReindexOpen}
      />

      <Dialog open={indexOpen} onOpenChange={setIndexOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>索引商品</DialogTitle>
            <DialogDescription>
              粘贴一个 ProductDocument JSON，提交后写入搜索索引。
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label htmlFor="index-json" className="text-xs">
              文档 JSON
            </Label>
            <Textarea
              id="index-json"
              rows={10}
              value={indexJson}
              spellCheck={false}
              className="font-mono text-[12px]"
              onChange={(event) => setIndexJson(event.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIndexOpen(false)}>
              取消
            </Button>
            <Button
              disabled={indexProduct.isPending || !indexJson.trim()}
              onClick={submitIndex}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={similarTarget !== null}
        onOpenChange={(open) => !open && setSimilarTarget(null)}
      >
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>相似商品</DialogTitle>
            <DialogDescription>
              {similarTarget?.title ?? ""}
            </DialogDescription>
          </DialogHeader>
          {similarQuery.isLoading ? (
            <p className="text-muted-foreground py-6 text-center text-xs">
              加载中…
            </p>
          ) : similarQuery.isError ? (
            <p className="text-muted-foreground py-6 text-center text-xs">
              加载失败：{toApiError(similarQuery.error).message}
            </p>
          ) : (similarQuery.data?.length ?? 0) === 0 ? (
            <p className="text-muted-foreground py-6 text-center text-xs">
              暂无相似商品
            </p>
          ) : (
            <div className="max-h-80 space-y-1 overflow-y-auto rounded-lg border border-[var(--color-border-secondary)] p-2">
              {similarQuery.data!.map((item) => (
                <div
                  key={`${item.productId}-${item.id ?? ""}`}
                  className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5 text-xs hover:bg-[var(--color-bg-sunken)]"
                >
                  <span className="min-w-0 flex-1 truncate">{item.title}</span>
                  <span className="font-mono text-[11px]">
                    {formatAmount(item.price)}
                  </span>
                </div>
              ))}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setSimilarTarget(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
