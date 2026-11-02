import dayjs from "dayjs";

import type {
  OrderDashboardCompare,
  OrderDashboardRange,
  OrderDashboardSummaryDTO,
  OrderResponse,
  OrderStatus,
} from "@/lib/api/contracts/order";
import type { ProdSkuDTO } from "@/lib/api/contracts/product";
import { inventoryApiService } from "@/lib/api/services/inventory";
import { orderApiService } from "@/lib/api/services/order";
import { productApiService } from "@/lib/api/services/product";
import { supplierApiService } from "@/lib/api/services/supplier";

import type {
  DashboardStats,
  InventoryAlert,
  KPIData,
  OrderStatusData,
  RecentOrder,
} from "../types";

const ALERT_PAGE_SIZE = 200;
const SKU_PAGE_SIZE = 1000;
const RECENT_ORDER_SIZE = 5;

const STATUS_LABELS: Record<OrderStatus, string> = {
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

const orderAmount = (order: OrderResponse): number =>
  order.payableAmount ?? order.totalAmount ?? 0;

const rangeLabelOf = (range: OrderDashboardRange): string => {
  if (range === "TODAY") return "今日";
  if (range === "LAST_30_DAYS") return "近 30 天";
  return "近 7 天";
};

const trendOf = (
  current: number,
  previous: number,
  period: string
): KPIData["trend"] => {
  // Growing from zero has no meaningful percentage; omit the badge instead of
  // showing a fake "+100%" or "0% up".
  if (previous === 0) return undefined;
  const percent = ((current - previous) / previous) * 100;
  return {
    value: Math.round(Math.abs(percent) * 10) / 10,
    direction: percent > 0.05 ? "up" : percent < -0.05 ? "down" : "flat",
    period,
  };
};

const buildOrderStatus = (
  summary: OrderDashboardSummaryDTO | null
): OrderStatusData[] =>
  (summary?.statusCounts ?? []).map((item) => ({
    status: item.status,
    label: STATUS_LABELS[item.status] ?? item.status,
    count: item.count,
  }));

const buildRecentOrders = (orders: OrderResponse[]): RecentOrder[] =>
  orders.slice(0, RECENT_ORDER_SIZE).map((order) => ({
    id: order.id,
    orderNo: order.orderNo,
    customerName: order.username || order.userId || "-",
    amount: orderAmount(order),
    status: order.status,
    statusLabel:
      order.statusName || STATUS_LABELS[order.status] || order.status,
    createdAt: order.createTime
      ? dayjs(order.createTime).format("YYYY-MM-DD HH:mm")
      : "-",
  }));

const buildInventoryAlerts = async (): Promise<InventoryAlert[]> => {
  const [low, out, skus] = await Promise.all([
    inventoryApiService.query({
      stockStatus: "LOW_STOCK",
      page: 1,
      size: ALERT_PAGE_SIZE,
    }),
    inventoryApiService.query({
      stockStatus: "OUT_OF_STOCK",
      page: 1,
      size: ALERT_PAGE_SIZE,
    }),
    productApiService.sku.list({ page: 1, size: SKU_PAGE_SIZE }),
  ]);

  const skuById = new Map<string, ProdSkuDTO>(
    (skus.records ?? []).map((sku) => [sku.id, sku])
  );

  const alerts = [...(low.records ?? []), ...(out.records ?? [])].map((item) => {
    const sku = skuById.get(item.skuId ?? "");
    const available = item.availableStock ?? 0;
    const safety = item.safetyStock ?? 0;
    const threshold = safety > 0 ? safety : Math.max(available, 1);
    const status: InventoryAlert["status"] =
      item.stockStatus === "OUT_OF_STOCK" || available === 0
        ? "out"
        : available <= threshold * 0.3
          ? "critical"
          : "low";

    return {
      id: item.id ?? `${item.skuId}-${item.warehouseId}`,
      productName: sku?.skuName ?? item.skuId ?? "-",
      skuCode: sku?.skuCode ?? item.skuId ?? "-",
      currentStock: available,
      threshold,
      status,
    };
  });

  // A row can appear in both status queries when its stock changes between
  // the two requests; the table needs one row per inventory record.
  return [...new Map(alerts.map((alert) => [alert.id, alert])).values()];
};

interface SourceResult<T> {
  data: T | null;
  issue?: string;
}

/**
 * A dashboard must not present a failed source as a real zero, so each source
 * is loaded independently and its failure is reported alongside the numbers.
 */
const loadSource = async <T>(
  label: string,
  load: () => Promise<T>
): Promise<SourceResult<T>> => {
  try {
    return { data: await load() };
  } catch {
    return { data: null, issue: label };
  }
};

const unavailableKpi = (
  title: string,
  icon: string,
  hint: string
): KPIData => ({ title, value: "—", icon, hint });

export const dashboardService = {
  getStats: async (
    range: OrderDashboardRange,
    compare: OrderDashboardCompare
  ): Promise<DashboardStats> => {
    const [
      summarySource,
      recentSource,
      inventorySource,
      supplierSource,
      alertSource,
    ] = await Promise.all([
      loadSource("订单聚合", () =>
        orderApiService.getDashboardSummary(range, compare)
      ),
      loadSource("最近订单", () =>
        orderApiService.queryOrders({
          pageNum: 1,
          pageSize: RECENT_ORDER_SIZE,
        })
      ),
      loadSource("库存统计", () => inventoryApiService.stats()),
      loadSource("供应商数据", () =>
        supplierApiService.list({ page: 1, size: 1 })
      ),
      loadSource("库存预警", buildInventoryAlerts),
    ]);

    const summary = summarySource.data;
    const inventoryStats = inventorySource.data;
    const suppliers = supplierSource.data;
    const inventoryAlerts = alertSource.data ?? [];

    const dataIssues = [
      summarySource.issue,
      recentSource.issue,
      inventorySource.issue,
      supplierSource.issue,
      alertSource.issue,
    ].filter((issue): issue is string => Boolean(issue));

    // Partial failures are surfaced next to the numbers; a dashboard with no
    // usable source at all should hit the retry state instead of rendering zeroes.
    if (dataIssues.length === 5) {
      throw new Error("仪表盘数据源全部不可用");
    }

    const rangeLabel = rangeLabelOf(range);
    const periodLabel =
      compare === "YEAR_OVER_YEAR"
        ? "较去年同期"
        : compare === "PREVIOUS_PERIOD"
          ? "较上期"
          : null;
    const trend = (current: number, previous: number) =>
      periodLabel ? trendOf(current, previous, periodLabel) : undefined;

    const orderKpis: KPIData[] = summary
      ? [
          {
            title: `${rangeLabel}营收`,
            prefix: "¥",
            value: Math.round(summary.currentRevenue).toLocaleString("zh-CN"),
            trend: trend(summary.currentRevenue, summary.comparisonRevenue),
            icon: "DollarOutlined",
            hint: "已付款及之后状态",
            sparkline: summary.points.map((point) => point.revenue),
          },
          {
            title: `${rangeLabel}订单`,
            suffix: "单",
            value: summary.currentOrders.toLocaleString("zh-CN"),
            trend: trend(summary.currentOrders, summary.comparisonOrders),
            icon: "ShoppingCartOutlined",
            hint: `累计 ${summary.totalOrders.toLocaleString("zh-CN")} 单`,
            sparkline: summary.points.map((point) => point.orders),
          },
        ]
      : [
          unavailableKpi(`${rangeLabel}营收`, "DollarOutlined", "订单数据不可用"),
          unavailableKpi(`${rangeLabel}订单`, "ShoppingCartOutlined", "订单数据不可用"),
        ];

    const stockRisk = inventoryAlerts.length;
    const alertsAvailable = !alertSource.issue;

    const kpis = [
      ...orderKpis,
      inventoryStats
        ? {
            title: "库存总量",
            suffix: "件",
            value:
              inventoryStats.totalStockQuantity?.toLocaleString("zh-CN") ?? "0",
            icon: "InboxOutlined",
            hint:
              stockRisk > 0
                ? `${stockRisk} 个 SKU 需关注`
                : alertsAvailable
                  ? "库存水位正常"
                  : "预警数据不可用",
          }
        : unavailableKpi("库存总量", "InboxOutlined", "数据不可用"),
      suppliers
        ? {
            title: "供应商",
            suffix: "家",
            value: suppliers.total.toLocaleString("zh-CN"),
            icon: "TeamOutlined",
            hint: "已建档供应商",
          }
        : unavailableKpi("供应商", "TeamOutlined", "数据不可用"),
    ];

    return {
      kpis,
      salesTrend: {
        granularity: summary?.granularity ?? "DAY",
        points: summary?.points ?? [],
      },
      orderStatus: buildOrderStatus(summary),
      recentOrders: buildRecentOrders(recentSource.data?.records ?? []),
      inventoryAlerts,
      alertsAvailable,
      attention: {
        pendingPayment: summary?.attention.pendingPayment ?? 0,
        pendingShip: summary?.attention.pendingShip ?? 0,
        refunding: summary?.attention.refunding ?? 0,
        lowStock: inventoryAlerts.filter(
          (alert) => alert.status === "low" || alert.status === "critical"
        ).length,
        outOfStock: inventoryAlerts.filter((alert) => alert.status === "out")
          .length,
      },
      dataIssues,
      generatedAt: summary?.generatedAt ?? new Date().toISOString(),
      range,
      compare,
      rangeLabel,
    };
  },
};
