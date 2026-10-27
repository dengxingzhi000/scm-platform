export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PENDING_SHIP'
  | 'SHIPPED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDING'
  | 'REFUNDED'

export interface Order {
  id: string
  orderNo: string
  userId: string
  /** The order API exposes no customer phone; `username` is the only identity it returns. */
  username: string
  status: OrderStatus
  statusLabel: string
  orderType?: number
  orderSource?: string
  totalAmount: number
  discountAmount: number
  freightAmount: number
  payableAmount: number
  paidAmount: number
  shippingAddress?: string
  warehouseId?: string
  waybillNo?: string
  carrier?: string
  paymentMethod?: number
  paymentNo?: string
  buyerMessage?: string
  remark?: string
  cancelReason?: string
  payTime?: string
  shipTime?: string
  completeTime?: string
  cancelTime?: string
  createdAt: string
  updatedAt: string
  /** Fetched separately: `GET /orders/{id}/items`. The list response never embeds items. */
  items: OrderItem[]
}

export interface OrderItem {
  id: string
  skuCode: string
  productName: string
  skuId?: string
  warehouseId?: string
  quantity: number
  unitPrice: number
  totalPrice: number
  discountAmount: number
  refundQuantity: number
  attributes?: string
  imageUrl?: string
}

export interface RefundRequest {
  id: string
  refundNo: string
  orderId: string
  orderNo: string
  reason: string
  amount: number
  /** Server sends the enum name: PENDING | APPROVED | REJECTED | COMPLETED. */
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED'
  handlerName?: string
  createdAt: string
}

export interface OrderSearchParams {
  current?: number
  pageSize?: number
  /** Exact match on `order_no`; the query API has no fuzzy keyword. */
  orderNo?: string
  userId?: string
  status?: OrderStatus
  startTime?: string
  endTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
