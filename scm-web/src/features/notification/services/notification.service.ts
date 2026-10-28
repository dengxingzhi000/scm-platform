import type { Notification, NotificationSearchParams } from '../types'

const typeMap: Record<string, { label: string; color: string }> = {
  system: { label: '系统', color: 'blue' },
  order: { label: '订单', color: 'green' },
  inventory: { label: '库存', color: 'orange' },
  approval: { label: '审批', color: 'purple' },
  security: { label: '安全', color: 'red' },
}

const mockNotifications: Notification[] = [
  { id: '1', title: '订单已发货', content: '订单 ORD-2026-002 已发货，快递单号 JD0987654321', type: 'order', typeLabel: '订单', isRead: false, link: '/order/2', createdAt: '2026-06-08 14:30' },
  { id: '2', title: '库存预警', content: 'AirPods Pro 2 库存已不足，请及时补货', type: 'inventory', typeLabel: '库存', isRead: false, link: '/inventory/alerts', createdAt: '2026-06-08 13:00' },
  { id: '3', title: '系统维护通知', content: '系统将于今晚 22:00-23:00 进行维护升级', type: 'system', typeLabel: '系统', isRead: true, createdAt: '2026-06-08 10:00' },
  { id: '4', title: '采购单审批通过', content: '采购单 PO-2026-003 已通过审批', type: 'approval', typeLabel: '审批', isRead: true, link: '/purchase/3', createdAt: '2026-06-07 16:00' },
  { id: '5', title: '登录异常', content: '您的账号在异地登录，如非本人操作请及时修改密码', type: 'security', typeLabel: '安全', isRead: false, createdAt: '2026-06-07 09:00' },
  { id: '6', title: '退款申请待审核', content: '订单 ORD-2026-004 发起退款申请，金额 ¥2,200', type: 'approval', typeLabel: '审批', isRead: false, link: '/order/refund', createdAt: '2026-06-08 15:20' },
  { id: '7', title: '库存同步完成', content: '主仓库库存数据已同步至缓存，共更新 1,284 个 SKU', type: 'system', typeLabel: '系统', isRead: true, createdAt: '2026-06-07 20:00' },
  { id: '8', title: '支付成功', content: '订单 ORD-2026-005 支付成功，实付 ¥450', type: 'order', typeLabel: '订单', isRead: true, link: '/order/payment', createdAt: '2026-06-07 18:45' },
  { id: '9', title: '安全策略更新', content: '平台已启用新的登录风控策略，请关注异常登录提醒', type: 'security', typeLabel: '安全', isRead: true, createdAt: '2026-06-06 11:00' },
]

export const notificationService = {
  list: async (params?: NotificationSearchParams) => {
    let filtered = [...mockNotifications]
    if (params?.type) filtered = filtered.filter(n => n.type === params.type)
    if (params?.isRead !== undefined) filtered = filtered.filter(n => n.isRead === params.isRead)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getUnreadCount: async (): Promise<number> => mockNotifications.filter(n => !n.isRead).length,
  markAsRead: async (id: string): Promise<void> => {},
  markAllAsRead: async (): Promise<void> => {},
  getTypeMap: () => typeMap,
}
