/** The backend derives exactly three levels from `availableStock` vs `safetyStock`. */
export type StockLevel = 'normal' | 'low' | 'out'

export interface InventoryItem {
  id: string
  skuId: string
  warehouseId: string
  locationCode?: string
  totalStock: number
  availableStock: number
  lockedStock: number
  damagedStock: number
  safetyStock: number
  maxStock?: number
  averageCost?: number
  status: StockLevel
  lastInboundAt?: string
  lastOutboundAt?: string
  updatedAt: string
}

/**
 * A warning row derived from the query API: `scm-inventory` exposes no alert endpoint
 * (`InvAlertController` is an empty stub), so low/out-of-stock rows are computed here.
 */
export interface StockAlert {
  id: string
  skuId: string
  warehouseId: string
  locationCode?: string
  currentStock: number
  threshold: number
  status: Extract<StockLevel, 'low' | 'out'>
}

export interface InventorySearchParams {
  current?: number
  pageSize?: number
  /** Exact SKU ids — the query API has no name/barcode keyword search. */
  skuIds?: string[]
  warehouseIds?: string[]
  locationCode?: string
  status?: StockLevel
  sortBy?: 'available_stock' | 'total_stock' | 'update_time'
  sortOrder?: 'ASC' | 'DESC'
}

export interface StockAdjustment {
  skuId: string
  warehouseId: string
  quantity: number
  adjustType: 1 | 2 | 6 | 7
  remark?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
