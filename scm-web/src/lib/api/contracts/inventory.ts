import type { MpPage } from "./page";

/** Derived server-side from `availableStock` vs `safetyStock`; never stored. */
export type StockStatus = "NORMAL" | "LOW_STOCK" | "OUT_OF_STOCK";

/** `adjustType`: only 1 (inbound) and 2 (outbound) stamp timestamps; 6/7 exist as codes only. */
export type AdjustType = 1 | 2 | 6 | 7;

export interface InventoryResponse {
  id?: string | null;
  skuId?: string | null;
  warehouseId?: string | null;
  totalStock?: number | null;
  availableStock?: number | null;
  lockedStock?: number | null;
  damagedStock?: number | null;
  safetyStock?: number | null;
  maxStock?: number | null;
  locationCode?: string | null;
  averageCost?: number | null;
  stockStatus?: StockStatus | null;
  version?: number | null;
  lastInboundAt?: string | null;
  lastOutboundAt?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

/** `sortBy` takes snake_case column names, whitelisted to these three. */
export interface InventoryQueryRequest {
  skuIds?: string[];
  warehouseIds?: string[];
  locationCode?: string;
  stockStatus?: StockStatus;
  minAvailableStock?: number;
  maxAvailableStock?: number;
  onlyInStock?: boolean;
  page?: number;
  size?: number;
  sortBy?: "available_stock" | "total_stock" | "update_time";
  sortOrder?: "ASC" | "DESC";
}

export interface InventoryAdjustRequest {
  skuId: string;
  warehouseId: string;
  /** Signed: positive increases stock, negative decreases it. */
  quantity: number;
  adjustType: AdjustType;
  /** Doubles as the 24h idempotency key; omit it and retries are not protected. */
  referenceNo?: string;
  referenceId?: string;
  operatorId?: string;
  operatorName?: string;
  remark?: string;
}

export interface InventoryTransferRequest {
  skuId: string;
  fromWarehouseId: string;
  toWarehouseId: string;
  quantity: number;
  transferNo?: string;
  operatorId?: string;
  operatorName?: string;
  remark?: string;
}

export interface InventoryReservationRequest {
  skuId: string;
  warehouseId: string;
  quantity: number;
  businessKey: string;
  /** Redis key TTL; defaults to 900s server-side. */
  timeoutSeconds?: number;
  operatorId?: string;
  operatorName?: string;
  remark?: string;
}

export interface InventoryStatsResponse {
  totalSkuCount?: number;
  totalWarehouseCount?: number;
  totalStockQuantity?: number;
  availableStockQuantity?: number;
  lockedStockQuantity?: number;
  damagedStockQuantity?: number;
  totalStockValue?: number;
  outOfStockCount?: number;
  lowStockCount?: number;
  normalStockCount?: number;
}

export type InventoryPage = MpPage<InventoryResponse>;
