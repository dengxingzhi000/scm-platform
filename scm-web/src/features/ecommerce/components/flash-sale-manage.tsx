"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery } from "@tanstack/react-query";
import { message } from "antd";
import { Check, RefreshCw, ShoppingCart } from "lucide-react";

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
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { toApiError } from "@/lib/api/envelope";
import { promotionApi } from "@/lib/api/services/ecommerce";
import type { FlashSale } from "@/lib/api/contracts/ecommerce";
import { cn } from "@/lib/utils";
import {
  flashSaleStatusMap,
  formatAmount,
  formatDateTime,
  statusMeta,
} from "../lib/status";

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, FlashSale>();
const emptyRows: FlashSale[] = [];

export default function FlashSaleManage() {
  const [purchaseTarget, setPurchaseTarget] = useState<FlashSale | null>(null);
  const [quantity, setQuantity] = useState("");

  const flashQuery = useQuery({
    queryKey: ["ecommerce", "flash-sales", "active"],
    queryFn: () => promotionApi.flashSales.active(),
  });

  const checkStock = useMutation({
    mutationFn: (row: FlashSale) =>
      promotionApi.flashSales.checkStock(row.id, row.skuId),
    onSuccess: (available) =>
      message.success(
        available
          ? "库存校验通过，库存充足"
          : "库存校验完成，当前库存不足"
      ),
    onError: (error) => message.error(toApiError(error).message),
  });

  const purchase = useMutation({
    mutationFn: () =>
      promotionApi.flashSales.purchase(
        purchaseTarget!.id,
        purchaseTarget!.skuId,
        Number(quantity)
      ),
    onSuccess: () => {
      message.success("秒杀下单成功");
      setPurchaseTarget(null);
      setQuantity("");
      void flashQuery.refetch();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("id", {
          header: "秒杀 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("activityId", {
          header: "活动 ID",
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
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("originalPrice", {
          header: "原价",
          cell: (info) => (
            <span className="text-muted-foreground">
              {formatAmount(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("flashPrice", {
          header: "秒杀价",
          cell: (info) => (
            <span className="font-semibold">{formatAmount(info.getValue())}</span>
          ),
        }),
        columnHelper.accessor("flashStock", {
          header: "秒杀库存",
          cell: (info) => (info.getValue() ?? 0).toLocaleString(),
        }),
        columnHelper.accessor("soldCount", {
          header: "已售",
          cell: (info) => (info.getValue() ?? 0).toLocaleString(),
        }),
        columnHelper.accessor("startTime", {
          header: "开始时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("endTime", {
          header: "结束时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(flashSaleStatusMap, info.getValue());
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
                disabled={checkStock.isPending}
                onClick={() => checkStock.mutate(row.original)}
              >
                <Check />
                校验库存
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => {
                  setPurchaseTarget(row.original);
                  setQuantity("1");
                }}
              >
                <ShoppingCart />
                秒杀下单
              </Button>
            </div>
          ),
        }),
      ]),
    [checkStock]
  );

  const table = useTable({
    features,
    columns,
    data: flashQuery.data ?? emptyRows,
  });

  const rows = table.getRowModel().rows;
  const numericColumns = new Set([
    "id",
    "activityId",
    "productId",
    "skuId",
    "flashStock",
    "soldCount",
  ]);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="限时秒杀"
        description="查看进行中的秒杀活动，校验库存并模拟下单"
        actions={
          <Button
            variant="outline"
            onClick={() => void flashQuery.refetch()}
            disabled={flashQuery.isFetching}
          >
            <RefreshCw />
            刷新
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-xs">
            进行中的秒杀共 {rows.length} 条
          </span>
        </TableToolbar>

        {flashQuery.isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无进行中的秒杀活动</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-260px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={numericColumns.has(header.column.id)}
                      className={cn(
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
      </TablePanel>

      <Dialog
        open={!!purchaseTarget}
        onOpenChange={(open) => !open && setPurchaseTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>秒杀下单</DialogTitle>
            <DialogDescription>
              秒杀 ID {purchaseTarget?.id} · SKU {purchaseTarget?.skuId}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label>购买数量</Label>
            <Input
              type="number"
              min={1}
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              placeholder="请输入购买数量"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPurchaseTarget(null)}>
              取消
            </Button>
            <Button
              disabled={
                purchase.isPending ||
                !quantity.trim() ||
                Number(quantity) < 1
              }
              onClick={() => purchase.mutate()}
            >
              下单
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
