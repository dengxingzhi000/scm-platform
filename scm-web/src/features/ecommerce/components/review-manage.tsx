"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import { Plus, Search, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
  TablePagination,
  TablePanel,
  TableToolbar,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import type { Review } from "@/lib/api/contracts/ecommerce";
import { toApiError } from "@/lib/api/envelope";
import { mallApi } from "@/lib/api/services/ecommerce";
import { cn } from "@/lib/utils";
import { formatDateTime, toNumberOrUndefined } from "../lib/status";

const PAGE_SIZE = 20;
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, Review>();
const emptyRows: Review[] = [];
const RATINGS = [5, 4, 3, 2, 1] as const;

interface ReviewFormState {
  userId: string;
  productId: string;
  skuId: string;
  orderNo: string;
  rating: string;
  content: string;
  isAnonymous: boolean;
}

const emptyForm: ReviewFormState = {
  userId: "",
  productId: "1",
  skuId: "",
  orderNo: "",
  rating: "5",
  content: "",
  isAnonymous: false,
};

export default function ReviewManage() {
  const queryClient = useQueryClient();

  const [productIdDraft, setProductIdDraft] = useState("1");
  const [productId, setProductId] = useState(1);
  const [page, setPage] = useState(1);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<ReviewFormState>(emptyForm);
  const [deleteTarget, setDeleteTarget] = useState<Review | null>(null);

  const listQuery = useQuery({
    queryKey: ["ecommerce", "reviews", productId, page],
    queryFn: () =>
      mallApi.reviews.listByProduct(productId, page, PAGE_SIZE),
    enabled: productId > 0,
  });

  const invalidateReviews = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "reviews"] });

  const createReview = useMutation({
    mutationFn: () => {
      const formProductId = toNumberOrUndefined(form.productId);
      return mallApi.reviews.create({
        userId: form.userId.trim(),
        productId: formProductId!,
        skuId: toNumberOrUndefined(form.skuId),
        orderNo: form.orderNo.trim() || undefined,
        rating: Number(form.rating),
        content: form.content.trim(),
        isAnonymous: form.isAnonymous,
      });
    },
    onSuccess: () => {
      message.success("评价已新增");
      setCreateOpen(false);
      setForm(emptyForm);
      void invalidateReviews();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const removeReview = useMutation({
    mutationFn: (review: Review) =>
      mallApi.reviews.remove(review.id, review.userId),
    onSuccess: () => {
      message.success("评价已删除");
      setDeleteTarget(null);
      void invalidateReviews();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const applyProductId = () => {
    const value = toNumberOrUndefined(productIdDraft);
    if (value === undefined || value <= 0) {
      message.error("请填写有效的商品 ID");
      return;
    }
    setProductId(value);
    setPage(1);
  };

  const openCreate = () => {
    setForm({ ...emptyForm, productId: String(productId) });
    setCreateOpen(true);
  };

  const formProductId = toNumberOrUndefined(form.productId);

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("id", {
          header: "ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("userId", {
          header: "用户 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("productId", {
          header: "商品 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("skuId", {
          header: "SKU ID",
          cell: (info) =>
            info.getValue() === undefined ? (
              <span className="text-muted-foreground">—</span>
            ) : (
              <span className="font-mono text-[11px]">{info.getValue()}</span>
            ),
        }),
        columnHelper.accessor("rating", {
          header: "评分",
          cell: (info) => (
            <span className="text-[var(--color-warning-text)] whitespace-nowrap">
              ★ {info.getValue() ?? 0}
            </span>
          ),
        }),
        columnHelper.accessor("content", {
          header: "评价内容",
          cell: (info) => (
            <span
              className="block max-w-[240px] truncate"
              title={info.getValue() ?? ""}
            >
              {info.getValue() || "—"}
            </span>
          ),
        }),
        columnHelper.accessor("isAnonymous", {
          header: "匿名",
          cell: (info) => (
            <Badge variant={info.getValue() ? "secondary" : "outline"}>
              {info.getValue() ? "是" : "否"}
            </Badge>
          ),
        }),
        columnHelper.accessor("createdAt", {
          header: "创建时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
        }),
        columnHelper.display({
          id: "actions",
          header: "操作",
          cell: ({ row }) => (
            <div className="flex items-center justify-end">
              <Button
                variant="ghost"
                size="sm"
                className="text-destructive h-7 px-2 text-xs"
                onClick={() => setDeleteTarget(row.original)}
              >
                <Trash2 />
                删除
              </Button>
            </div>
          ),
        }),
      ]),
    []
  );

  const table = useTable({
    features,
    columns,
    data: listQuery.data?.records ?? emptyRows,
  });

  const total = listQuery.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const numericColumns = new Set(["id", "productId", "skuId", "rating"]);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="评价管理"
        description="按商品查看用户评价，维护评价内容与匿名状态"
        actions={
          <Button onClick={openCreate}>
            <Plus />
            新增评价
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              applyProductId();
            }}
          >
            <Input
              type="number"
              min={1}
              value={productIdDraft}
              onChange={(event) => setProductIdDraft(event.target.value)}
              placeholder="商品 ID"
              className="h-8 w-40 text-xs"
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
          <span className="text-muted-foreground text-[11px]">
            当前商品 ID：{productId}
          </span>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : listQuery.isError ? (
          <TableEmpty>
            加载失败：{toApiError(listQuery.error).message}
          </TableEmpty>
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无评价数据</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={numericColumns.has(header.column.id)}
                      className={cn(
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
              {table.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={numericColumns.has(cell.column.id)}
                      className={cn(
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

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-h-[86vh] max-w-lg overflow-y-auto">
          <DialogHeader>
            <DialogTitle>新增评价</DialogTitle>
            <DialogDescription>
              为指定商品录入一条用户评价。
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1">
              <Label className="text-xs">用户 ID</Label>
              <Input
                value={form.userId}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, userId: event.target.value }))
                }
                placeholder="必填"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">商品 ID</Label>
              <Input
                type="number"
                min={1}
                value={form.productId}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    productId: event.target.value,
                  }))
                }
                placeholder="必填"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">SKU ID</Label>
              <Input
                type="number"
                min={1}
                value={form.skuId}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, skuId: event.target.value }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">订单号</Label>
              <Input
                value={form.orderNo}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, orderNo: event.target.value }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">评分</Label>
              <Select
                value={form.rating}
                onValueChange={(value) =>
                  setForm((prev) => ({ ...prev, rating: value }))
                }
              >
                <SelectTrigger size="sm" className="w-full text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RATINGS.map((rating) => (
                    <SelectItem key={rating} value={String(rating)}>
                      {rating} 星
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1 sm:col-span-2">
              <Label className="text-xs">评价内容</Label>
              <Textarea
                rows={4}
                value={form.content}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, content: event.target.value }))
                }
                placeholder="必填"
              />
            </div>
          </div>

          <label className="flex items-center gap-2 text-xs">
            <Checkbox
              checked={form.isAnonymous}
              onCheckedChange={(checked) =>
                setForm((prev) => ({
                  ...prev,
                  isAnonymous: checked === true,
                }))
              }
            />
            匿名评价
          </label>

          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                createReview.isPending ||
                !form.userId.trim() ||
                formProductId === undefined ||
                !form.content.trim()
              }
              onClick={() => createReview.mutate()}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="删除评价"
        description={`确认删除评价 #${deleteTarget?.id ?? ""}？该操作不可撤销。`}
        confirmText="删除"
        destructive
        pending={removeReview.isPending}
        onConfirm={() => deleteTarget && removeReview.mutate(deleteTarget)}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      />
    </div>
  );
}
