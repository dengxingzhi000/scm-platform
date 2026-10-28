import { orderApiService } from "@/lib/api/services/order";
import { toOrder, toOrderPage, toRefund } from "../lib/mappers";
import type {
  Order,
  OrderSearchParams,
  OrderStatus,
  PageResult,
  RefundRequest,
} from "../types";

const statusFlow: Record<OrderStatus, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: "待付款", color: "warning" },
  PAID: { label: "已付款", color: "processing" },
  PENDING_SHIP: { label: "待发货", color: "processing" },
  SHIPPED: { label: "已发货", color: "processing" },
  IN_TRANSIT: { label: "运输中", color: "processing" },
  DELIVERED: { label: "已送达", color: "processing" },
  COMPLETED: { label: "已完成", color: "success" },
  CANCELLED: { label: "已取消", color: "default" },
  REFUNDING: { label: "退款中", color: "error" },
  REFUNDED: { label: "已退款", color: "error" },
};

export const orderService = {
  list: async (params?: OrderSearchParams): Promise<PageResult<Order>> =>
    toOrderPage(
      await orderApiService.queryOrders({
        pageNum: params?.current ?? 1,
        pageSize: params?.pageSize ?? 20,
        orderNo: params?.orderNo || undefined,
        userId: params?.userId || undefined,
        status: params?.status || undefined,
        startTime: params?.startTime || undefined,
        endTime: params?.endTime || undefined,
      })
    ),

  /** Line items are a second request: `OrderResponse.items` is always null. */
  getById: async (id: string): Promise<Order> => {
    const [dto, items] = await Promise.all([
      orderApiService.getOrder(id),
      orderApiService.listOrderItems(id).catch(() => []),
    ]);
    return toOrder(dto, items);
  },

  updateStatus: async (id: string, status: OrderStatus): Promise<Order> =>
    toOrder(await orderApiService.updateOrderStatus(id, { targetStatus: status })),

  cancel: async (id: string, reason: string): Promise<Order> =>
    toOrder(await orderApiService.cancelOrder(id, { reason })),

  listRefundsByOrder: async (orderId: string): Promise<RefundRequest[]> =>
    (await orderApiService.listRefundsByOrder(orderId)).map(toRefund),

  listStatusHistory: (orderId: string) =>
    orderApiService.listStatusHistoryByOrder(orderId),

  getStatusFlow: () => statusFlow,
};
