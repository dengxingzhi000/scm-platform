import dayjs from "dayjs";

import type {
  OrderItemResponse,
  OrderPage,
  OrderResponse,
  RefundResponse,
} from "@/lib/api/contracts/order";
import type { Order, OrderItem, PageResult, RefundRequest } from "../types";

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "待付款",
  PAID: "已付款",
  PENDING_SHIP: "待发货",
  SHIPPED: "已发货",
  IN_TRANSIT: "运输中",
  DELIVERED: "已送达",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  REFUNDING: "退款中",
  REFUNDED: "已退款",
};

const money = (value?: number | null): number => value ?? 0;

export const formatDateTime = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";

const labelFor = (order: Pick<OrderResponse, "status" | "statusName">): string =>
  order.statusName || STATUS_LABELS[order.status] || order.status;

/**
 * `subtotal` is never computed server-side and `skuId`/`spuId` arrive null on item DTOs,
 * so the line total falls back to unit price x quantity.
 */
export const toOrderItem = (dto: OrderItemResponse): OrderItem => {
  const unitPrice = money(dto.sellingPrice);
  const quantity = dto.quantity ?? 0;
  return {
    id: dto.id,
    skuCode: dto.skuCode ?? "-",
    productName: dto.skuName || dto.spuName || "-",
    skuId: dto.skuId ?? undefined,
    warehouseId: dto.warehouseId ?? undefined,
    quantity,
    unitPrice,
    totalPrice: dto.finalAmount ?? dto.subtotal ?? unitPrice * quantity,
    discountAmount: money(dto.discountAmount),
    refundQuantity: dto.refundQuantity ?? 0,
    attributes: dto.attributes ?? undefined,
    imageUrl: dto.imageUrl ?? undefined,
  };
};

export const toOrder = (
  dto: OrderResponse,
  items?: OrderItemResponse[]
): Order => ({
  id: dto.id,
  orderNo: dto.orderNo,
  userId: dto.userId,
  username: dto.username ?? "-",
  status: dto.status,
  statusLabel: labelFor(dto),
  orderType: dto.orderType ?? undefined,
  orderSource: dto.orderSource ?? undefined,
  totalAmount: money(dto.totalAmount),
  discountAmount: money(dto.discountAmount),
  freightAmount: money(dto.freightAmount),
  payableAmount: money(dto.payableAmount),
  paidAmount: money(dto.paidAmount),
  shippingAddress: dto.shippingAddress ?? undefined,
  warehouseId: dto.warehouseId ?? undefined,
  waybillNo: dto.waybillNo ?? undefined,
  carrier: dto.carrier ?? undefined,
  paymentMethod: dto.paymentMethod ?? undefined,
  paymentNo: dto.paymentNo ?? undefined,
  buyerMessage: dto.buyerMessage ?? undefined,
  remark: dto.remark ?? undefined,
  cancelReason: dto.cancelReason ?? undefined,
  payTime: dto.paidAt ?? undefined,
  shipTime: dto.shippedAt ?? undefined,
  completeTime: dto.completedAt ?? undefined,
  cancelTime: dto.cancelledAt ?? undefined,
  createdAt: formatDateTime(dto.createTime),
  updatedAt: formatDateTime(dto.updateTime),
  items: (items ?? dto.items ?? []).map(toOrderItem),
});

export const toOrderPage = (page: OrderPage | null | undefined): PageResult<Order> => ({
  records: (page?.records ?? []).map((row) => toOrder(row)),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});

export const toRefund = (dto: RefundResponse): RefundRequest => ({
  id: dto.id,
  refundNo: dto.refundNo ?? "-",
  orderId: dto.orderId ?? "",
  orderNo: dto.orderNo ?? "-",
  reason: dto.reason ?? "-",
  amount: money(dto.refundAmount),
  status: dto.refundStatus ?? "PENDING",
  handlerName: dto.handlerName ?? undefined,
  createdAt: formatDateTime(dto.createTime),
});
