import type { MpPage } from "./page";

/** `com.cognora.scm.warehouse.domain.entity.WmsWarehouse`. */
export interface WmsWarehouseDTO {
  id: string;
  warehouseCode?: string | null;
  warehouseName?: string | null;
  warehouseType?: number | null;
  province?: string | null;
  city?: string | null;
  district?: string | null;
  address?: string | null;
  managerName?: string | null;
  phone?: string | null;
  email?: string | null;
  enabled?: boolean | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

/** `com.cognora.scm.warehouse.domain.entity.WmsInbound`. */
export interface WmsInboundDTO {
  id: string;
  inboundNo?: string | null;
  warehouseId?: string | null;
  inboundType?: number | null;
  sourceType?: string | null;
  sourceId?: string | null;
  sourceNo?: string | null;
  supplierId?: string | null;
  status?: number | null;
  totalQuantity?: number | null;
  receivedQuantity?: number | null;
  expectedAt?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  operatorName?: string | null;
  createTime?: string | null;
  remark?: string | null;
}

/** `com.cognora.scm.warehouse.domain.entity.WmsInboundItem`. */
export interface WmsInboundItemDTO {
  id: string;
  inboundId?: string | null;
  inboundNo?: string | null;
  skuId?: string | null;
  skuCode?: string | null;
  skuName?: string | null;
  planQuantity?: number | null;
  actualQuantity?: number | null;
  locationCode?: string | null;
  qualityStatus?: number | null;
}

/** `com.cognora.scm.warehouse.domain.entity.WmsOutbound`. */
export interface WmsOutboundDTO {
  id: string;
  outboundNo?: string | null;
  warehouseId?: string | null;
  outboundType?: number | null;
  sourceType?: string | null;
  sourceId?: string | null;
  sourceNo?: string | null;
  priority?: number | null;
  status?: number | null;
  totalQuantity?: number | null;
  pickedQuantity?: number | null;
  expectedAt?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  pickerName?: string | null;
  createTime?: string | null;
  remark?: string | null;
}

/** `com.cognora.scm.warehouse.domain.entity.WmsOutboundItem`. */
export interface WmsOutboundItemDTO {
  id: string;
  outboundId?: string | null;
  outboundNo?: string | null;
  skuId?: string | null;
  skuCode?: string | null;
  skuName?: string | null;
  planQuantity?: number | null;
  actualQuantity?: number | null;
  locationCode?: string | null;
}

export type WmsWarehousePage = MpPage<WmsWarehouseDTO>;
export type WmsInboundPage = MpPage<WmsInboundDTO>;
export type WmsOutboundPage = MpPage<WmsOutboundDTO>;

/** `com.cognora.scm.warehouse.domain.entity.WmsWavePicking`. */
export interface WmsWavePickingDTO {
  tenantId?: string | null;
  id: string;
  waveNo?: string | null;
  warehouseId?: string | null;
  outboundIds?: string | null;
  orderCount?: number | null;
  totalItems?: number | null;
  pickingPath?: string | null;
  totalDistance?: number | null;
  optimizationRate?: number | null;
  status?: number | null;
  pickerId?: string | null;
  pickerName?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  remark?: string | null;
}

export type WmsWavePickingPage = MpPage<WmsWavePickingDTO>;

export interface WmsWavePickingQuery {
  page: number;
  size: number;
  warehouseId?: string;
  status?: number;
}
