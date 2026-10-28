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
import { useOrderList } from "../hooks";
import { orderService } from "../services/order.service";
import type { Order, OrderStatus } from "../types";

const PAGE_SIZE = 20;
const ALL = "__all__";

const statusVariant: Record<
  OrderStatus,
  "success" | "warning" | "info" | "destructive" | "secondary"
> = {
  PENDING_PAYMENT: "warning",
  PAID: "info",
  PENDING_SHIP: "info",
  SHIPPED: "info",
  IN_TRANSIT: "info",
  DELIVERED: "info",
  COMPLETED: "success",
  CANCELLED: "secondary",
  REFUNDING: "destructive",
  REFUNDED: "destructive",
};

const statusAccent: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "var(--color-warning)",
  PAID: "var(--color-info)",
  PENDING_SHIP: "var(--color-info)",
  SHIPPED: "var(--color-info)",
  IN_TRANSIT: "var(--color-info)",
  DELIVERED: "var(--color-info)",
  COMPLETED: "var(--color-success)",
  CANCELLED: "var(--color-border-strong)",
  REFUNDING: "var(--color-error)",
  REFUNDED: "var(--color-error)",
};

const currency = (value: number) => `¥${value.toLocaleString()}`;

export default function OrderList() {
  const router = useRouter();
  const [current, setCurrent] = useState(1);
  const [orderNoDraft, setOrderNoDraft] = useState("");
  const [orderNo, setOrderNo] = useState("");
  const [status, setStatus] = useState<OrderStatus>();

  const { data, isLoading } = useOrderList({
    current,
    pageSize: PAGE_SIZE,
    orderNo: orderNo || undefined,
    status,
  });
  const statusFlow = orderService.getStatusFlow();

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="订单履约"
        title="订单管理"
        description="查询订单、支付状态与履约进度"
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setOrderNo(orderNoDraft.trim());
              setCurrent(1);
            }}
            className="flex items-center gap-2"
          >
            {/* The query API matches order_no exactly; there is no fuzzy keyword filter. */}
            <Input
              value={orderNoDraft}
              onChange={(event) => setOrderNoDraft(event.target.value)}
              placeholder="按订单号精确查询"
              className="h-8 w-56 text-xs"
            />
            <Button
              type="submit"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label="查询"
            >
              <Search />
            </Button>
          </form>

          <TableToolbarEnd>
            <Select
              value={status ?? ALL}
              onValueChange={(value) => {
                setStatus(
                  value === ALL ? undefined : (value as OrderStatus),
                );
                setCurrent(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                {Object.entries(statusFlow).map(([key, value]) => (
                  <SelectItem key={key} value={key}>
                    {value.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 张订单
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无订单</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[240px]">订单</TableHead>
                <TableHead numeric>订单金额</TableHead>
                <TableHead numeric>应付金额</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((order: Order) => (
                <TableRow key={order.id}>
                  <TableCell>
                    <div
                      className="border-l-4 pl-2"
                      style={{ borderLeftColor: statusAccent[order.status] }}
                    >
                      <button
                        type="button"
                        className="text-foreground font-mono text-[12px] font-semibold hover:text-[var(--color-primary-text)] hover:underline"
                        onClick={() => router.push(`/order/${order.id}`)}
                      >
                        {order.orderNo}
                      </button>
                      <div className="text-muted-foreground mt-0.5 text-[10px]">
                        下单用户 {order.username || "-"}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell numeric>
                    {currency(order.totalAmount)}
                  </TableCell>
                  <TableCell numeric className="font-semibold">
                    {currency(order.payableAmount)}
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
                          aria-label="查看订单"
                          onClick={() => router.push(`/order/${order.id}`)}
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
