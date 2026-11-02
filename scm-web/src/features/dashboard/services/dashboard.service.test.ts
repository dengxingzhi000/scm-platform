import { beforeEach, describe, expect, it, vi } from "vitest";

import type { OrderDashboardSummaryDTO } from "@/lib/api/contracts/order";
import { inventoryApiService } from "@/lib/api/services/inventory";
import { orderApiService } from "@/lib/api/services/order";
import { productApiService } from "@/lib/api/services/product";
import { supplierApiService } from "@/lib/api/services/supplier";

import { dashboardService } from "./dashboard.service";

vi.mock("@/lib/api/services/order", () => ({
  orderApiService: { getDashboardSummary: vi.fn(), queryOrders: vi.fn() },
}));
vi.mock("@/lib/api/services/inventory", () => ({
  inventoryApiService: { query: vi.fn(), stats: vi.fn() },
}));
vi.mock("@/lib/api/services/supplier", () => ({
  supplierApiService: { list: vi.fn() },
}));
vi.mock("@/lib/api/services/product", () => ({
  productApiService: { sku: { list: vi.fn() } },
}));

const summary = (): OrderDashboardSummaryDTO => ({
  range: "LAST_7_DAYS",
  compareMode: "PREVIOUS_PERIOD",
  granularity: "DAY",
  rangeStart: "2026-10-24T00:00:00",
  rangeEnd: "2026-10-31T00:00:00",
  generatedAt: "2026-10-30T14:40:09",
  currentRevenue: 79586,
  comparisonRevenue: 39894,
  currentOrders: 15,
  comparisonOrders: 13,
  totalOrders: 32,
  points: [
    { startTime: "2026-10-27T00:00:00", endTime: "2026-10-28T00:00:00", revenue: 23997, orders: 4 },
  ],
  statusCounts: [{ status: "PENDING_PAYMENT", count: 3 }],
  attention: { pendingPayment: 4, pendingShip: 2, refunding: 2 },
});

const stockKpiHint = (kpis: { title: string; hint?: string }[]): string =>
  kpis.find((kpi) => kpi.title === "库存总量")?.hint ?? "";

const mpPage = <T>(records: T[]) => ({
  records,
  total: records.length,
  size: 200,
  current: 1,
});

const skuPage = <T>(records: T[]) => ({
  records,
  total: records.length,
  pageNum: 1,
  pageSize: 1000,
});

beforeEach(() => {
  vi.mocked(orderApiService.getDashboardSummary).mockReset().mockResolvedValue(summary());
  vi.mocked(orderApiService.queryOrders).mockReset().mockResolvedValue(mpPage([]));
  vi.mocked(inventoryApiService.stats).mockReset().mockResolvedValue({ totalStockQuantity: 321 });
  vi.mocked(supplierApiService.list).mockReset().mockResolvedValue(mpPage([]));
  vi.mocked(productApiService.sku.list).mockReset().mockResolvedValue(skuPage([]));
  vi.mocked(inventoryApiService.query).mockReset().mockResolvedValue(mpPage([]));
});

describe("dashboardService.getStats 的数据源降级", () => {
  it("预警源可用且无异常时给出库存水位结论", async () => {
    const stats = await dashboardService.getStats("LAST_7_DAYS", "PREVIOUS_PERIOD");

    expect(stats.alertsAvailable).toBe(true);
    expect(stats.dataIssues).toEqual([]);
    expect(stockKpiHint(stats.kpis)).toBe("库存水位正常");
  });

  it("预警源失败时不把空列表当成库存健康", async () => {
    vi.mocked(inventoryApiService.query).mockRejectedValue(new Error("503"));

    const stats = await dashboardService.getStats("LAST_7_DAYS", "PREVIOUS_PERIOD");

    expect(stats.inventoryAlerts).toEqual([]);
    expect(stats.alertsAvailable).toBe(false);
    expect(stats.dataIssues).toContain("库存预警");
    expect(stockKpiHint(stats.kpis)).toBe("预警数据不可用");
  });

  it("有预警时把数量回写到库存卡片副标题", async () => {
    vi.mocked(inventoryApiService.query).mockResolvedValue(
      mpPage([
        { id: "a1", skuId: "s1", warehouseId: "w1", availableStock: 1, safetyStock: 50, stockStatus: "LOW_STOCK" },
        { id: "a2", skuId: "s2", warehouseId: "w1", availableStock: 0, safetyStock: 10, stockStatus: "OUT_OF_STOCK" },
      ])
    );

    const stats = await dashboardService.getStats("LAST_7_DAYS", "PREVIOUS_PERIOD");

    expect(stats.alertsAvailable).toBe(true);
    expect(stockKpiHint(stats.kpis)).toBe("2 个 SKU 需关注");
    expect(stats.attention).toMatchObject({ lowStock: 1, outOfStock: 1 });
  });

  it("五个数据源全部失败时抛出，交给页面重试态", async () => {
    vi.mocked(orderApiService.getDashboardSummary).mockRejectedValue(new Error("500"));
    vi.mocked(orderApiService.queryOrders).mockRejectedValue(new Error("500"));
    vi.mocked(inventoryApiService.stats).mockRejectedValue(new Error("500"));
    vi.mocked(supplierApiService.list).mockRejectedValue(new Error("500"));
    vi.mocked(inventoryApiService.query).mockRejectedValue(new Error("500"));

    await expect(
      dashboardService.getStats("LAST_7_DAYS", "PREVIOUS_PERIOD")
    ).rejects.toThrow("仪表盘数据源全部不可用");
  });
});
