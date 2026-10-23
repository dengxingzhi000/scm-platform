import type { DashboardStats, KPIData } from '../types'

const mockKPIs: KPIData[] = [
  { title: '总营收', value: '¥128,500', trend: { value: 12.5, direction: 'up', period: '较上月' }, icon: 'DollarOutlined', color: '#1677ff', sparkline: [42, 48, 45, 52, 49, 58, 61, 60, 68, 72] },
  { title: '订单数', value: '1,234', trend: { value: 8.3, direction: 'up', period: '较上月' }, icon: 'ShoppingCartOutlined', color: '#52c41a', sparkline: [31, 35, 34, 38, 41, 39, 44, 47, 46, 51] },
  { title: '库存量', value: '5,678', trend: { value: 2.1, direction: 'down', period: '较上月' }, icon: 'InboxOutlined', color: '#faad14', sparkline: [70, 68, 69, 66, 64, 63, 60, 61, 58, 56] },
  { title: '供应商', value: '89', trend: { value: 5, direction: 'up', period: '较上月' }, icon: 'TeamOutlined', color: '#722ed1', sparkline: [20, 22, 21, 24, 25, 24, 26, 28, 27, 30] },
]

export const dashboardService = {
  getStats: async (): Promise<DashboardStats> => {
    return {
      kpis: mockKPIs,
      salesTrend: {
        dates: ['06-01', '06-02', '06-03', '06-04', '06-05', '06-06', '06-07'],
        series: [
          { name: '销售额', data: [1200, 1500, 1800, 1400, 2100, 1900, 2300], color: '#1677ff' },
          { name: '订单量', data: [80, 95, 120, 88, 140, 125, 155], color: '#52c41a' },
        ],
      },
      orderStatus: [
        { status: '待付款', count: 45, color: '#faad14' },
        { status: '已付款', count: 120, color: '#1677ff' },
        { status: '已发货', count: 85, color: '#722ed1' },
        { status: '已完成', count: 200, color: '#52c41a' },
        { status: '已取消', count: 15, color: '#ff4d4f' },
      ],
      recentOrders: [
        { id: '1', orderNo: 'ORD-2026-001', customerName: '张三', amount: 1280, status: 'PAID', statusLabel: '已付款', createdAt: '2026-06-08 14:30' },
        { id: '2', orderNo: 'ORD-2026-002', customerName: '李四', amount: 2560, status: 'PENDING', statusLabel: '待付款', createdAt: '2026-06-08 13:15' },
        { id: '3', orderNo: 'ORD-2026-003', customerName: '王五', amount: 890, status: 'SHIPPED', statusLabel: '已发货', createdAt: '2026-06-08 11:20' },
        { id: '4', orderNo: 'ORD-2026-004', customerName: '赵六', amount: 3200, status: 'COMPLETED', statusLabel: '已完成', createdAt: '2026-06-08 10:05' },
        { id: '5', orderNo: 'ORD-2026-005', customerName: '钱七', amount: 450, status: 'CANCELLED', statusLabel: '已取消', createdAt: '2026-06-08 09:30' },
      ],
      inventoryAlerts: [
        { id: '1', productName: 'iPhone 15 Pro', skuCode: 'SKU-001', currentStock: 5, threshold: 20, status: 'critical' },
        { id: '2', productName: 'MacBook Air M3', skuCode: 'SKU-002', currentStock: 12, threshold: 30, status: 'low' },
        { id: '3', productName: 'AirPods Pro 2', skuCode: 'SKU-003', currentStock: 0, threshold: 50, status: 'out' },
        { id: '4', productName: 'iPad Air', skuCode: 'SKU-004', currentStock: 8, threshold: 25, status: 'low' },
      ],
    }
  },
}
