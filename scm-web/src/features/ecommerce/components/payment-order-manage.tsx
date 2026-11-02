"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import { Eye, Plus, RefreshCw, Search, XCircle } from "lucide-react";

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
import { toApiError } from "@/lib/api/envelope";
import { paymentApi } from "@/lib/api/services/ecommerce";
import type { PaymentOrderDTO } from "@/lib/api/contracts/ecommerce";
import {
  formatAmount,
  formatDateTime,
  paymentStatusMap,
  statusMeta,
} from "../lib/status";

const PAGE_SIZE = 20;
const ALL = "__all__";
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, PaymentOrderDTO>();
const emptyRows: PaymentOrderDTO[] = [];

interface PaymentFormState {
  orderNo: string;
  userId: string;
  amount: string;
  paymentChannel: string;
}

const emptyPaymentForm: PaymentFormState = {
  orderNo: "",
  userId: "",
  amount: "",
  paymentChannel: "",
};

export default function PaymentOrderManage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [orderNoDraft, setOrderNoDraft] = useState("");
  const [userIdDraft, setUserIdDraft] = useState("");
  const [channelDraft, setChannelDraft] = useState("");
  const [orderNo, setOrderNo] = useState("");
  const [userId, setUserId] = useState("");
  const [paymentChannel, setPaymentChannel] = useState("");
  const [status, setStatus] = useState<number | undefined>();

  const [detailNo, setDetailNo] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<PaymentOrderDTO | null>(
    null
  );

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] =
    useState<PaymentFormState>(emptyPaymentForm);

  const listQuery = useQuery({
    queryKey: [
      "ecommerce",
      "payment-orders",
      page,
      orderNo,
      userId,
      paymentChannel,
      status,
    ],
    queryFn: () =>
      paymentApi.page({
        page,
        size: PAGE_SIZE,
        orderNo: orderNo || undefined,
        userId: userId || undefined,
        paymentChannel: paymentChannel || undefined,
        status,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ["ecommerce", "payment-orders", "detail", detailNo],
    queryFn: () => paymentApi.get(detailNo!),
    enabled: !!detailNo,
  });

  const invalidatePayments = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "payment-orders"] });

  const createPayment = useMutation({
    mutationFn: () =>
      paymentApi.create({
        orderNo: createForm.orderNo.trim(),
        userId: createForm.userId.trim(),
        amount: Number(createForm.amount),
        paymentChannel: createForm.paymentChannel.trim(),
      }),
    onSuccess: () => {
      message.success("支付单已创建");
      setCreateOpen(false);
      setCreateForm(emptyPaymentForm);
      void invalidatePayments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const cancelPayment = useMutation({
    mutationFn: (paymentNo: string) => paymentApi.cancel(paymentNo),
    onSuccess: () => {
      message.success("支付单已取消");
      setCancelTarget(null);
      void invalidatePayments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("paymentNo", {
          header: "支付单号",
          cell: (info) => (
            <span className="text-foreground font-mono text-[11px] font-semibold">
              {info.getValue()}
            </span>
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
        columnHelper.accessor("amount", {
          header: "金额",
          cell: (info) => (
            <span className="font-semibold">{formatAmount(info.getValue())}</span>
          ),
        }),
        columnHelper.accessor("paymentChannel", {
          header: "支付渠道",
          cell: (info) => info.getValue() || "—",
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(paymentStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.accessor("paidAt", {
          header: "支付时间",
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
                onClick={() => setDetailNo(row.original.paymentNo)}
              >
                <Eye />
                详情
              </Button>
              {row.original.status === 1 && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-destructive h-7 px-2 text-xs"
                  onClick={() => setCancelTarget(row.original)}
                >
                  <XCircle />
                  取消
                </Button>
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
  const numericColumns = new Set(["amount"]);

  const applyFilters = () => {
    setOrderNo(orderNoDraft.trim());
    setUserId(userIdDraft.trim());
    setPaymentChannel(channelDraft.trim());
    setPage(1);
  };

  const amountValue = Number(createForm.amount);
  const canCreate = Boolean(
    createForm.orderNo.trim() &&
      createForm.userId.trim() &&
      createForm.paymentChannel.trim() &&
      createForm.amount.trim() &&
      Number.isFinite(amountValue) &&
      amountValue > 0
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="支付单管理"
        description="创建、查询与取消电商支付单"
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
                setCreateForm(emptyPaymentForm);
                setCreateOpen(true);
              }}
            >
              <Plus />
              创建支付单
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
            <Input
              value={channelDraft}
              onChange={(event) => setChannelDraft(event.target.value)}
              placeholder="支付渠道"
              className="h-8 w-36 text-xs"
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
                <SelectItem value="1">待支付</SelectItem>
                <SelectItem value="2">已支付</SelectItem>
                <SelectItem value="3">失败</SelectItem>
                <SelectItem value="4">已取消</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无支付单数据</TableEmpty>
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
            <DialogTitle>支付单详情</DialogTitle>
            <DialogDescription>{detailNo}</DialogDescription>
          </DialogHeader>

          {detailQuery.isLoading ? (
            <p className="text-muted-foreground text-xs">加载中…</p>
          ) : detailQuery.data ? (
            <div className="grid gap-3 sm:grid-cols-3">
              <Info label="支付单号" value={detailQuery.data.paymentNo} />
              <Info label="订单号" value={detailQuery.data.orderNo} />
              <Info label="用户 ID" value={detailQuery.data.userId} />
              <Info label="金额" value={formatAmount(detailQuery.data.amount)} />
              <Info
                label="币种"
                value={detailQuery.data.currency || "—"}
              />
              <Info
                label="支付渠道"
                value={detailQuery.data.paymentChannel || "—"}
              />
              <Info
                label="状态"
                value={statusMeta(paymentStatusMap, detailQuery.data.status).label}
              />
              <Info
                label="交易号"
                value={detailQuery.data.transactionId || "—"}
              />
              <Info
                label="支付时间"
                value={formatDateTime(detailQuery.data.paidAt)}
              />
              <Info
                label="创建时间"
                value={formatDateTime(detailQuery.data.createdAt)}
              />
              <Info
                label="回调地址"
                value={detailQuery.data.callbackUrl || "—"}
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
            <DialogTitle>创建支付单</DialogTitle>
            <DialogDescription>
              同一订单与渠道不会重复创建待支付单
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
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
            <Field label="金额">
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
            <Field label="支付渠道">
              <Input
                value={createForm.paymentChannel}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    paymentChannel: event.target.value,
                  }))
                }
                placeholder="如 ALIPAY / WECHAT"
              />
            </Field>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={createPayment.isPending || !canCreate}
              onClick={() => createPayment.mutate()}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!cancelTarget}
        title="取消支付单"
        description={`确定取消支付单 ${cancelTarget?.paymentNo ?? ""} 吗？`}
        confirmText="取消支付单"
        destructive
        pending={cancelPayment.isPending}
        onOpenChange={(open) => !open && setCancelTarget(null)}
        onConfirm={() =>
          cancelTarget && cancelPayment.mutate(cancelTarget.paymentNo)
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
