import type {
  OrderDashboardCompare,
  OrderDashboardRange,
} from "@/lib/api/contracts/order";

export interface KPIData {
  title: string
  value: number | string
  prefix?: string
  suffix?: string
  trend?: {
    value: number
    direction: 'up' | 'down' | 'flat'
    period: string
  }
  icon: string
  /** Secondary context shown next to the trend, e.g. the all-time total. */
  hint?: string
  /** Mini trend series rendered with Recharts inside the KPI card. */
  sparkline?: number[]
}

export interface SalesTrendPoint {
  startTime: string
  endTime: string
  revenue: number
  orders: number
}

export interface SalesTrendData {
  granularity: 'HOUR' | 'DAY'
  points: SalesTrendPoint[]
}

export interface OrderStatusData {
  /** Backend status code, e.g. `PENDING_PAYMENT`. */
  status: string
  label: string
  count: number
}

export interface RecentOrder {
  id: string
  orderNo: string
  customerName: string
  amount: number
  status: string
  statusLabel: string
  createdAt: string
}

export interface InventoryAlert {
  id: string
  productName: string
  skuCode: string
  currentStock: number
  threshold: number
  status: 'low' | 'critical' | 'out'
}

export interface DashboardAttention {
  pendingPayment: number
  pendingShip: number
  refunding: number
  lowStock: number
  outOfStock: number
}

export interface DashboardStats {
  kpis: KPIData[]
  salesTrend: SalesTrendData
  orderStatus: OrderStatusData[]
  recentOrders: RecentOrder[]
  inventoryAlerts: InventoryAlert[]
  /** False when the alert source failed; an empty list must not read as "all stock is healthy". */
  alertsAvailable: boolean
  /** Exception counters used by the action bar above the KPI row. */
  attention: DashboardAttention
  /** Sources that failed while aggregating; surfaced so zeroes are not mistaken for real values. */
  dataIssues: string[]
  generatedAt: string
  range: OrderDashboardRange
  compare: OrderDashboardCompare
  /** “今日” / “近 7 天” / “近 30 天”, reused by KPI titles and card copy. */
  rangeLabel: string
}
