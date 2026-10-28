"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
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
import { usePurchaseOrderList } from "../hooks";
import type { PurchaseOrder } from "../types";

const PAGE_SIZE = 20;
const ALL = "__all__";

const statusVariant: Record<
  PurchaseOrder["status"],
  "success" | "warning" | "info" | "destructive" | "secondary"
> = {
  draft: "secondary",
  pending: "warning",
  approved: "info",
  ordered: "info",
  received: "info",
  completed: "success",
  cancelled: "destructive",
};

const statusAccent: Record<PurchaseOrder["status"], string> = {
  draft: "var(--color-border-strong)",
  pending: "var(--color-warning)",
  approved: "var(--color-info)",
  ordered: "var(--color-info)",
  received: "var(--color-success)",
  completed: "var(--color-success)",
  cancelled: "var(--color-error)",
};

const currency = (value: number) => `¥${value.toLocaleString()}`;

export default function PurchaseOrderList() {
  const router = useRouter();
  const [current, setCurrent] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string>();

  const { data, isLoading } = usePurchaseOrderList({
    current,
    pageSize: PAGE_SIZE,
    keyword: keyword || undefined,
    status,
  });

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="采购协同"
        title="采购管理"
        description="跟踪采购订单、供应商交付与到货进度"
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setKeyword(keywordDraft.trim());
              setCurrent(1);
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
              placeholder="搜索采购单号 / 供应商"
              className="h-8 w-60 text-xs"
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
                <SelectItem value="draft">草稿</SelectItem>
                <SelectItem value="pending">待审批</SelectItem>
                <SelectItem value="approved">已审批</SelectItem>
                <SelectItem value="ordered">已下单</SelectItem>
                <SelectItem value="received">已收货</SelectItem>
                <SelectItem value="completed">已完成</SelectItem>
                <SelectItem value="cancelled">已取消</SelectItem>
              </SelectContent>
            </Select>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 张采购单
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无采购单</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[240px]">采购单</TableHead>
                <TableHead>供应商</TableHead>
                <TableHead numeric>商品数</TableHead>
                <TableHead numeric>总金额</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>
                    <div
                      className="border-l-4 pl-2"
                      style={{ borderLeftColor: statusAccent[order.status] }}
                    >
                      <button
                        type="button"
                        className="text-foreground font-mono text-[12px] font-semibold hover:text-[var(--color-primary-text)] hover:underline"
                        onClick={() => router.push(`/purchase/${order.id}`)}
                      >
                        {order.purchaseNo}
                      </button>
                      <div className="text-muted-foreground mt-0.5 text-[10px]">
                        {order.items?.[0]?.productName ?? "无商品明细"}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>{order.supplierName || "-"}</TableCell>
                  <TableCell numeric>{order.items?.length ?? 0}</TableCell>
                  <TableCell numeric className="font-semibold">
                    {currency(order.totalAmount)}
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge variant={statusVariant[order.status]}>
                      {order.statusLabel}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {order.createdAt}
                  </TableCell>
                  <TableCell className="text-right">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="查看采购单"
                          onClick={() => router.push(`/purchase/${order.id}`)}
                        >
                          <Eye />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>查看详情</TooltipContent>
                    </Tooltip>
                  </TableCell>
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
