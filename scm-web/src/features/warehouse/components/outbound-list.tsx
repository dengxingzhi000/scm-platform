"use client";

import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useOutboundList } from "../hooks";
import type { OutboundOrder } from "../types";

const statusConfig: Record<
  OutboundOrder["status"],
  { label: string; variant: "success" | "warning" | "info" | "destructive" }
> = {
  pending: { label: "待出库", variant: "warning" },
  picking: { label: "拣货中", variant: "info" },
  shipping: { label: "发运中", variant: "info" },
  completed: { label: "已完成", variant: "success" },
  cancelled: { label: "已取消", variant: "destructive" },
};

const statusAccent: Record<OutboundOrder["status"], string> = {
  pending: "var(--color-warning)",
  picking: "var(--color-info)",
  shipping: "var(--color-info)",
  completed: "var(--color-success)",
  cancelled: "var(--color-error)",
};

export default function OutboundList() {
  const { data: outboundOrders, isLoading } = useOutboundList();
  const rows = outboundOrders ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="仓储作业"
        title="出库管理"
        description="跟踪拣货、发运与出库单完成情况"
      />

      <TablePanel>
        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无出库单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">出库单</TableHead>
                <TableHead>仓库</TableHead>
                <TableHead numeric>商品数</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((order) => {
                const config = statusConfig[order.status];
                return (
                  <TableRow key={order.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{ borderLeftColor: statusAccent[order.status] }}
                      >
                        <div className="font-mono text-[12px] font-semibold">
                          {order.outboundNo}
                        </div>
                        <div className="text-muted-foreground mt-0.5 text-[10px]">
                          {order.items?.[0]?.productName ?? "无商品明细"}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{order.warehouseName || "-"}</TableCell>
                    <TableCell numeric>{order.items?.length ?? 0}</TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {order.createdAt}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 张出库单
        </div>
      </TablePanel>
    </div>
  );
}
