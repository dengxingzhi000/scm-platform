import type { Settlement, Invoice } from '../types'

const mockSettlements: Settlement[] = [
  { id: '1', settlementNo: 'STL-2026-001', supplierName: 'Apple供应链', amount: 550000, status: 'completed', statusLabel: '已完成', period: '2026-05', createdAt: '2026-06-01' },
  { id: '2', settlementNo: 'STL-2026-002', supplierName: 'Samsung代理', amount: 480000, status: 'processing', statusLabel: '处理中', period: '2026-06', createdAt: '2026-06-08' },
  { id: '3', settlementNo: 'STL-2026-003', supplierName: '深圳华强电子', amount: 320000, status: 'pending', statusLabel: '待结算', period: '2026-06', createdAt: '2026-06-10' },
  { id: '4', settlementNo: 'STL-2026-004', supplierName: '上海联创供应链', amount: 275000, status: 'processing', statusLabel: '处理中', period: '2026-06', createdAt: '2026-06-11' },
  { id: '5', settlementNo: 'STL-2026-005', supplierName: '杭州锐迈科技', amount: 98000, status: 'rejected', statusLabel: '已驳回', period: '2026-05', createdAt: '2026-06-04' },
  { id: '6', settlementNo: 'STL-2026-006', supplierName: '广州捷普物流', amount: 156000, status: 'completed', statusLabel: '已完成', period: '2026-04', createdAt: '2026-05-18' },
]

const mockInvoices: Invoice[] = [
  { id: '1', invoiceNo: 'INV-2026-001', settlementNo: 'STL-2026-001', amount: 550000, taxAmount: 71500, status: 'received', statusLabel: '已收票', createdAt: '2026-06-05' },
  { id: '2', invoiceNo: 'INV-2026-002', settlementNo: 'STL-2026-002', amount: 480000, taxAmount: 62400, status: 'pending', statusLabel: '待开票', createdAt: '2026-06-08' },
  { id: '3', invoiceNo: 'INV-2026-003', settlementNo: 'STL-2026-003', amount: 320000, taxAmount: 41600, status: 'issued', statusLabel: '已开票', createdAt: '2026-06-10' },
  { id: '4', invoiceNo: 'INV-2026-004', settlementNo: 'STL-2026-004', amount: 275000, taxAmount: 35750, status: 'sent', statusLabel: '已寄送', createdAt: '2026-06-11' },
  { id: '5', invoiceNo: 'INV-2026-005', settlementNo: 'STL-2026-005', amount: 98000, taxAmount: 12740, status: 'pending', statusLabel: '待开票', createdAt: '2026-06-04' },
  { id: '6', invoiceNo: 'INV-2026-006', settlementNo: 'STL-2026-006', amount: 156000, taxAmount: 20280, status: 'received', statusLabel: '已收票', createdAt: '2026-05-20' },
]

export const financeService = {
  listSettlements: async () => mockSettlements,
  listInvoices: async () => mockInvoices,
}
