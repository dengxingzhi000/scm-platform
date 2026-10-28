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
import { useInvoiceList } from "../hooks";
import type { Invoice } from "../types";

const statusConfig: Record<
  Invoice["status"],
  { label: string; variant: "success" | "warning" | "info" }
> = {
  pending: { label: "待开票", variant: "warning" },
  issued: { label: "已开票", variant: "info" },
  sent: { label: "已寄送", variant: "info" },
  received: { label: "已收票", variant: "success" },
};

const statusAccent: Record<Invoice["status"], string> = {
  pending: "var(--color-warning)",
  issued: "var(--color-info)",
  sent: "var(--color-info)",
  received: "var(--color-success)",
};

const currency = (value: number) => `¥${value.toLocaleString()}`;

export default function InvoiceList() {
  const { data: invoices, isLoading } = useInvoiceList();
  const rows = invoices ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="财务管理"
        title="发票管理"
        description="跟踪结算单开票、寄送与接收状态"
      />

      <TablePanel>
        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无发票</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">发票</TableHead>
                <TableHead numeric>金额</TableHead>
                <TableHead numeric>税额</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((invoice) => {
                const config = statusConfig[invoice.status];
                return (
                  <TableRow key={invoice.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{
                          borderLeftColor: statusAccent[invoice.status],
                        }}
                      >
                        <div className="font-mono text-[12px] font-semibold">
                          {invoice.invoiceNo}
                        </div>
                        <div className="text-muted-foreground mt-0.5 text-[10px]">
                          结算单 {invoice.settlementNo}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell numeric className="font-semibold">
                      {currency(invoice.amount)}
                    </TableCell>
                    <TableCell numeric className="text-muted-foreground">
                      {currency(invoice.taxAmount)}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {invoice.createdAt}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 张发票
        </div>
      </TablePanel>
    </div>
  );
}
