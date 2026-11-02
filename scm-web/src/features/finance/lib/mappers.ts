import dayjs from "dayjs";

import type {
  InvoiceDTO,
  InvoicePage,
  SettlementOrderDTO,
  SettlementOrderPage,
} from "@/lib/api/contracts/finance";
import type { Invoice, Settlement } from "../types";

export const formatDate = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "-";

const SETTLEMENT_STATUS: Record<
  number,
  { status: Settlement["status"]; label: string }
> = {
  0: { status: "pending", label: "待确认" },
  1: { status: "processing", label: "已确认" },
  2: { status: "processing", label: "待付款" },
  3: { status: "processing", label: "部分付款" },
  4: { status: "completed", label: "已付款" },
};

const INVOICE_STATUS: Record<
  number,
  { status: Invoice["status"]; label: string }
> = {
  0: { status: "pending", label: "待开票" },
  1: { status: "issued", label: "已开票" },
  2: { status: "sent", label: "已寄送" },
  3: { status: "voided", label: "已作废" },
  4: { status: "red_flushed", label: "已红冲" },
};

export const toSettlement = (dto: SettlementOrderDTO): Settlement => {
  const mapped = SETTLEMENT_STATUS[dto.status ?? 0] ?? SETTLEMENT_STATUS[0];
  return {
    id: dto.id,
    settlementNo: dto.settlementNo ?? "-",
    supplierName: dto.partnerName ?? "-",
    amount: dto.actualAmount ?? dto.totalAmount ?? 0,
    status: mapped.status,
    statusLabel: mapped.label,
    period: dto.settlementPeriod ?? "-",
    createdAt: formatDate(dto.createTime),
  };
};

export const toSettlementPage = (
  page: SettlementOrderPage | null | undefined
): Settlement[] => (page?.records ?? []).map(toSettlement);

export const toInvoice = (
  dto: InvoiceDTO,
  settlementNo: string
): Invoice => {
  const mapped = INVOICE_STATUS[dto.status ?? 0] ?? INVOICE_STATUS[0];
  return {
    id: dto.id,
    invoiceNo: dto.invoiceNo ?? "-",
    settlementNo,
    amount: dto.amount ?? 0,
    taxAmount: dto.taxAmount ?? 0,
    status: mapped.status,
    statusLabel: mapped.label,
    createdAt: formatDate(dto.createTime ?? dto.invoiceDate),
  };
};

export const toInvoicePage = (
  page: InvoicePage | null | undefined,
  settlementNoById: Record<string, string>
): Invoice[] =>
  (page?.records ?? []).map((dto) =>
    toInvoice(dto, dto.settlementId ? settlementNoById[dto.settlementId] ?? "-" : "-")
  );
