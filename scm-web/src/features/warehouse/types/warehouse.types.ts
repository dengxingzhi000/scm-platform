export interface Warehouse {
  id: string
  name: string
  code: string
  address: string
  contactPerson: string
  contactPhone: string
  status: 'active' | 'disabled'
  createdAt: string
}

export interface InboundOrder {
  id: string
  inboundNo: string
  warehouseName: string
  supplierName: string
  items: InboundItem[]
  status: 'pending' | 'receiving' | 'completed' | 'cancelled'
  statusLabel: string
  createdAt: string
}

export interface InboundItem {
  id: string
  skuCode: string
  productName: string
  expectedQuantity: number
  actualQuantity: number
}

export interface OutboundOrder {
  id: string
  outboundNo: string
  warehouseName: string
  items: OutboundItem[]
  status: 'pending' | 'picking' | 'shipping' | 'completed' | 'cancelled'
  statusLabel: string
  createdAt: string
}

export interface OutboundItem {
  id: string
  skuCode: string
  productName: string
  quantity: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
