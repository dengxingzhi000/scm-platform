import dayjs from "dayjs";

import type {
  InventoryPage,
  InventoryResponse,
  StockStatus,
} from "@/lib/api/contracts/inventory";
import type { InventoryItem, PageResult, StockAlert, StockLevel } from "../types";

const LEVEL_BY_STATUS: Record<StockStatus, StockLevel> = {
  NORMAL: "normal",
  LOW_STOCK: "low",
  OUT_OF_STOCK: "out",
};

const STATUS_BY_LEVEL: Record<StockLevel, StockStatus> = {
  normal: "NORMAL",
  low: "LOW_STOCK",
  out: "OUT_OF_STOCK",
};

const num = (value?: number | null): number => value ?? 0;

export const formatDateTime = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";

export const toStockStatus = (level?: StockLevel): StockStatus | undefined =>
  level ? STATUS_BY_LEVEL[level] : undefined;

export const toInventoryItem = (dto: InventoryResponse): InventoryItem => ({
  id: dto.id ?? `${dto.skuId}-${dto.warehouseId}`,
  skuId: dto.skuId ?? "-",
  warehouseId: dto.warehouseId ?? "-",
  locationCode: dto.locationCode ?? undefined,
  totalStock: num(dto.totalStock),
  availableStock: num(dto.availableStock),
  lockedStock: num(dto.lockedStock),
  damagedStock: num(dto.damagedStock),
  safetyStock: num(dto.safetyStock),
  maxStock: dto.maxStock ?? undefined,
  averageCost: dto.averageCost ?? undefined,
  // Fall back to the local rule when the server leaves the derived field empty.
  status:
    (dto.stockStatus && LEVEL_BY_STATUS[dto.stockStatus]) ||
    (num(dto.availableStock) === 0
      ? "out"
      : num(dto.availableStock) <= num(dto.safetyStock)
        ? "low"
        : "normal"),
  lastInboundAt: dto.lastInboundAt ?? undefined,
  lastOutboundAt: dto.lastOutboundAt ?? undefined,
  updatedAt: formatDateTime(dto.updateTime ?? dto.createTime),
});

export const toInventoryPage = (
  page: InventoryPage | null | undefined
): PageResult<InventoryItem> => ({
  records: (page?.records ?? []).map(toInventoryItem),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});

export const toStockAlerts = (items: InventoryItem[]): StockAlert[] =>
  items
    .filter((item): item is InventoryItem & { status: "low" | "out" } =>
      item.status !== "normal")
    .map((item) => ({
      id: item.id,
      skuId: item.skuId,
      warehouseId: item.warehouseId,
      locationCode: item.locationCode,
      currentStock: item.availableStock,
      threshold: item.safetyStock,
      status: item.status,
    }));
