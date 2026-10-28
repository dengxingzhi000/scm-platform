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
import { useInboundList } from "../hooks";
import type { InboundOrder } from "../types";

const statusConfig: Record<
  InboundOrder["status"],
  { label: string; variant: "success" | "warning" | "info" | "destructive" }
> = {
  pending: { label: "待收货", variant: "warning" },
  receiving: { label: "收货中", variant: "info" },
  completed: { label: "已完成", variant: "success" },
  cancelled: { label: "已取消", variant: "destructive" },
};

const statusAccent: Record<InboundOrder["status"], string> = {
  pending: "var(--color-warning)",
  receiving: "var(--color-info)",
  completed: "var(--color-success)",
  cancelled: "var(--color-error)",
};

export default function InboundList() {
  const { data: inboundOrders, isLoading } = useInboundList();
  const rows = inboundOrders ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="仓储作业"
        title="入库管理"
        description="跟踪采购到货、收货进度与入库单状态"
      />

      <TablePanel>
        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无入库单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">入库单</TableHead>
                <TableHead>供应商</TableHead>
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
                          {order.inboundNo}
                        </div>
                        <div className="text-muted-foreground mt-0.5 text-[10px]">
                          {order.warehouseName}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{order.supplierName || "-"}</TableCell>
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
          共 {rows.length} 张入库单
        </div>
      </TablePanel>
    </div>
  );
}
