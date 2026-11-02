"use client";

import Link from "next/link";
import { ExternalLink } from "lucide-react";

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
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

import { useRequestDetail } from "../hooks";
import {
  PRIORITIES,
  REQUEST_TYPES,
  formatDateTime,
  formatMoney,
  requestStatusMeta,
} from "../lib/request-meta";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  requestId: string | null;
}

const Field = ({ label, value }: { label: string; value?: string | null }) => (
  <div>
    <div className="text-muted-foreground text-[11px]">{label}</div>
    <div className="mt-0.5 text-[13px]">{value || "-"}</div>
  </div>
);

export function PurchaseRequestDetailDialog({
  open,
  onOpenChange,
  requestId,
}: Props) {
  const { data, isLoading, isError, refetch } = useRequestDetail(
    requestId ?? "",
    open && !!requestId,
  );
  const request = data?.request;
  const items = data?.items ?? [];
  const meta = requestStatusMeta(request?.status);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[88vh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            采购申请
            {request?.requestNo && (
              <span className="text-muted-foreground font-mono text-[13px] font-normal">
                {request.requestNo}
              </span>
            )}
            {request && <Badge variant={meta.tone}>{meta.label}</Badge>}
          </DialogTitle>
          <DialogDescription>
            申请单头与全部明细行；转采购单后可在采购订单中继续跟踪。
          </DialogDescription>
        </DialogHeader>

        {isError ? (
          <div className="py-10 text-center">
            <p className="text-muted-foreground text-[13px]">采购申请加载失败</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={() => refetch()}
            >
              重试
            </Button>
          </div>
        ) : isLoading || !request ? (
          <div className="space-y-3">
            <Skeleton className="h-24 w-full rounded-lg" />
            <Skeleton className="h-40 w-full rounded-lg" />
          </div>
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-x-6 gap-y-3 rounded-lg border border-[var(--color-border-secondary)] p-3 md:grid-cols-4">
              <Field label="申请人" value={request.requesterName} />
              <Field label="申请部门" value={request.deptName} />
              <Field
                label="申请类型"
                value={REQUEST_TYPES[request.requestType ?? 1]}
              />
              <Field label="优先级" value={PRIORITIES[request.priority ?? 2]} />
              <Field
                label="期望到货"
                value={formatDateTime(request.expectedDelivery)}
              />
              <Field label="预算金额" value={formatMoney(request.budgetAmount)} />
              <Field label="创建时间" value={formatDateTime(request.createTime)} />
              <Field
                label="审批人"
                value={
                  request.currentApproverName ||
                  (request.approvedAt ? "已审批" : "-")
                }
              />
              <Field label="采购用途" value={request.purpose} />
              <Field label="备注" value={request.remark} />
              {request.status === 3 && (
                <Field label="驳回原因" value={request.rejectReason} />
              )}
              {request.converted && (
                <Field label="采购单号" value={request.purchaseOrderNo} />
              )}
            </div>

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>物料</TableHead>
                  <TableHead>规格</TableHead>
                  <TableHead numeric>数量</TableHead>
                  <TableHead numeric>预算单价</TableHead>
                  <TableHead numeric>小计</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={5}
                      className="text-muted-foreground py-8 text-center text-xs"
                    >
                      暂无明细
                    </TableCell>
                  </TableRow>
                ) : (
                  items.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        <div className="text-[13px]">
                          {item.materialName || "-"}
                        </div>
                        <div className="text-muted-foreground text-[11px]">
                          {item.materialCode || "-"}
                        </div>
                      </TableCell>
                      <TableCell>{item.materialSpec || "-"}</TableCell>
                      <TableCell numeric>
                        {item.quantity ?? 0} {item.unit ?? ""}
                      </TableCell>
                      <TableCell numeric>
                        {formatMoney(item.budgetPrice)}
                      </TableCell>
                      <TableCell numeric>
                        {formatMoney(
                          (item.quantity ?? 0) * (item.budgetPrice ?? 0),
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
          {request?.converted && request.purchaseOrderId && (
            <Button asChild>
              <Link
                href={`/purchase/${request.purchaseOrderId}`}
                onClick={() => onOpenChange(false)}
              >
                <ExternalLink />
                查看采购单
              </Link>
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
