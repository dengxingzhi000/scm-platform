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
import { useWaybillList } from "../hooks";
import type { Waybill } from "../types";

const statusConfig: Record<
  Waybill["status"],
  {
    label: string;
    variant: "success" | "warning" | "info" | "destructive" | "secondary";
  }
> = {
  pending: { label: "待揽收", variant: "secondary" },
  picked: { label: "已揽收", variant: "info" },
  in_transit: { label: "运输中", variant: "info" },
  delivered: { label: "已送达", variant: "success" },
  exception: { label: "异常", variant: "destructive" },
};

const statusAccent: Record<Waybill["status"], string> = {
  pending: "var(--color-border-strong)",
  picked: "var(--color-info)",
  in_transit: "var(--color-info)",
  delivered: "var(--color-success)",
  exception: "var(--color-error)",
};

export default function WaybillList() {
  const { data: waybills, isLoading } = useWaybillList();
  const rows = waybills ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="履约网络"
        title="运单管理"
        description="跟踪承运商运单、物流轨迹与签收状态"
      />

      <TablePanel>
        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无运单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">运单</TableHead>
                <TableHead>订单号</TableHead>
                <TableHead>承运商</TableHead>
                <TableHead>收件人</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((waybill) => {
                const config = statusConfig[waybill.status];
                return (
                  <TableRow key={waybill.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{
                          borderLeftColor: statusAccent[waybill.status],
                        }}
                      >
                        <div className="font-mono text-[12px] font-semibold">
                          {waybill.waybillNo}
                        </div>
                        <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                          {waybill.trackingNo}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-[11px]">
                      {waybill.orderNo}
                    </TableCell>
                    <TableCell>{waybill.carrierName || "-"}</TableCell>
                    <TableCell>{waybill.receiverName || "-"}</TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {waybill.createdAt}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 张运单
        </div>
      </TablePanel>
    </div>
  );
}
