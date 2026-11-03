"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import {
  Eye,
  Package,
  PackageCheck,
  Plus,
  RefreshCw,
  Search,
  Truck,
  XCircle,
} from "lucide-react";

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
import { fulfillmentApi } from "@/lib/api/services/ecommerce";
import type { FulfillmentOrder } from "@/lib/api/contracts/ecommerce";
import {
  formatAmount,
  formatDateTime,
  fulfillmentStatusMap,
  statusMeta,
} from "../lib/status";

const PAGE_SIZE = 20;
const ALL = "__all__";
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, FulfillmentOrder>();
const emptyRows: FulfillmentOrder[] = [];

const TYPE_LABELS: Record<string, string> = {
  EXPRESS: "快递",
  PICKUP: "自提",
  LOCAL: "同城配送",
};

const TYPE_OPTIONS = [
  { value: "EXPRESS", label: "快递" },
  { value: "PICKUP", label: "自提" },
  { value: "LOCAL", label: "同城配送" },
];

interface FulfillmentFormState {
  orderNo: string;
  userId: string;
  fulfillmentType: string;
}

const emptyFulfillmentForm: FulfillmentFormState = {
  orderNo: "",
  userId: "",
  fulfillmentType: "EXPRESS",
};

export default function FulfillmentManage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [orderNoDraft, setOrderNoDraft] = useState("");
  const [userIdDraft, setUserIdDraft] = useState("");
  const [orderNo, setOrderNo] = useState("");
  const [userId, setUserId] = useState("");
  const [status, setStatus] = useState<number | undefined>();

  const [detailNo, setDetailNo] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<FulfillmentOrder | null>(
    null
  );
  const [shipTarget, setShipTarget] = useState<FulfillmentOrder | null>(null);
  const [trackingNo, setTrackingNo] = useState("");
  const [carrier, setCarrier] = useState("");

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] =
    useState<FulfillmentFormState>(emptyFulfillmentForm);

  const listQuery = useQuery({
    queryKey: ["ecommerce", "fulfillments", page, orderNo, userId, status],
    queryFn: () =>
      fulfillmentApi.page({
        page,
        size: PAGE_SIZE,
        orderNo: orderNo || undefined,
        userId: userId || undefined,
        status,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ["ecommerce", "fulfillments", "detail", detailNo],
    queryFn: () => fulfillmentApi.get(detailNo!),
    enabled: !!detailNo,
  });

  const invalidateFulfillments = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "fulfillments"] });

  const createFulfillment = useMutation({
    mutationFn: () =>
      fulfillmentApi.create({
        orderNo: createForm.orderNo.trim(),
        userId: createForm.userId.trim(),
        fulfillmentType: createForm.fulfillmentType,
      }),
    onSuccess: () => {
      message.success("履约单已创建");
      setCreateOpen(false);
      setCreateForm(emptyFulfillmentForm);
      void invalidateFulfillments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const pickFulfillment = useMutation({
    mutationFn: (fulfillmentNo: string) => fulfillmentApi.pick(fulfillmentNo),
    onSuccess: () => {
      message.success("已完成拣货");
      void invalidateFulfillments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const packFulfillment = useMutation({
    mutationFn: (fulfillmentNo: string) => fulfillmentApi.pack(fulfillmentNo),
    onSuccess: () => {
      message.success("已完成打包");
      void invalidateFulfillments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const shipFulfillment = useMutation({
    mutationFn: () =>
      fulfillmentApi.ship(
        shipTarget!.fulfillmentNo,
        trackingNo.trim(),
        carrier.trim()
      ),
    onSuccess: () => {
      message.success("履约单已发货");
      setShipTarget(null);
      setTrackingNo("");
      setCarrier("");
      void invalidateFulfillments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const deliverFulfillment = useMutation({
    mutationFn: (fulfillmentNo: string) =>
      fulfillmentApi.deliver(fulfillmentNo),
    onSuccess: () => {
      message.success("履约单已签收");
      void invalidateFulfillments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const cancelFulfillment = useMutation({
    mutationFn: () =>
      fulfillmentApi.cancel(cancelTarget!.fulfillmentNo),
    onSuccess: () => {
      message.success("履约单已取消");
      setCancelTarget(null);
      void invalidateFulfillments();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("fulfillmentNo", {
          header: "履约单号",
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
        columnHelper.accessor("fulfillmentType", {
          header: "履约类型",
          cell: (info) => {
            const value = info.getValue();
            return value ? (TYPE_LABELS[value] ?? value) : "—";
          },
        }),
        columnHelper.accessor("totalItems", {
          header: "商品件数",
          cell: (info) => info.getValue() ?? 0,
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(fulfillmentStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
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
          cell: ({ row }) => {
            const fulfillment = row.original;
            const fulfillmentStatus = fulfillment.status;
            return (
              <div className="flex items-center justify-end gap-1">
                {fulfillmentStatus === 1 && (
                  <>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-7 px-2 text-xs"
                      disabled={pickFulfillment.isPending}
                      onClick={() =>
                        pickFulfillment.mutate(fulfillment.fulfillmentNo)
                      }
                    >
                      <Package />
                      拣货
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive h-7 px-2 text-xs"
                      onClick={() => setCancelTarget(fulfillment)}
                    >
                      <XCircle />
                      取消
                    </Button>
                  </>
                )}
                {fulfillmentStatus === 2 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    disabled={packFulfillment.isPending}
                    onClick={() =>
                      packFulfillment.mutate(fulfillment.fulfillmentNo)
                    }
                  >
                    <Package />
                    打包
                  </Button>
                )}
                {fulfillmentStatus === 3 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => {
                      setTrackingNo("");
                      setCarrier("");
                      setShipTarget(fulfillment);
                    }}
                  >
                    <Truck />
                    发货
                  </Button>
                )}
                {fulfillmentStatus === 4 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    disabled={deliverFulfillment.isPending}
                    onClick={() =>
                      deliverFulfillment.mutate(fulfillment.fulfillmentNo)
                    }
                  >
                    <PackageCheck />
                    签收
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-xs"
                  onClick={() => setDetailNo(fulfillment.fulfillmentNo)}
                >
                  <Eye />
                  详情
                </Button>
              </div>
            );
          },
        }),
      ]),
    [
      pickFulfillment,
      packFulfillment,
      deliverFulfillment,
    ]
  );

  const table = useTable({
    features,
    columns,
    data: listQuery.data?.records ?? emptyRows,
  });

  const total = listQuery.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const numericColumns = new Set(["totalItems"]);

  const applyFilters = () => {
    setOrderNo(orderNoDraft.trim());
    setUserId(userIdDraft.trim());
    setPage(1);
  };

  const canCreate = Boolean(
    createForm.orderNo.trim() &&
      createForm.userId.trim() &&
      createForm.fulfillmentType
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="履约管理"
        description="跟踪履约单的拣货、打包、发货与签收"
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
                setCreateForm(emptyFulfillmentForm);
                setCreateOpen(true);
              }}
            >
              <Plus />
              创建履约单
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
              className="h-8 w-48 text-xs"
            />
            <Input
              value={userIdDraft}
              onChange={(event) => setUserIdDraft(event.target.value)}
              placeholder="用户 ID"
              className="h-8 w-48 text-xs"
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
                <SelectItem value="1">待拣货</SelectItem>
                <SelectItem value="2">已拣货</SelectItem>
                <SelectItem value="3">已打包</SelectItem>
                <SelectItem value="4">已发货</SelectItem>
                <SelectItem value="5">已签收</SelectItem>
                <SelectItem value="6">已取消</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无履约单数据</TableEmpty>
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
            <DialogTitle>履约单详情</DialogTitle>
            <DialogDescription>{detailNo}</DialogDescription>
          </DialogHeader>

          {detailQuery.isLoading ? (
            <p className="text-muted-foreground text-xs">加载中…</p>
          ) : detailQuery.data ? (
            <div className="grid gap-3 sm:grid-cols-3">
              <Info
                label="履约单号"
                value={detailQuery.data.fulfillmentNo}
              />
              <Info label="订单号" value={detailQuery.data.orderNo} />
              <Info label="用户 ID" value={detailQuery.data.userId} />
              <Info
                label="履约类型"
                value={
                  detailQuery.data.fulfillmentType
                    ? (TYPE_LABELS[detailQuery.data.fulfillmentType] ??
                      detailQuery.data.fulfillmentType)
                    : "—"
                }
              />
              <Info
                label="状态"
                value={statusMeta(fulfillmentStatusMap, detailQuery.data.status)
                  .label}
              />
              <Info
                label="仓库 ID"
                value={String(detailQuery.data.warehouseId ?? "—")}
              />
              <Info
                label="配送方式"
                value={detailQuery.data.shippingMethod || "—"}
              />
              <Info
                label="收货人"
                value={detailQuery.data.receiverName || "—"}
              />
              <Info
                label="联系电话"
                value={detailQuery.data.receiverPhone || "—"}
              />
              <Info
                label="收货地址"
                value={detailQuery.data.receiverAddress || "—"}
              />
              <Info
                label="商品件数"
                value={String(detailQuery.data.totalItems ?? 0)}
              />
              <Info
                label="运费"
                value={formatAmount(detailQuery.data.shippingFee)}
              />
              <Info
                label="创建时间"
                value={formatDateTime(detailQuery.data.createdAt)}
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
            <DialogTitle>创建履约单</DialogTitle>
            <DialogDescription>
              为已支付订单创建履约任务
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
            <div className="space-y-1 sm:col-span-2">
              <Label className="text-xs">履约类型</Label>
              <Select
                value={createForm.fulfillmentType}
                onValueChange={(value) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    fulfillmentType: value,
                  }))
                }
              >
                <SelectTrigger className="w-full text-xs">
                  <SelectValue placeholder="请选择履约类型" />
                </SelectTrigger>
                <SelectContent>
                  {TYPE_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={createFulfillment.isPending || !canCreate}
              onClick={() => createFulfillment.mutate()}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!shipTarget}
        onOpenChange={(open) => {
          if (!open) {
            setShipTarget(null);
            setTrackingNo("");
            setCarrier("");
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>履约发货</DialogTitle>
            <DialogDescription>{shipTarget?.fulfillmentNo}</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1">
              <Label className="text-xs">物流单号</Label>
              <Input
                value={trackingNo}
                onChange={(event) => setTrackingNo(event.target.value)}
                placeholder="必填"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">承运商</Label>
              <Input
                value={carrier}
                onChange={(event) => setCarrier(event.target.value)}
                placeholder="必填，如 SF / JD"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShipTarget(null)}>
              取消
            </Button>
            <Button
              disabled={
                shipFulfillment.isPending ||
                !trackingNo.trim() ||
                !carrier.trim()
              }
              onClick={() => shipFulfillment.mutate()}
            >
              确认发货
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!cancelTarget}
        title="取消履约单"
        description={`确定取消履约单 ${cancelTarget?.fulfillmentNo ?? ""} 吗？`}
        confirmText="取消履约单"
        destructive
        pending={cancelFulfillment.isPending}
        onOpenChange={(open) => !open && setCancelTarget(null)}
        onConfirm={() => cancelFulfillment.mutate()}
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
