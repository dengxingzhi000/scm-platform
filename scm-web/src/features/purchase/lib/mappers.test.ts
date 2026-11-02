import { describe, expect, it } from "vitest";

import type {
  PurOrderDTO,
  PurOrderItemDTO,
} from "@/lib/api/contracts/purchase";
import {
  purchaseStatusToApi,
  toPurchaseOrder,
  toPurchaseOrderItem,
  toPurchaseOrderPage,
} from "./mappers";

const orderFixture: PurOrderDTO = {
  id: "po-1",
  orderNo: "PO-2026-1001",
  supplierId: "sup-1",
  supplierName: "Apple供应链",
  totalAmount: 550000,
  payableAmount: 550000,
  status: 5,
  createTime: "2026-10-01T10:00:00",
  updateTime: "2026-10-05T16:30:00",
};

const itemFixture: PurOrderItemDTO = {
  id: "poi-1",
  orderId: "po-1",
  orderNo: "PO-2026-1001",
  materialCode: "IP15P-128",
  materialName: "iPhone 15 Pro 128G",
  skuId: "sku-1",
  skuCode: "SKU-IP15P-128",
  quantity: 100,
  unitPrice: 5500,
  subtotal: 550000,
  receivedQuantity: 100,
};

describe("purchase mappers", () => {
  it("maps backend status codes onto UI statuses", () => {
    const order = toPurchaseOrder(orderFixture, [itemFixture]);

    expect(order.status).toBe("completed");
    expect(order.statusLabel).toBe("已完成");
    expect(order.items).toHaveLength(1);
    expect(purchaseStatusToApi("received")).toBe(4);
    expect(purchaseStatusToApi(undefined)).toBeUndefined();
  });

  it("derives the line total when subtotal is absent", () => {
    const item = toPurchaseOrderItem({ ...itemFixture, subtotal: null });

    expect(item.totalPrice).toBe(550000);
    expect(item.productName).toBe("iPhone 15 Pro 128G");
  });

  it("attaches items per order when building the page", () => {
    const page = toPurchaseOrderPage(
      { records: [orderFixture], total: 1, size: 20, current: 1 },
      { "po-1": [itemFixture] }
    );

    expect(page.records[0].items).toHaveLength(1);
    expect(page.total).toBe(1);
  });
});
