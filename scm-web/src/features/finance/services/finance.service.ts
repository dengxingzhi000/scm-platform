import { financeApiService } from "@/lib/api/services/finance";

import {
  toInvoicePage,
  toSettlementPage,
} from "../lib/mappers";
import type { Invoice, Settlement } from "../types";

const LIST_SIZE = 100;

export const financeService = {
  listSettlements: async (): Promise<Settlement[]> =>
    toSettlementPage(await financeApiService.listSettlements(1, LIST_SIZE)),

  listInvoices: async (): Promise<Invoice[]> => {
    const [invoices, settlements] = await Promise.all([
      financeApiService.pageInvoices(1, LIST_SIZE),
      financeApiService.listSettlements(1, LIST_SIZE).catch(() => null),
    ]);

    const settlementNoById = Object.fromEntries(
      (settlements?.records ?? []).map((settlement) => [
        settlement.id,
        settlement.settlementNo ?? "-",
      ])
    );

    return toInvoicePage(invoices, settlementNoById);
  },
};
