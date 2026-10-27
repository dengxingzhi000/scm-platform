import { describe, expect, it } from "vitest";

import type {
  OrderItemResponse,
  OrderPage,
  OrderResponse,
} from "@/lib/api/contracts/order";
import { toOrder, toOrderItem, toOrderPage } from "./mappers";

const itemFixture: OrderItemResponse = {
  id: "item-1",
  skuId: null,
  spuId: null,
  skuCode: "SKU-001-128",
  skuName: "iPhone 15 Pro 128G",
  spuName: "iPhone 15 Pro",
  sellingPrice: 7999,
  quantity: 2,
  subtotal: null,
  finalAmount: null,
  discountAmount: 100,
  refundQuantity: 0,
};

const orderFixture: OrderResponse = {
  id: "0f8fad5b-d9cb-469f-a165-70867728950e",
  orderNo: "ORD-2026-001",
  userId: "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  username: "张三",
  status: "COMPLETED",
  statusName: "已完成",
  totalAmount: 15998,
  discountAmount: 1000,
  payableAmount: 14998,
  paidAmount: 14998,
  createTime: "2026-06-01T10:00:00",
  updateTime: "2026-06-05T16:30:00",
  paidAt: "2026-06-01T10:30:00",
  items: null,
};

describe("order mappers", () => {
  it("derives a line total when subtotal is null server-side", () => {
    const item = toOrderItem(itemFixture);

    expect(item.totalPrice).toBe(15998);
    expect(item.productName).toBe("iPhone 15 Pro 128G");
    expect(item.skuId).toBeUndefined();
  });

  it("prefers finalAmount over the derived total", () => {
    expect(
      toOrderItem({ ...itemFixture, finalAmount: 14998 }).totalPrice
    ).toBe(14998);
  });

  it("maps the order row and formats its timestamps", () => {
    const order = toOrder(orderFixture, [itemFixture]);

    expect(order.statusLabel).toBe("已完成");
    expect(order.payableAmount).toBe(14998);
    expect(order.payTime).toBe("2026-06-01T10:30:00");
    expect(order.createdAt).toBe("2026-06-01 10:00");
    expect(order.items).toHaveLength(1);
  });

  it("falls back to a local label when statusName is absent", () => {
    const order = toOrder({ ...orderFixture, status: "REFUNDED", statusName: null });

    expect(order.statusLabel).toBe("已退款");
  });

  it("translates the MyBatis-Plus page echo into UI paging", () => {
    const page: OrderPage = {
      records: [orderFixture],
      total: 41,
      size: 20,
      current: 2,
      pages: 3,
    };

    expect(toOrderPage(page)).toEqual({
      records: [expect.objectContaining({ orderNo: "ORD-2026-001" })],
      total: 41,
      current: 2,
      pageSize: 20,
    });
  });

  it("survives an empty page", () => {
    expect(toOrderPage(null)).toEqual({ records: [], total: 0, current: 1, pageSize: 0 });
  });
});
