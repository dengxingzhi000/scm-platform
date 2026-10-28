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
import { useSettlementList } from "../hooks";
import type { Settlement } from "../types";

const statusConfig: Record<
  Settlement["status"],
  { label: string; variant: "success" | "warning" | "info" | "destructive" }
> = {
  pending: { label: "待结算", variant: "warning" },
  processing: { label: "处理中", variant: "info" },
  completed: { label: "已完成", variant: "success" },
  rejected: { label: "已驳回", variant: "destructive" },
};

const statusAccent: Record<Settlement["status"], string> = {
  pending: "var(--color-warning)",
  processing: "var(--color-info)",
  completed: "var(--color-success)",
  rejected: "var(--color-error)",
};

const currency = (value: number) => `¥${value.toLocaleString()}`;

export default function SettlementList() {
  const { data: settlements, isLoading } = useSettlementList();
  const rows = settlements ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="财务管理"
        title="结算管理"
        description="管理供应商结算单、账期与付款进度"
      />

      <TablePanel>
        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无结算单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">结算单</TableHead>
                <TableHead>供应商</TableHead>
                <TableHead numeric>金额</TableHead>
                <TableHead numeric>账期</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((settlement) => {
                const config = statusConfig[settlement.status];
                return (
                  <TableRow key={settlement.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{
                          borderLeftColor: statusAccent[settlement.status],
                        }}
                      >
                        <div className="font-mono text-[12px] font-semibold">
                          {settlement.settlementNo}
                        </div>
                        <div className="text-muted-foreground mt-0.5 text-[10px]">
                          {settlement.supplierName}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{settlement.supplierName || "-"}</TableCell>
                    <TableCell numeric className="font-semibold">
                      {currency(settlement.amount)}
                    </TableCell>
                    <TableCell numeric>{settlement.period || "-"}</TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {settlement.createdAt}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 张结算单
        </div>
      </TablePanel>
    </div>
  );
}
