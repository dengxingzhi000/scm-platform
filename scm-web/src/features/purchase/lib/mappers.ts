import dayjs from "dayjs";

import type {
  PurOrderDTO,
  PurOrderItemDTO,
  PurOrderPage,
} from "@/lib/api/contracts/purchase";
import type {
  PageResult,
  PurchaseOrder,
  PurchaseOrderItem,
} from "../types";

export const formatDate = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "-";

const STATUS_BY_CODE: Record<
  number,
  { status: PurchaseOrder["status"]; label: string }
> = {
  0: { status: "draft", label: "草稿" },
  1: { status: "pending", label: "待审批" },
  2: { status: "approved", label: "已审批" },
  3: { status: "ordered", label: "已下单" },
  4: { status: "received", label: "部分收货" },
  5: { status: "completed", label: "已完成" },
  6: { status: "cancelled", label: "已取消" },
  7: { status: "exception", label: "异常" },
};

const STATUS_BY_UI: Record<PurchaseOrder["status"], number> = {
  draft: 0,
  pending: 1,
  approved: 2,
  ordered: 3,
  received: 4,
  completed: 5,
  cancelled: 6,
  exception: 7,
};

const mapStatus = (code?: number | null) =>
  STATUS_BY_CODE[code ?? 0] ?? STATUS_BY_CODE[0];

export const purchaseStatusToApi = (
  status?: string
): number | undefined =>
  status ? STATUS_BY_UI[status as PurchaseOrder["status"]] : undefined;

export const toPurchaseOrderItem = (
  dto: PurOrderItemDTO
): PurchaseOrderItem => ({
  id: dto.id,
  purchaseOrderId: dto.orderId ?? "",
  skuId: dto.skuId ?? "",
  skuCode: dto.skuCode ?? dto.materialCode ?? "-",
  productName: dto.materialName ?? dto.skuCode ?? "-",
  quantity: dto.quantity ?? 0,
  unitPrice: dto.unitPrice ?? 0,
  totalPrice:
    dto.subtotal ??
    (dto.unitPrice ?? 0) * (dto.quantity ?? 0),
  receivedQuantity: dto.receivedQuantity ?? 0,
});

export const toPurchaseOrder = (
  dto: PurOrderDTO,
  items: PurOrderItemDTO[] = []
): PurchaseOrder => {
  const mapped = mapStatus(dto.status);
  return {
    id: dto.id,
    purchaseNo: dto.orderNo ?? "-",
    supplierId: dto.supplierId ?? "",
    supplierName: dto.supplierName ?? "-",
    items: items.map(toPurchaseOrderItem),
    totalAmount: dto.totalAmount ?? dto.payableAmount ?? 0,
    status: mapped.status,
    statusLabel: mapped.label,
    remark: dto.remark ?? undefined,
    createdAt: formatDate(dto.createTime ?? dto.orderDate),
    updatedAt: formatDate(dto.updateTime ?? dto.createTime),
  };
};

export const toPurchaseOrderPage = (
  page: PurOrderPage | null | undefined,
  itemsByOrder: Record<string, PurOrderItemDTO[]>
): PageResult<PurchaseOrder> => ({
  records: (page?.records ?? []).map((dto) =>
    toPurchaseOrder(dto, itemsByOrder[dto.id] ?? [])
  ),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});
