import type { PurchaseOrder, PurchaseSearchParams } from '../types'

const statusMap: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  pending: { label: '待审批', color: 'warning' },
  approved: { label: '已审批', color: 'processing' },
  ordered: { label: '已下单', color: 'blue' },
  received: { label: '已收货', color: 'purple' },
  completed: { label: '已完成', color: 'success' },
  cancelled: { label: '已取消', color: 'error' },
}

const mockPurchaseOrders: PurchaseOrder[] = [
  { id: '1', purchaseNo: 'PO-2026-001', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '11', purchaseOrderId: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 100, unitPrice: 5500, totalPrice: 550000, receivedQuantity: 100 },
  ], totalAmount: 550000, status: 'completed', statusLabel: '已完成', createdAt: '2026-05-15', updatedAt: '2026-06-01' },
  { id: '2', purchaseNo: 'PO-2026-002', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '21', purchaseOrderId: '2', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 50, unitPrice: 7000, totalPrice: 350000, receivedQuantity: 30 },
  ], totalAmount: 350000, status: 'received', statusLabel: '已收货', createdAt: '2026-06-01', updatedAt: '2026-06-08' },
  { id: '3', purchaseNo: 'PO-2026-003', supplierId: '2', supplierName: 'Samsung代理', items: [
    { id: '31', purchaseOrderId: '3', skuId: '41', skuCode: 'SKU-004', productName: 'Galaxy S24 Ultra', quantity: 80, unitPrice: 6000, totalPrice: 480000, receivedQuantity: 0 },
  ], totalAmount: 480000, status: 'approved', statusLabel: '已审批', createdAt: '2026-06-05', updatedAt: '2026-06-07' },
  { id: '4', purchaseNo: 'PO-2026-004', supplierId: '4', supplierName: '深圳华强电子', items: [
    { id: '41', purchaseOrderId: '4', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', quantity: 200, unitPrice: 1800, totalPrice: 360000, receivedQuantity: 0 },
  ], totalAmount: 360000, status: 'pending', statusLabel: '待审批', createdAt: '2026-06-09', updatedAt: '2026-06-09' },
  { id: '5', purchaseNo: 'PO-2026-005', supplierId: '5', supplierName: '上海联创供应链', items: [
    { id: '51', purchaseOrderId: '5', skuId: '51', skuCode: 'SKU-005', productName: '小米 14 Ultra', quantity: 120, unitPrice: 6200, totalPrice: 744000, receivedQuantity: 60 },
  ], totalAmount: 744000, status: 'ordered', statusLabel: '已下单', createdAt: '2026-06-08', updatedAt: '2026-06-10' },
  { id: '6', purchaseNo: 'PO-2026-006', supplierId: '8', supplierName: '苏州精工制造', items: [
    { id: '61', purchaseOrderId: '6', skuId: '61', skuCode: 'SKU-006', productName: 'ThinkPad X1 Carbon', quantity: 40, unitPrice: 9000, totalPrice: 360000, receivedQuantity: 0 },
  ], totalAmount: 360000, status: 'draft', statusLabel: '草稿', createdAt: '2026-06-11', updatedAt: '2026-06-11' },
  { id: '7', purchaseNo: 'PO-2026-007', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '71', purchaseOrderId: '7', skuId: '71', skuCode: 'SKU-007', productName: 'iPhone 15 128G', quantity: 60, unitPrice: 5200, totalPrice: 312000, receivedQuantity: 0 },
  ], totalAmount: 312000, status: 'cancelled', statusLabel: '已取消', createdAt: '2026-06-03', updatedAt: '2026-06-04' },
  { id: '8', purchaseNo: 'PO-2026-008', supplierId: '6', supplierName: '广州捷普物流', items: [
    { id: '81', purchaseOrderId: '8', skuId: '81', skuCode: 'SKU-008', productName: '包装耗材套装', quantity: 500, unitPrice: 60, totalPrice: 30000, receivedQuantity: 500 },
  ], totalAmount: 30000, status: 'completed', statusLabel: '已完成', createdAt: '2026-05-28', updatedAt: '2026-06-02' },
]

export const purchaseService = {
  list: async (params?: PurchaseSearchParams) => {
    let filtered = [...mockPurchaseOrders]
    if (params?.keyword) filtered = filtered.filter(o => o.purchaseNo.includes(params.keyword!) || o.supplierName.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(o => o.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<PurchaseOrder> => {
    const o = mockPurchaseOrders.find(x => x.id === id)
    if (!o) throw new Error('PurchaseOrder not found')
    return o
  },
  getStatusMap: () => statusMap,
}
