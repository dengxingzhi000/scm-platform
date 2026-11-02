import type {
  WmsInboundDTO,
  WmsInboundItemDTO,
  WmsOutboundDTO,
  WmsOutboundItemDTO,
} from "@/lib/api/contracts/warehouse";
import { supplierApiService } from "@/lib/api/services/supplier";
import { warehouseApiService } from "@/lib/api/services/warehouse";

import {
  toInboundPage,
  toOutboundPage,
  toWarehousePage,
} from "../lib/mappers";
import type {
  InboundOrder,
  OutboundOrder,
  Warehouse,
} from "../types";

/** The screens render every row in one panel, so one large page keeps it a single request. */
const LIST_PAGE_SIZE = 100;

const loadWarehouseNames = async (): Promise<Record<string, string>> => {
  const page = await warehouseApiService
    .pageWarehouses(1, LIST_PAGE_SIZE)
    .catch(() => null);
  return Object.fromEntries(
    (page?.records ?? []).map((warehouse) => [
      warehouse.id,
      warehouse.warehouseName ?? "-",
    ])
  );
};

const loadSupplierNames = async (): Promise<Record<string, string>> => {
  const page = await supplierApiService
    .list({ page: 1, size: LIST_PAGE_SIZE })
    .catch(() => null);
  return Object.fromEntries(
    (page?.records ?? []).map((supplier) => [supplier.id, supplier.supplierName ?? "-"])
  );
};

const loadInboundItems = async (
  records: WmsInboundDTO[]
): Promise<Record<string, WmsInboundItemDTO[]>> => {
  const entries = await Promise.all(
    records.map(
      async (record) =>
        [
          record.id,
          await warehouseApiService.listInboundItems(record.id).catch(() => []),
        ] as const
    )
  );
  return Object.fromEntries(entries);
};

const loadOutboundItems = async (
  records: WmsOutboundDTO[]
): Promise<Record<string, WmsOutboundItemDTO[]>> => {
  const entries = await Promise.all(
    records.map(
      async (record) =>
        [
          record.id,
          await warehouseApiService.listOutboundItems(record.id).catch(() => []),
        ] as const
    )
  );
  return Object.fromEntries(entries);
};

export const warehouseService = {
  listWarehouses: async (): Promise<Warehouse[]> => {
    const page = await warehouseApiService.pageWarehouses(1, LIST_PAGE_SIZE);
    return toWarehousePage(page).records;
  },

  listInbound: async (): Promise<InboundOrder[]> => {
    const [page, warehouses, suppliers] = await Promise.all([
      warehouseApiService.pageInbound(1, LIST_PAGE_SIZE),
      loadWarehouseNames(),
      loadSupplierNames(),
    ]);
    const items = await loadInboundItems(page.records);
    return toInboundPage(page, items, warehouses, suppliers).records;
  },

  listOutbound: async (): Promise<OutboundOrder[]> => {
    const [page, warehouses] = await Promise.all([
      warehouseApiService.pageOutbound(1, LIST_PAGE_SIZE),
      loadWarehouseNames(),
    ]);
    const items = await loadOutboundItems(page.records);
    return toOutboundPage(page, items, warehouses).records;
  },
};
