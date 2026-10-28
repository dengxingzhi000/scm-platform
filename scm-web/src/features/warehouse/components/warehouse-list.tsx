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
import { useWarehouseList } from "../hooks";
import type { Warehouse } from "../types";

const statusAccent: Record<Warehouse["status"], string> = {
  active: "var(--color-success)",
  disabled: "var(--color-error)",
};

export default function WarehouseList() {
  const { data: warehouses, isLoading } = useWarehouseList();
  const rows = warehouses ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="履约网络"
        title="仓库管理"
        description="维护仓库主数据、履约地址与联系人"
      />

      <TablePanel>
        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无仓库</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">仓库</TableHead>
                <TableHead>地址</TableHead>
                <TableHead>联系人</TableHead>
                <TableHead>联系电话</TableHead>
                <TableHead className="text-center">状态</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((warehouse) => (
                <TableRow key={warehouse.id}>
                  <TableCell>
                    <div
                      className="border-l-4 pl-2"
                      style={{
                        borderLeftColor: statusAccent[warehouse.status],
                      }}
                    >
                      <div className="font-semibold">{warehouse.name}</div>
                      <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                        {warehouse.code}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[280px] truncate">
                    {warehouse.address || "-"}
                  </TableCell>
                  <TableCell>{warehouse.contactPerson || "-"}</TableCell>
                  <TableCell className="font-mono text-[11px]">
                    {warehouse.contactPhone || "-"}
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge
                      variant={
                        warehouse.status === "active" ? "success" : "destructive"
                      }
                    >
                      {warehouse.status === "active" ? "启用" : "禁用"}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 个仓库
        </div>
      </TablePanel>
    </div>
  );
}
