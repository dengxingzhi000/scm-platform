import dayjs from "dayjs";

import type {
  TmsTrackingDTO,
  TmsWaybillDTO,
  TmsWaybillPage,
} from "@/lib/api/contracts/logistics";
import type { TrackingEvent, Waybill } from "../types";

export const formatDate = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "-";

export const formatDateTime = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";

const WAYBILL_STATUS: Record<
  number,
  { status: Waybill["status"]; label: string }
> = {
  0: { status: "pending", label: "待揽收" },
  1: { status: "picked", label: "已揽收" },
  2: { status: "in_transit", label: "运输中" },
  3: { status: "in_transit", label: "派送中" },
  4: { status: "delivered", label: "已送达" },
  5: { status: "exception", label: "异常" },
  6: { status: "exception", label: "退回" },
};

/** `sender_address` / `receiver_address` are JSONB objects serialized as strings. */
const formatAddress = (value?: string | null): string => {
  if (!value) return "-";
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    const parts = ["province", "city", "district", "detail"]
      .map((key) => parsed[key])
      .filter((part): part is string => typeof part === "string" && part.length > 0);
    return parts.length > 0 ? parts.join(" ") : value;
  } catch {
    return value;
  }
};

export const toWaybill = (dto: TmsWaybillDTO): Waybill => {
  const mapped = WAYBILL_STATUS[dto.status ?? 0] ?? WAYBILL_STATUS[0];
  return {
    id: dto.id,
    waybillNo: dto.waybillNo ?? "-",
    orderNo: dto.orderNo ?? "-",
    carrierName: dto.carrierName ?? "-",
    // tms_waybill 只保存一个运单号，它本身就是承运商跟踪号。
    trackingNo: dto.waybillNo ?? "-",
    status: mapped.status,
    statusLabel: mapped.label,
    senderName: dto.senderName ?? "-",
    senderAddress: formatAddress(dto.senderAddress),
    receiverName: dto.receiverName ?? "-",
    receiverAddress: formatAddress(dto.receiverAddress),
    createdAt: formatDate(dto.createTime),
  };
};

export const toWaybillPage = (
  page: TmsWaybillPage | null | undefined
): { records: Waybill[]; total: number } => ({
  records: (page?.records ?? []).map(toWaybill),
  total: page?.total ?? 0,
});

export const toTrackingEvent = (dto: TmsTrackingDTO): TrackingEvent => ({
  id: dto.id,
  time: formatDateTime(dto.trackTime),
  location: dto.location ?? "-",
  description: dto.description ?? "-",
  status: dto.trackStatus ?? "",
});
