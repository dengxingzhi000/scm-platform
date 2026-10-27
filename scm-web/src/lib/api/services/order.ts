import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  OrderCancelRequest,
  OrderItemPage,
  OrderItemResponse,
  OrderPage,
  OrderQueryRequest,
  OrderResponse,
  OrderStatusUpdateRequest,
  PaymentPage,
  PaymentResponse,
  RefundPage,
  RefundQueryRequest,
  RefundResponse,
  StatusHistoryResponse,
} from "../contracts/order";

/**
 * scm-order is reached through the gateway's Nacos discovery locator
 * (`/scm-<service>/**` with the first segment stripped), so the service sees
 * its own `/api/v1/**` paths.
 */
const ORDER_BASE = "/scm-order/api/v1";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.post(url, data));

const put = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.put(url, data));

export const orderApiService = {
  getOrder: (id: string) => get<OrderResponse>(`${ORDER_BASE}/orders/${id}`),
  queryOrders: (request: OrderQueryRequest) =>
    post<OrderPage>(`${ORDER_BASE}/orders/query`, request),
  listOrderItems: (orderId: string) =>
    get<OrderItemResponse[]>(`${ORDER_BASE}/orders/${orderId}/items`),
  queryOrderItems: (request: {
    orderId: string;
    pageNum: number;
    pageSize: number;
  }) => post<OrderItemPage>(`${ORDER_BASE}/order-items/query`, request),
  createOrder: (request: unknown) =>
    post<OrderResponse>(`${ORDER_BASE}/orders`, request),
  updateOrderStatus: (id: string, request: OrderStatusUpdateRequest) =>
    put<OrderResponse>(`${ORDER_BASE}/orders/${id}/status`, request),
  cancelOrder: (id: string, request: OrderCancelRequest) =>
    put<OrderResponse>(`${ORDER_BASE}/orders/${id}/cancel`, request),

  getRefund: (id: string) => get<RefundResponse>(`${ORDER_BASE}/refunds/${id}`),
  listRefundItems: (id: string) =>
    get<RefundResponse>(`${ORDER_BASE}/refunds/${id}/items`),
  listRefundsByOrder: (orderId: string) =>
    get<RefundResponse[]>(`${ORDER_BASE}/refunds/order/${orderId}`),
  queryRefunds: (request: RefundQueryRequest) =>
    post<RefundPage>(`${ORDER_BASE}/refunds/query`, request),
  approveRefund: (
    id: string,
    request: { handlerId: string; handlerName?: string; handlerRemark?: string }
  ) => put<RefundResponse>(`${ORDER_BASE}/refunds/${id}/approve`, request),
  rejectRefund: (
    id: string,
    request: { handlerId: string; handlerName?: string; handlerRemark: string }
  ) => put<RefundResponse>(`${ORDER_BASE}/refunds/${id}/reject`, request),
  completeRefund: (id: string) =>
    put<RefundResponse>(`${ORDER_BASE}/refunds/${id}/complete`),

  getPayment: (id: string) =>
    get<PaymentResponse>(`${ORDER_BASE}/payments/${id}`),
  listPaymentsByOrder: (orderId: string) =>
    get<PaymentResponse[]>(`${ORDER_BASE}/payments/order/${orderId}`),
  queryPayments: (request: unknown) =>
    post<PaymentPage>(`${ORDER_BASE}/payments/query`, request),

  listStatusHistoryByOrder: (orderId: string) =>
    get<StatusHistoryResponse[]>(`${ORDER_BASE}/status-history/order/${orderId}`),
};
