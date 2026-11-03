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
  CheckCircle,
  CreditCard,
  Eye,
  MapPin,
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
import { Textarea } from "@/components/ui/textarea";
import { toApiError } from "@/lib/api/envelope";
import { orderCenterApi } from "@/lib/api/services/ecommerce";
import type { OcOrder, OcOrderItem } from "@/lib/api/contracts/ecommerce";
import {
  formatAmount,
  formatDateTime,
  orderCenterStatusMap,
  statusMeta,
  toNumberOrUndefined,
} from "../lib/status";

const PAGE_SIZE = 20;
const ALL = "__all__";
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, OcOrder>();
const emptyRows: OcOrder[] = [];
/** Spread target so the create payload carries every declared field except `id`. */
const emptyOcOrder = {} as OcOrder;

interface OrderFormState {
  orderNo: string;
  userId: string;
  channel: string;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  remark: string;
  totalAmount: string;
  productId: string;
  skuId: string;
  productName: string;
  skuName: string;
  price: string;
  quantity: string;
}

const emptyOrderForm: OrderFormState = {
  orderNo: "",
  userId: "",
  channel: "",
  receiverName: "",
  receiverPhone: "",
  receiverAddress: "",
  remark: "",
  totalAmount: "",
  productId: "",
  skuId: "",
  productName: "",
  skuName: "",
  price: "",
  quantity: "1",
};

export default function OrderCenterManage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [orderNoDraft, setOrderNoDraft] = useState("");
  const [userIdDraft, setUserIdDraft] = useState("");
  const [orderNo, setOrderNo] = useState("");
  const [userId, setUserId] = useState("");
  const [status, setStatus] = useState<number | undefined>();

  const [detailNo, setDetailNo] = useState<string | null>(null);

  const [payTarget, setPayTarget] = useState<OcOrder | null>(null);
  const [paymentNo, setPaymentNo] = useState("");
  const [shipTarget, setShipTarget] = useState<OcOrder | null>(null);
  const [logisticsNo, setLogisticsNo] = useState("");
  const [cancelTarget, setCancelTarget] = useState<OcOrder | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [deliverTarget, setDeliverTarget] = useState<OcOrder | null>(null);
  const [confirmTarget, setConfirmTarget] = useState<OcOrder | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] =
    useState<OrderFormState>(emptyOrderForm);

  const listQuery = useQuery({
    queryKey: ["ecommerce", "order-center", page, orderNo, userId, status],
    queryFn: () =>
      orderCenterApi.page({
        page,
        size: PAGE_SIZE,
        orderNo: orderNo || undefined,
        userId: userId || undefined,
        status,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ["ecommerce", "order-center", "detail", detailNo],
    queryFn: () => orderCenterApi.get(detailNo!),
    enabled: !!detailNo,
  });

  const invalidateOrders = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "order-center"] });

  const createOrder = useMutation({
    mutationFn: (payload: { order: OcOrder; items: OcOrderItem[] }) =>
      orderCenterApi.create(payload.order, payload.items),
    onSuccess: () => {
      message.success("订单已创建");
      setCreateOpen(false);
      setCreateForm(emptyOrderForm);
      void invalidateOrders();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const payOrder = useMutation({
    mutationFn: () =>
      orderCenterApi.pay(payTarget!.orderNo, paymentNo.trim()),
    onSuccess: () => {
      message.success("订单已支付");
      setPayTarget(null);
      setPaymentNo("");
      void invalidateOrders();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const shipOrder = useMutation({
    mutationFn: () =>
      orderCenterApi.ship(shipTarget!.orderNo, logisticsNo.trim()),
    onSuccess: () => {
      message.success("订单已发货");
      setShipTarget(null);
      setLogisticsNo("");
      void invalidateOrders();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const cancelOrder = useMutation({
    mutationFn: () =>
      orderCenterApi.cancel(
        cancelTarget!.orderNo,
        cancelReason.trim() || undefined
      ),
    onSuccess: () => {
      message.success("订单已取消");
      setCancelTarget(null);
      setCancelReason("");
      void invalidateOrders();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const deliverOrder = useMutation({
    mutationFn: (orderNoValue: string) => orderCenterApi.deliver(orderNoValue),
    onSuccess: () => {
      message.success("订单已标记送达");
      setDeliverTarget(null);
      void invalidateOrders();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const confirmOrder = useMutation({
    mutationFn: (orderNoValue: string) => orderCenterApi.confirm(orderNoValue),
    onSuccess: () => {
      message.success("订单已确认收货");
      setConfirmTarget(null);
      void invalidateOrders();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("orderNo", {
          header: "订单号",
          cell: (info) => (
            <span className="text-foreground font-mono text-[11px] font-semibold">
              {info.getValue()}
            </span>
          ),
        }),
        columnHelper.accessor("userId", {
          header: "用户 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("channel", {
          header: "渠道",
          cell: (info) => info.getValue() || "—",
        }),
        columnHelper.accessor("payableAmount", {
          header: "应付金额",
          cell: (info) => (
            <span className="font-semibold">
              {formatAmount(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(orderCenterStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.accessor("receiverName", {
          header: "收货人",
          cell: (info) => info.getValue() || "—",
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
            const order = row.original;
            const orderStatus = order.status;
            return (
              <div className="flex items-center justify-end gap-1">
                {orderStatus === 1 && (
                  <>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-7 px-2 text-xs"
                      onClick={() => {
                        setPaymentNo("");
                        setPayTarget(order);
                      }}
                    >
                      <CreditCard />
                      支付
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive h-7 px-2 text-xs"
                      onClick={() => {
                        setCancelReason("");
                        setCancelTarget(order);
                      }}
                    >
                      <XCircle />
                      取消
                    </Button>
                  </>
                )}
                {(orderStatus === 2 || orderStatus === 3) && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => {
                      setLogisticsNo("");
                      setShipTarget(order);
                    }}
                  >
                    <Truck />
                    发货
                  </Button>
                )}
                {orderStatus === 4 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => setDeliverTarget(order)}
                  >
                    <MapPin />
                    送达
                  </Button>
                )}
                {orderStatus === 5 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    onClick={() => setConfirmTarget(order)}
                  >
                    <CheckCircle />
                    确认收货
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 px-2 text-xs"
                  onClick={() => setDetailNo(order.orderNo)}
                >
                  <Eye />
                  详情
                </Button>
              </div>
            );
          },
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
  const numericColumns = new Set(["payableAmount"]);

  const applyFilters = () => {
    setOrderNo(orderNoDraft.trim());
    setUserId(userIdDraft.trim());
    setPage(1);
  };

  const totalAmountValue = toNumberOrUndefined(createForm.totalAmount);
  const priceValue = toNumberOrUndefined(createForm.price);
  const quantityValue = toNumberOrUndefined(createForm.quantity);

  const canCreate = Boolean(
    createForm.orderNo.trim() &&
      createForm.userId.trim() &&
      (!createForm.totalAmount.trim() || totalAmountValue !== undefined) &&
      priceValue !== undefined &&
      priceValue >= 0 &&
      quantityValue !== undefined &&
      Number.isInteger(quantityValue) &&
      quantityValue >= 1
  );

  const submitCreate = () => {
    const order: OcOrder = {
      ...emptyOcOrder,
      orderNo: createForm.orderNo.trim(),
      userId: createForm.userId.trim(),
      channel: createForm.channel.trim() || undefined,
      receiverName: createForm.receiverName.trim() || undefined,
      receiverPhone: createForm.receiverPhone.trim() || undefined,
      receiverAddress: createForm.receiverAddress.trim() || undefined,
      remark: createForm.remark.trim() || undefined,
      totalAmount: totalAmountValue,
    };
    const items: OcOrderItem[] = [
      {
        productId: toNumberOrUndefined(createForm.productId),
        skuId: toNumberOrUndefined(createForm.skuId),
        productName: createForm.productName.trim() || undefined,
        skuName: createForm.skuName.trim() || undefined,
        price: priceValue,
        quantity: quantityValue as number,
      },
    ];
    createOrder.mutate({ order, items });
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="订单中心"
        description="管理订单的支付、发货、送达与确认收货"
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
                setCreateForm(emptyOrderForm);
                setCreateOpen(true);
              }}
            >
              <Plus />
              新建订单
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
                <SelectItem value="1">待支付</SelectItem>
                <SelectItem value="2">已支付</SelectItem>
                <SelectItem value="3">备货中</SelectItem>
                <SelectItem value="4">已发货</SelectItem>
                <SelectItem value="5">已送达</SelectItem>
                <SelectItem value="6">已取消</SelectItem>
                <SelectItem value="7">已完成</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无订单数据</TableEmpty>
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
            <DialogTitle>订单详情</DialogTitle>
            <DialogDescription>{detailNo}</DialogDescription>
          </DialogHeader>

          {detailQuery.isLoading ? (
            <p className="text-muted-foreground text-xs">加载中…</p>
          ) : detailQuery.data ? (
            <div className="space-y-3">
              <div className="grid gap-3 sm:grid-cols-3">
                <Info label="订单号" value={detailQuery.data.orderNo} />
                <Info label="用户 ID" value={detailQuery.data.userId} />
                <Info
                  label="状态"
                  value={
                    statusMeta(orderCenterStatusMap, detailQuery.data.status)
                      .label
                  }
                />
                <Info
                  label="渠道"
                  value={detailQuery.data.channel || "—"}
                />
                <Info
                  label="订单总额"
                  value={formatAmount(detailQuery.data.totalAmount)}
                />
                <Info
                  label="应付金额"
                  value={formatAmount(detailQuery.data.payableAmount)}
                />
                <Info
                  label="已付金额"
                  value={formatAmount(detailQuery.data.paidAmount)}
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
                  label="备注"
                  value={detailQuery.data.remark || "—"}
                />
                <Info
                  label="创建时间"
                  value={formatDateTime(detailQuery.data.createdAt)}
                />
              </div>
              <p className="text-muted-foreground text-[11px]">
                后端详情接口仅返回订单头信息，不包含商品明细。
              </p>
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
        <DialogContent className="max-h-[86vh] max-w-3xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>新建订单</DialogTitle>
            <DialogDescription>
              订单总额留空时由后端按明细金额计算
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-3">
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
            <Field label="渠道">
              <Input
                value={createForm.channel}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    channel: event.target.value,
                  }))
                }
                placeholder="如 APP / WEB"
              />
            </Field>
            <Field label="收货人">
              <Input
                value={createForm.receiverName}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    receiverName: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </Field>
            <Field label="收货电话">
              <Input
                value={createForm.receiverPhone}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    receiverPhone: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </Field>
            <Field label="订单总额">
              <Input
                type="number"
                min={0}
                step="0.01"
                value={createForm.totalAmount}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    totalAmount: event.target.value,
                  }))
                }
                placeholder="可选，留空自动计算"
              />
            </Field>
            <div className="space-y-1 sm:col-span-3">
              <Label className="text-xs">收货地址</Label>
              <Input
                value={createForm.receiverAddress}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    receiverAddress: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1 sm:col-span-3">
              <Label className="text-xs">备注</Label>
              <Textarea
                value={createForm.remark}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    remark: event.target.value,
                  }))
                }
                rows={2}
                placeholder="可选"
              />
            </div>
          </div>

          <div className="space-y-3 rounded-lg border border-[var(--color-border-secondary)] p-3">
            <h3 className="text-[13px] font-semibold">订单明细（1 件商品）</h3>
            <div className="grid gap-3 sm:grid-cols-3">
              <Field label="商品 ID">
                <Input
                  type="number"
                  min={0}
                  value={createForm.productId}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      productId: event.target.value,
                    }))
                  }
                  placeholder="可选"
                />
              </Field>
              <Field label="SKU ID">
                <Input
                  type="number"
                  min={0}
                  value={createForm.skuId}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      skuId: event.target.value,
                    }))
                  }
                  placeholder="可选"
                />
              </Field>
              <Field label="数量">
                <Input
                  type="number"
                  min={1}
                  step={1}
                  value={createForm.quantity}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      quantity: event.target.value,
                    }))
                  }
                  placeholder="必填，大于 0"
                />
              </Field>
              <Field label="商品名称">
                <Input
                  value={createForm.productName}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      productName: event.target.value,
                    }))
                  }
                  placeholder="可选"
                />
              </Field>
              <Field label="SKU 名称">
                <Input
                  value={createForm.skuName}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      skuName: event.target.value,
                    }))
                  }
                  placeholder="可选"
                />
              </Field>
              <Field label="单价">
                <Input
                  type="number"
                  min={0}
                  step="0.01"
                  value={createForm.price}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      price: event.target.value,
                    }))
                  }
                  placeholder="必填"
                />
              </Field>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={createOrder.isPending || !canCreate}
              onClick={submitCreate}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!payTarget}
        onOpenChange={(open) => {
          if (!open) {
            setPayTarget(null);
            setPaymentNo("");
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>订单支付</DialogTitle>
            <DialogDescription>{payTarget?.orderNo}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label className="text-xs">支付单号</Label>
            <Input
              value={paymentNo}
              onChange={(event) => setPaymentNo(event.target.value)}
              placeholder="必填"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPayTarget(null)}>
              取消
            </Button>
            <Button
              disabled={payOrder.isPending || !paymentNo.trim()}
              onClick={() => payOrder.mutate()}
            >
              确认支付
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!shipTarget}
        onOpenChange={(open) => {
          if (!open) {
            setShipTarget(null);
            setLogisticsNo("");
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>订单发货</DialogTitle>
            <DialogDescription>{shipTarget?.orderNo}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label className="text-xs">物流单号</Label>
            <Input
              value={logisticsNo}
              onChange={(event) => setLogisticsNo(event.target.value)}
              placeholder="必填"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShipTarget(null)}>
              取消
            </Button>
            <Button
              disabled={shipOrder.isPending || !logisticsNo.trim()}
              onClick={() => shipOrder.mutate()}
            >
              确认发货
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!cancelTarget}
        onOpenChange={(open) => {
          if (!open) {
            setCancelTarget(null);
            setCancelReason("");
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>取消订单</DialogTitle>
            <DialogDescription>{cancelTarget?.orderNo}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label className="text-xs">取消原因</Label>
            <Textarea
              value={cancelReason}
              onChange={(event) => setCancelReason(event.target.value)}
              rows={3}
              placeholder="可选"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCancelTarget(null)}>
              返回
            </Button>
            <Button
              variant="destructive"
              disabled={cancelOrder.isPending}
              onClick={() => cancelOrder.mutate()}
            >
              确认取消
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deliverTarget}
        title="标记订单送达"
        description={`确认订单 ${deliverTarget?.orderNo ?? ""} 已送达吗？`}
        confirmText="确认送达"
        pending={deliverOrder.isPending}
        onOpenChange={(open) => !open && setDeliverTarget(null)}
        onConfirm={() =>
          deliverTarget && deliverOrder.mutate(deliverTarget.orderNo)
        }
      />

      <ConfirmDialog
        open={!!confirmTarget}
        title="确认收货"
        description={`确认订单 ${confirmTarget?.orderNo ?? ""} 已完成收货吗？`}
        confirmText="确认收货"
        pending={confirmOrder.isPending}
        onOpenChange={(open) => !open && setConfirmTarget(null)}
        onConfirm={() =>
          confirmTarget && confirmOrder.mutate(confirmTarget.orderNo)
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
