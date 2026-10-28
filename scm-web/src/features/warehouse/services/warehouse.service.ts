import type { Warehouse, InboundOrder, OutboundOrder } from '../types'

const mockWarehouses: Warehouse[] = [
  { id: '1', name: '主仓库', code: 'WH-001', address: '上海市浦东新区', contactPerson: '张仓管', contactPhone: '13800000001', status: 'active', createdAt: '2025-01-01' },
  { id: '2', name: '华东仓', code: 'WH-002', address: '杭州市余杭区', contactPerson: '李仓管', contactPhone: '13800000002', status: 'active', createdAt: '2025-02-01' },
  { id: '3', name: '华南仓', code: 'WH-003', address: '广州市黄埔区', contactPerson: '王仓管', contactPhone: '13800000003', status: 'active', createdAt: '2025-03-12' },
  { id: '4', name: '华北仓', code: 'WH-004', address: '北京市大兴区', contactPerson: '赵仓管', contactPhone: '13800000004', status: 'active', createdAt: '2025-04-08' },
  { id: '5', name: '西南仓', code: 'WH-005', address: '成都市双流区', contactPerson: '周仓管', contactPhone: '13800000005', status: 'disabled', createdAt: '2025-05-20' },
]

const mockInbound: InboundOrder[] = [
  { id: '1', inboundNo: 'IN-2026-001', warehouseName: '主仓库', supplierName: 'Apple供应链', items: [
    { id: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', expectedQuantity: 100, actualQuantity: 100 },
  ], status: 'completed', statusLabel: '已完成', createdAt: '2026-06-01' },
  { id: '2', inboundNo: 'IN-2026-002', warehouseName: '主仓库', supplierName: 'Apple供应链', items: [
    { id: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', expectedQuantity: 50, actualQuantity: 30 },
  ], status: 'receiving', statusLabel: '收货中', createdAt: '2026-06-08' },
  { id: '3', inboundNo: 'IN-2026-003', warehouseName: '华东仓', supplierName: 'Samsung代理', items: [
    { id: '31', skuCode: 'SKU-004', productName: 'Galaxy S24 Ultra', expectedQuantity: 80, actualQuantity: 0 },
  ], status: 'pending', statusLabel: '待收货', createdAt: '2026-06-10' },
  { id: '4', inboundNo: 'IN-2026-004', warehouseName: '华南仓', supplierName: '深圳华强电子', items: [
    { id: '41', skuCode: 'SKU-003', productName: 'AirPods Pro 2', expectedQuantity: 200, actualQuantity: 180 },
  ], status: 'receiving', statusLabel: '收货中', createdAt: '2026-06-11' },
  { id: '5', inboundNo: 'IN-2026-005', warehouseName: '华北仓', supplierName: '上海联创供应链', items: [
    { id: '51', skuCode: 'SKU-005', productName: '小米 14 Ultra', expectedQuantity: 120, actualQuantity: 120 },
  ], status: 'completed', statusLabel: '已完成', createdAt: '2026-06-09' },
  { id: '6', inboundNo: 'IN-2026-006', warehouseName: '主仓库', supplierName: '苏州精工制造', items: [
    { id: '61', skuCode: 'SKU-006', productName: 'ThinkPad X1 Carbon', expectedQuantity: 40, actualQuantity: 0 },
  ], status: 'cancelled', statusLabel: '已取消', createdAt: '2026-06-07' },
]

const mockOutbound: OutboundOrder[] = [
  { id: '1', outboundNo: 'OUT-2026-001', warehouseName: '主仓库', items: [
    { id: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 2 },
  ], status: 'completed', statusLabel: '已完成', createdAt: '2026-06-05' },
  { id: '2', outboundNo: 'OUT-2026-002', warehouseName: '主仓库', items: [
    { id: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 1 },
  ], status: 'picking', statusLabel: '拣货中', createdAt: '2026-06-08' },
  { id: '3', outboundNo: 'OUT-2026-003', warehouseName: '华东仓', items: [
    { id: '31', skuCode: 'SKU-004', productName: 'Galaxy S24 Ultra', quantity: 4 },
  ], status: 'shipping', statusLabel: '发运中', createdAt: '2026-06-09' },
  { id: '4', outboundNo: 'OUT-2026-004', warehouseName: '华南仓', items: [
    { id: '41', skuCode: 'SKU-003', productName: 'AirPods Pro 2', quantity: 10 },
  ], status: 'picking', statusLabel: '拣货中', createdAt: '2026-06-10' },
  { id: '5', outboundNo: 'OUT-2026-005', warehouseName: '华北仓', items: [
    { id: '51', skuCode: 'SKU-007', productName: 'iPad Air 11', quantity: 3 },
  ], status: 'pending', statusLabel: '待出库', createdAt: '2026-06-11' },
  { id: '6', outboundNo: 'OUT-2026-006', warehouseName: '主仓库', items: [
    { id: '61', skuCode: 'SKU-008', productName: '华为 MatePad Pro', quantity: 5 },
  ], status: 'cancelled', statusLabel: '已取消', createdAt: '2026-06-06' },
]

export const warehouseService = {
  listWarehouses: async () => mockWarehouses,
  listInbound: async () => mockInbound,
  listOutbound: async () => mockOutbound,
}
