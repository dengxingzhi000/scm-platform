import { describe, expect, it } from "vitest";

import type { InventoryPage, InventoryResponse } from "@/lib/api/contracts/inventory";
import { toInventoryItem, toInventoryPage, toStockAlerts, toStockStatus } from "./mappers";

const row: InventoryResponse = {
  id: "inv-1",
  skuId: "sku-1",
  warehouseId: "wh-1",
  totalStock: 50,
  availableStock: 45,
  lockedStock: 5,
  damagedStock: 0,
  safetyStock: 20,
  locationCode: "A-01",
  stockStatus: "NORMAL",
  updateTime: "2026-06-08T14:30:00",
};

describe("inventory mappers", () => {
  it("maps the server stock status onto the UI level", () => {
    expect(toInventoryItem(row).status).toBe("normal");
    expect(toStockStatus("low")).toBe("LOW_STOCK");
    expect(toStockStatus(undefined)).toBeUndefined();
  });

  it("recomputes the level locally when the server leaves stockStatus empty", () => {
    expect(toInventoryItem({ ...row, stockStatus: null, availableStock: 5 }).status).toBe("low");
    expect(toInventoryItem({ ...row, stockStatus: null, availableStock: 0 }).status).toBe("out");
    expect(toInventoryItem({ ...row, stockStatus: null, availableStock: 21 }).status).toBe("normal");
  });

  it("translates MyBatis-Plus page echo into UI paging", () => {
    const page: InventoryPage = { records: [row], total: 7, size: 20, current: 1 };

    expect(toInventoryPage(page)).toMatchObject({ total: 7, current: 1, pageSize: 20 });
  });

  it("derives warnings from low and out-of-stock rows only", () => {
    const items = [
      toInventoryItem(row),
      toInventoryItem({ ...row, id: "inv-2", skuId: "sku-2", availableStock: 3, stockStatus: "LOW_STOCK" }),
      toInventoryItem({ ...row, id: "inv-3", skuId: "sku-3", availableStock: 0, stockStatus: "OUT_OF_STOCK" }),
    ];

    const alerts = toStockAlerts(items);

    expect(alerts.map((a) => a.skuId)).toEqual(["sku-2", "sku-3"]);
    expect(alerts[1]).toMatchObject({ status: "out", currentStock: 0, threshold: 20 });
  });
});
