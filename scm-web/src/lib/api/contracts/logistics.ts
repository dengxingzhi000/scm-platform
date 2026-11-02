import type { MpPage } from "./page";

/** `com.scmcloud.logistics.domain.entity.TmsWaybill`. */
export interface TmsWaybillDTO {
  id: string;
  waybillNo?: string | null;
  carrierId?: string | null;
  carrierName?: string | null;
  orderId?: string | null;
  orderNo?: string | null;
  senderName?: string | null;
  senderPhone?: string | null;
  /** JSONB object serialized as a string. */
  senderAddress?: string | null;
  receiverName?: string | null;
  receiverPhone?: string | null;
  /** JSONB object serialized as a string. */
  receiverAddress?: string | null;
  goodsName?: string | null;
  status?: number | null;
  courierName?: string | null;
  courierPhone?: string | null;
  signTime?: string | null;
  exceptionReason?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.logistics.domain.entity.TmsTracking`. */
export interface TmsTrackingDTO {
  id: string;
  waybillId?: string | null;
  waybillNo?: string | null;
  trackTime?: string | null;
  location?: string | null;
  description?: string | null;
  longitude?: number | null;
  latitude?: number | null;
  trackStatus?: string | null;
  operator?: string | null;
  source?: string | null;
  extraData?: string | null;
  createTime?: string | null;
}

/** `com.scmcloud.logistics.domain.entity.TmsCarrier`. */
export interface TmsCarrierDTO {
  tenantId?: string | null;
  id: string;
  carrierCode?: string | null;
  carrierName?: string | null;
  carrierType?: number | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  website?: string | null;
  apiUrl?: string | null;
  apiKey?: string | null;
  apiSecret?: string | null;
  serviceArea?: string | null;
  serviceTypes?: string | null;
  baseRate?: number | null;
  enabled?: boolean | null;
  sortOrder?: number | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  deleted?: boolean | null;
  remark?: string | null;
}

export type TmsWaybillPage = MpPage<TmsWaybillDTO>;
export type TmsTrackingPage = MpPage<TmsTrackingDTO>;
export type TmsCarrierPage = MpPage<TmsCarrierDTO>;

export interface TmsTrackingQuery {
  page: number;
  size: number;
  waybillNo?: string;
  trackStatus?: string;
}

export interface TmsCarrierQuery {
  page: number;
  size: number;
  carrierName?: string;
  carrierType?: number;
  enabled?: boolean;
}

export const CARRIER_TYPE: Record<number, string> = {
  1: "快递",
  2: "物流",
  3: "同城配送",
  4: "自营配送",
};
