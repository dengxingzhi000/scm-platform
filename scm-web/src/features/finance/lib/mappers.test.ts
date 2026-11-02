import { describe, expect, it } from "vitest";

import type {
  InvoiceDTO,
  SettlementOrderDTO,
} from "@/lib/api/contracts/finance";
import { toInvoice, toSettlement } from "./mappers";

const settlementFixture: SettlementOrderDTO = {
  id: "stl-1",
  settlementNo: "STL-2026-1001",
  partnerName: "Apple供应链",
  settlementPeriod: "2026-09",
  actualAmount: 550000,
  status: 4,
  createTime: "2026-09-28T10:00:00",
};

const invoiceFixture: InvoiceDTO = {
  id: "inv-1",
  invoiceNo: "INV-2026-1001",
  settlementId: "stl-1",
  amount: 550000,
  taxAmount: 71500,
  status: 2,
  createTime: "2026-09-28T10:00:00",
};

describe("finance mappers", () => {
  it("maps settlement status codes and amounts", () => {
    const settlement = toSettlement(settlementFixture);

    expect(settlement.status).toBe("completed");
    expect(settlement.statusLabel).toBe("已付款");
    expect(settlement.supplierName).toBe("Apple供应链");
    expect(settlement.period).toBe("2026-09");
  });

  it("maps invoice statuses including voided and red-flushed", () => {
    expect(toInvoice(invoiceFixture, "STL-2026-1001").status).toBe("sent");
    expect(toInvoice({ ...invoiceFixture, status: 3 }, "-").status).toBe(
      "voided"
    );
    expect(toInvoice({ ...invoiceFixture, status: 4 }, "-").status).toBe(
      "red_flushed"
    );
  });

  it("keeps the settlement number supplied by the caller", () => {
    expect(toInvoice(invoiceFixture, "STL-2026-1001").settlementNo).toBe(
      "STL-2026-1001"
    );
    expect(toInvoice(invoiceFixture, "-").settlementNo).toBe("-");
  });
});
