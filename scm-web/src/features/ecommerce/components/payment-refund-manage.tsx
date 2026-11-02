"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import { Check, Eye, Plus, RefreshCw, Search, XCircle } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
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
import { Textarea } from "@/components/ui/textarea";
import { toApiError } from "@/lib/api/envelope";
import { paymentApi } from "@/lib/api/services/ecommerce";
import type { PaymentRefundDTO } from "@/lib/api/contracts/ecommerce";
import {
  formatAmount,
  formatDateTime,
  refundStatusMap,
  statusMeta,
} from "../lib/status";

const PAGE_SIZE = 20;
const ALL = "__all__";
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, PaymentRefundDTO>();
const emptyRows: PaymentRefundDTO[] = [];

interface RefundFormState {
  paymentNo: string;
  orderNo: string;
  userId: string;
  amount: string;
  reason: string;
}

const emptyRefundForm: RefundFormState = {
  paymentNo: "",
  orderNo: "",
  userId: "",
  amount: "",
  reason: "",
};

export default function PaymentRefundManage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [paymentNoDraft, setPaymentNoDraft] = useState("");
  const [orderNoDraft, setOrderNoDraft] = useState("");
  const [userIdDraft, setUserIdDraft] = useState("");
  const [paymentNo, setPaymentNo] = useState("");
  const [orderNo, setOrderNo] = useState("");
  const [userId, setUserId] = useState("");
  const [status, setStatus] = useState<number | undefined>();

  const [detailNo, setDetailNo] = useState<string | null>(null);
  const [approveTarget, setApproveTarget] = useState<PaymentRefundDTO | null>(
    null
  );
  const [rejectTarget, setRejectTarget] = useState<PaymentRefundDTO | null>(
    null
  );
  const [rejectReason, setRejectReason] = useState("");

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] =
    useState<RefundFormState>(emptyRefundForm);

  const listQuery = useQuery({
    queryKey: [
      "ecommerce",
      "payment-refunds",
      page,
      paymentNo,
      orderNo,
      userId,
      status,
    ],
    queryFn: () =>
      paymentApi.refunds.page({
        page,
        size: PAGE_SIZE,
        paymentNo: paymentNo || undefined,
        orderNo: orderNo || undefined,
        userId: userId || undefined,
        status,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ["ecommerce", "payment-refunds", "detail", detailNo],
    queryFn: () => paymentApi.refunds.get(detailNo!),
    enabled: !!detailNo,
  });

  const invalidateRefunds = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "payment-refunds"] });

  const createRefund = useMutation({
    mutationFn: () =>
      paymentApi.refunds.create({
        paymentNo: createForm.paymentNo.trim(),
        orderNo: createForm.orderNo.trim(),
        userId: createForm.userId.trim(),
        amount: Number(createForm.amount),
        reason: createForm.reason.trim() || undefined,
      }),
    onSuccess: () => {
      message.success("退款单已创建");
      setCreateOpen(false);
      setCreateForm(emptyRefundForm);
      void invalidateRefunds();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const approveRefund = useMutation({
    mutationFn: (refundNo: string) => paymentApi.refunds.approve(refundNo),
    onSuccess: () => {
      message.success("退款已审批通过");
      setApproveTarget(null);
      void invalidateRefunds();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const rejectRefund = useMutation({
    mutationFn: (refundNo: string) =>
      paymentApi.refunds.reject(refundNo, rejectReason.trim()),
    onSuccess: () => {
      message.success("退款已驳回");
      setRejectTarget(null);
      setRejectReason("");
      void invalidateRefunds();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("refundNo", {
          header: "退款单号",
          cell: (info) => (
            <span className="text-foreground font-mono text-[11px] font-semibold">
              {info.getValue()}
            </span>
          ),
        }),
        columnHelper.accessor("paymentNo", {
          header: "支付单号",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("orderNo", {
          header: "订单号",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("userId", {
          header: "用户 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("refundAmount", {
          header: "退款金额",
          cell: (info) => (
            <span className="font-semibold">
              {formatAmount(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("refundReason", {
          header: "退款原因",
          cell: (info) => (
            <span className="text-muted-foreground block max-w-[200px] truncate text-[11px]">
              {info.getValue() || "—"}
            </span>
          ),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(refundStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.accessor("refundedAt", {
          header: "退款时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("createdAt", {
          header: "创建时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
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
                onClick={() => setDetailNo(row.original.refundNo)}
              >
                <Eye />
                详情
              </Button>
              {row.original.status === 1 && (
                <>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => setApproveTarget(row.original)}
                  >
                    <Check />
                    审批通过
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive h-7 px-2 text-xs"
                    onClick={() => {
                      setRejectReason("");
                      setRejectTarget(row.original);
                    }}
                  >
                    <XCircle />
                    驳回
                  </Button>
                </>
              )}
            </div>
          ),
        }),
      ]),
    []
  );

  const table = useTable({
    features,
    columns,
    data: listQuery.data?.records ?? emptyRows,
  });

  const total = listQuery.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const numericColumns = new Set(["refundAmount"]);

  const applyFilters = () => {
    setPaymentNo(paymentNoDraft.trim());
    setOrderNo(orderNoDraft.trim());
    setUserId(userIdDraft.trim());
    setPage(1);
  };

  const amountValue = Number(createForm.amount);
  const canCreate = Boolean(
    createForm.paymentNo.trim() &&
      createForm.orderNo.trim() &&
      createForm.userId.trim() &&
      createForm.amount.trim() &&
      Number.isFinite(amountValue) &&
      amountValue > 0
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="退款管理"
        description="发起退款申请并完成审批与驳回"
        actions={
          <>
            <Button
              variant="outline"
              onClick={() => void listQuery.refetch()}
              disabled={listQuery.isFetching}
            >
              <RefreshCw />
              刷新
            </Button>
            <Button
              onClick={() => {
                setCreateForm(emptyRefundForm);
                setCreateOpen(true);
              }}
            >
              <Plus />
              创建退款
            </Button>
          </>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex flex-wrap items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              applyFilters();
            }}
          >
            <Input
              value={paymentNoDraft}
              onChange={(event) => setPaymentNoDraft(event.target.value)}
              placeholder="支付单号"
              className="h-8 w-44 text-xs"
            />
            <Input
              value={orderNoDraft}
              onChange={(event) => setOrderNoDraft(event.target.value)}
              placeholder="订单号"
              className="h-8 w-44 text-xs"
            />
            <Input
              value={userIdDraft}
              onChange={(event) => setUserIdDraft(event.target.value)}
              placeholder="用户 ID"
              className="h-8 w-44 text-xs"
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
              value={status === undefined ? ALL : String(status)}
              onValueChange={(value) => {
                setStatus(value === ALL ? undefined : Number(value));
                setPage(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                <SelectItem value="1">待审批</SelectItem>
                <SelectItem value="2">退款成功</SelectItem>
                <SelectItem value="3">已驳回</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无退款单数据</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={numericColumns.has(header.column.id)}
                      className={
                        header.column.id === "status"
                          ? "text-center"
                          : header.column.id === "actions"
                            ? "text-right"
                            : undefined
                      }
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
              {table.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={numericColumns.has(cell.column.id)}
                      className={
                        cell.column.id === "status"
                          ? "text-center"
                          : cell.column.id === "actions"
                            ? "text-right"
                            : undefined
                      }
                    >
                      <table.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog
        open={!!detailNo}
        onOpenChange={(open) => !open && setDetailNo(null)}
      >
        <DialogContent className="max-h-[86vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>退款单详情</DialogTitle>
            <DialogDescription>{detailNo}</DialogDescription>
          </DialogHeader>

          {detailQuery.isLoading ? (
            <p className="text-muted-foreground text-xs">加载中…</p>
          ) : detailQuery.data ? (
            <div className="grid gap-3 sm:grid-cols-3">
              <Info label="退款单号" value={detailQuery.data.refundNo} />
              <Info label="支付单号" value={detailQuery.data.paymentNo} />
              <Info label="订单号" value={detailQuery.data.orderNo} />
              <Info label="用户 ID" value={detailQuery.data.userId} />
              <Info
                label="退款金额"
                value={formatAmount(detailQuery.data.refundAmount)}
              />
              <Info
                label="状态"
                value={statusMeta(refundStatusMap, detailQuery.data.status).label}
              />
              <Info
                label="退款时间"
                value={formatDateTime(detailQuery.data.refundedAt)}
              />
              <Info
                label="创建时间"
                value={formatDateTime(detailQuery.data.createdAt)}
              />
              <Info
                label="退款原因"
                value={detailQuery.data.refundReason || "—"}
              />
            </div>
          ) : (
            <p className="text-muted-foreground text-xs">暂无详情数据</p>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setDetailNo(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>创建退款</DialogTitle>
            <DialogDescription>仅已支付的支付单可发起退款</DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="支付单号">
              <Input
                value={createForm.paymentNo}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    paymentNo: event.target.value,
                  }))
                }
                placeholder="必填"
              />
            </Field>
            <Field label="订单号">
              <Input
                value={createForm.orderNo}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    orderNo: event.target.value,
                  }))
                }
                placeholder="必填"
              />
            </Field>
            <Field label="用户 ID">
              <Input
                value={createForm.userId}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    userId: event.target.value,
                  }))
                }
                placeholder="必填"
              />
            </Field>
            <Field label="退款金额">
              <Input
                type="number"
                min={0}
                step="0.01"
                value={createForm.amount}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    amount: event.target.value,
                  }))
                }
                placeholder="必填，大于 0"
              />
            </Field>
            <div className="space-y-1 sm:col-span-2">
              <Label className="text-xs">退款原因</Label>
              <Textarea
                value={createForm.reason}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    reason: event.target.value,
                  }))
                }
                rows={3}
                placeholder="可选"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={createRefund.isPending || !canCreate}
              onClick={() => createRefund.mutate()}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!rejectTarget}
        onOpenChange={(open) => {
          if (!open) {
            setRejectTarget(null);
            setRejectReason("");
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>驳回退款</DialogTitle>
            <DialogDescription>{rejectTarget?.refundNo}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label className="text-xs">驳回原因</Label>
            <Textarea
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              rows={3}
              placeholder="必填"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={rejectRefund.isPending || !rejectReason.trim()}
              onClick={() =>
                rejectTarget && rejectRefund.mutate(rejectTarget.refundNo)
              }
            >
              驳回
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!approveTarget}
        title="审批通过退款"
        description={`确认通过退款单 ${approveTarget?.refundNo ?? ""} 的退款申请吗？`}
        confirmText="审批通过"
        pending={approveRefund.isPending}
        onOpenChange={(open) => !open && setApproveTarget(null)}
        onConfirm={() =>
          approveTarget && approveRefund.mutate(approveTarget.refundNo)
        }
      />
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-[var(--color-border-secondary)] px-3 py-2">
      <p className="text-muted-foreground text-[10px] tracking-[0.08em] uppercase">
        {label}
      </p>
      <p className="mt-0.5 truncate text-[12px] font-medium">{value}</p>
    </div>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <Label className="text-xs">{label}</Label>
      {children}
    </div>
  );
}
