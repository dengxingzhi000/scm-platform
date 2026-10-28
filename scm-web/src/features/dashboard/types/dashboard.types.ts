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
  /** Mini trend series rendered with Recharts inside the KPI card. */
  sparkline?: number[]
}

export interface SalesTrendData {
  dates: string[]
  series: Array<{
    name: string
    data: number[]
  }>
}

export interface OrderStatusData {
  status: string
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

export interface DashboardStats {
  kpis: KPIData[]
  salesTrend: SalesTrendData
  orderStatus: OrderStatusData[]
  recentOrders: RecentOrder[]
  inventoryAlerts: InventoryAlert[]
}
