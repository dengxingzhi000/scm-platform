import type { MpPage } from "./page";

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PAID"
  | "PENDING_SHIP"
  | "SHIPPED"
  | "IN_TRANSIT"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "REFUNDING"
  | "REFUNDED";

export type RefundStatus = "PENDING" | "APPROVED" | "REJECTED" | "COMPLETED";

export type PaymentStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUCCESS"
  | "FAILED"
  | "REFUNDING"
  | "REFUNDED"
  | "CANCELLED";

/** `com.scmcloud.order.dto.OrderResponse`. `items` is never populated by the API. */
export interface OrderResponse {
  id: string;
  orderNo: string;
  userId: string;
  username?: string | null;
  orderType?: number | null;
  orderSource?: string | null;
  status: OrderStatus;
  statusName?: string | null;
  totalAmount?: number | null;
  discountAmount?: number | null;
  freightAmount?: number | null;
  payableAmount?: number | null;
  paidAmount?: number | null;
  paymentMethod?: number | null;
  paymentNo?: string | null;
  paidAt?: string | null;
  shippingAddress?: string | null;
  warehouseId?: string | null;
  skuId?: string | null;
  quantity?: number | null;
  waybillNo?: string | null;
  carrier?: string | null;
  shippedAt?: string | null;
  estimatedArrival?: string | null;
  paymentDeadline?: string | null;
  autoCancelAt?: string | null;
  autoCompleteAt?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  cancelReason?: string | null;
  reservationId?: string | null;
  buyerMessage?: string | null;
  sellerMessage?: string | null;
  tags?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  items?: OrderItemResponse[] | null;
}

/** `OrderItemResponse`. `skuId`/`spuId`/`subtotal` come back null (entity type mismatch server-side). */
export interface OrderItemResponse {
  id: string;
  skuId?: string | null;
  spuId?: string | null;
  skuCode?: string | null;
  skuName?: string | null;
  spuName?: string | null;
  attributes?: string | null;
  imageUrl?: string | null;
  originalPrice?: number | null;
  sellingPrice?: number | null;
  quantity?: number | null;
  subtotal?: number | null;
  discountAmount?: number | null;
  finalAmount?: number | null;
  warehouseId?: string | null;
  refundQuantity?: number | null;
  refundAmount?: number | null;
  remark?: string | null;
}

export interface RefundResponse {
  id: string;
  refundNo?: string | null;
  orderId?: string | null;
  orderNo?: string | null;
  userId?: string | null;
  refundType?: number | null;
  reason?: string | null;
  description?: string | null;
  refundAmount?: number | null;
  refundStatus?: RefundStatus | null;
  approvedAt?: string | null;
  rejectedAt?: string | null;
  completedAt?: string | null;
  handlerId?: string | null;
  handlerName?: string | null;
  handlerRemark?: string | null;
  returnWaybillNo?: string | null;
  returnCarrier?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  items?: RefundItemResponse[] | null;
}

export interface RefundItemResponse {
  id: string;
  refundId?: string | null;
  refundNo?: string | null;
  orderId?: string | null;
  orderNo?: string | null;
  orderItemId?: string | null;
  skuId?: string | null;
  skuCode?: string | null;
  skuName?: string | null;
  quantity?: number | null;
  refundAmount?: number | null;
  remark?: string | null;
}

export interface PaymentResponse {
  id: string;
  paymentNo?: string | null;
  orderId?: string | null;
  orderNo?: string | null;
  userId?: string | null;
  paymentMethod?: number | null;
  paymentChannel?: string | null;
  paymentAmount?: number | null;
  paymentStatus?: PaymentStatus | null;
  thirdPartyNo?: string | null;
  initiatedAt?: string | null;
  paidAt?: string | null;
  failedAt?: string | null;
  refundedAt?: string | null;
  refundAmount?: number | null;
  refundReason?: string | null;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** `fromStatus` is a numeric code while `toStatus` is the enum name — mixed by design. */
export interface StatusHistoryResponse {
  id: string;
  orderId?: string | null;
  orderNo?: string | null;
  fromStatus?: number | null;
  fromStatusName?: string | null;
  toStatus?: OrderStatus | null;
  toStatusName?: string | null;
  event?: string | null;
  operatorId?: string | null;
  operatorName?: string | null;
  operatorType?: string | null;
  remark?: string | null;
  transitionedAt?: string | null;
}

/** All paging is POST-body: `pageNum`/`pageSize` in, MyBatis-Plus `Page` out. */
export interface OrderQueryRequest {
  orderNo?: string;
  userId?: string;
  status?: OrderStatus;
  orderSource?: string;
  startTime?: string;
  endTime?: string;
  pageNum: number;
  pageSize: number;
}

export interface OrderStatusUpdateRequest {
  targetStatus: OrderStatus;
  reason?: string;
}

export interface OrderCancelRequest {
  reason: string;
}

export interface RefundQueryRequest {
  refundNo?: string;
  orderId?: string;
  userId?: string;
  status?: RefundStatus;
  startTime?: string;
  endTime?: string;
  pageNum: number;
  pageSize: number;
}

/** Every field the backend marks required must be sent, including `items`. */
export interface RefundItemRequest {
  orderItemId: string;
  skuId: string;
  skuCode?: string;
  skuName: string;
  quantity: number;
  refundAmount: number;
  remark?: string;
}

export interface RefundCreateRequest {
  refundNo: string;
  orderId: string;
  orderNo: string;
  userId: string;
  refundType: number;
  reason: string;
  description?: string;
  refundAmount: number;
  items: RefundItemRequest[];
}

export interface PaymentQueryRequest {
  paymentNo?: string;
  orderId?: string;
  userId?: string;
  status?: PaymentStatus;
  startTime?: string;
  endTime?: string;
  pageNum: number;
  pageSize: number;
}

export interface PaymentCreateRequest {
  paymentNo: string;
  orderId: string;
  orderNo: string;
  userId: string;
  paymentMethod: number;
  paymentChannel?: string;
  paymentAmount: number;
  remark?: string;
}

export interface PaymentStatusUpdateRequest {
  targetStatus: PaymentStatus;
  reason?: string;
}

export type OrderPage = MpPage<OrderResponse>;
export type OrderItemPage = MpPage<OrderItemResponse>;
export type RefundPage = MpPage<RefundResponse>;
export type PaymentPage = MpPage<PaymentResponse>;
