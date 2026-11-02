import dayjs from "dayjs";

import type {
  WmsInboundDTO,
  WmsInboundItemDTO,
  WmsInboundPage,
  WmsOutboundDTO,
  WmsOutboundItemDTO,
  WmsOutboundPage,
  WmsWarehouseDTO,
  WmsWarehousePage,
} from "@/lib/api/contracts/warehouse";
import type {
  InboundItem,
  InboundOrder,
  OutboundItem,
  OutboundOrder,
  PageResult,
  Warehouse,
} from "../types";

export const formatDate = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "-";

const INBOUND_STATUS: Record<
  number,
  { status: InboundOrder["status"]; label: string }
> = {
  0: { status: "pending", label: "待收货" },
  1: { status: "receiving", label: "收货中" },
  2: { status: "receiving", label: "部分入库" },
  3: { status: "completed", label: "已完成" },
  4: { status: "cancelled", label: "已取消" },
};

const OUTBOUND_STATUS: Record<
  number,
  { status: OutboundOrder["status"]; label: string }
> = {
  0: { status: "pending", label: "待出库" },
  1: { status: "picking", label: "拣货中" },
  2: { status: "picking", label: "已打包" },
  3: { status: "shipping", label: "发运中" },
  4: { status: "cancelled", label: "已取消" },
};

export const toWarehouse = (dto: WmsWarehouseDTO): Warehouse => ({
  id: dto.id,
  name: dto.warehouseName ?? "-",
  code: dto.warehouseCode ?? "-",
  address:
    [dto.province, dto.city, dto.district, dto.address]
      .filter(Boolean)
      .join(" ") || "-",
  contactPerson: dto.managerName ?? "-",
  contactPhone: dto.phone ?? "-",
  status: dto.enabled === false ? "disabled" : "active",
  createdAt: formatDate(dto.createTime),
});

export const toWarehousePage = (
  page: WmsWarehousePage | null | undefined
): PageResult<Warehouse> => ({
  records: (page?.records ?? []).map(toWarehouse),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});

export const toInboundItem = (dto: WmsInboundItemDTO): InboundItem => ({
  id: dto.id,
  skuCode: dto.skuCode ?? "-",
  productName: dto.skuName ?? "-",
  expectedQuantity: dto.planQuantity ?? 0,
  actualQuantity: dto.actualQuantity ?? 0,
});

export const toInboundOrder = (
  dto: WmsInboundDTO,
  items: WmsInboundItemDTO[],
  warehouseName: string,
  supplierName: string
): InboundOrder => {
  const mapped = INBOUND_STATUS[dto.status ?? 0] ?? INBOUND_STATUS[0];
  return {
    id: dto.id,
    inboundNo: dto.inboundNo ?? "-",
    warehouseName,
    supplierName,
    items: items.map(toInboundItem),
    status: mapped.status,
    statusLabel: mapped.label,
    createdAt: formatDate(dto.createTime),
  };
};

export const toInboundPage = (
  page: WmsInboundPage | null | undefined,
  itemsByInbound: Record<string, WmsInboundItemDTO[]>,
  warehouseNames: Record<string, string>,
  supplierNames: Record<string, string>
): PageResult<InboundOrder> => ({
  records: (page?.records ?? []).map((dto) =>
    toInboundOrder(
      dto,
      itemsByInbound[dto.id] ?? [],
      warehouseNames[dto.warehouseId ?? ""] ?? "-",
      supplierNames[dto.supplierId ?? ""] ?? "-"
    )
  ),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});

export const toOutboundItem = (dto: WmsOutboundItemDTO): OutboundItem => ({
  id: dto.id,
  skuCode: dto.skuCode ?? "-",
  productName: dto.skuName ?? "-",
  quantity: dto.planQuantity ?? 0,
});

export const toOutboundOrder = (
  dto: WmsOutboundDTO,
  items: WmsOutboundItemDTO[],
  warehouseName: string
): OutboundOrder => {
  const mapped = OUTBOUND_STATUS[dto.status ?? 0] ?? OUTBOUND_STATUS[0];
  return {
    id: dto.id,
    outboundNo: dto.outboundNo ?? "-",
    warehouseName,
    items: items.map(toOutboundItem),
    status: mapped.status,
    statusLabel: mapped.label,
    createdAt: formatDate(dto.createTime),
  };
};

export const toOutboundPage = (
  page: WmsOutboundPage | null | undefined,
  itemsByOutbound: Record<string, WmsOutboundItemDTO[]>,
  warehouseNames: Record<string, string>
): PageResult<OutboundOrder> => ({
  records: (page?.records ?? []).map((dto) =>
    toOutboundOrder(
      dto,
      itemsByOutbound[dto.id] ?? [],
      warehouseNames[dto.warehouseId ?? ""] ?? "-"
    )
  ),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});
