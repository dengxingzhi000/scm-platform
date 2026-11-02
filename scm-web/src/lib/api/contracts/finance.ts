import type { MpPage } from "./page";

/** `com.scmcloud.finance.domain.entity.SettlementOrder`. */
export interface SettlementOrderDTO {
  id: string;
  settlementNo?: string | null;
  settlementType?: number | null;
  partnerType?: string | null;
  partnerId?: string | null;
  partnerName?: string | null;
  settlementPeriod?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  totalAmount?: number | null;
  discountAmount?: number | null;
  adjustmentAmount?: number | null;
  actualAmount?: number | null;
  paidAmount?: number | null;
  unpaidAmount?: number | null;
  status?: number | null;
  approverName?: string | null;
  approvedAt?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.finance.domain.entity.Invoice`. */
export interface InvoiceDTO {
  id: string;
  invoiceNo?: string | null;
  invoiceType?: number | null;
  invoiceKind?: number | null;
  partyType?: string | null;
  partyId?: string | null;
  partyName?: string | null;
  partyTaxNo?: string | null;
  invoiceCode?: string | null;
  invoiceNumber?: string | null;
  invoiceDate?: string | null;
  amount?: number | null;
  taxAmount?: number | null;
  totalAmount?: number | null;
  taxRate?: number | null;
  settlementId?: string | null;
  issuerName?: string | null;
  issueDate?: string | null;
  status?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

export type SettlementOrderPage = MpPage<SettlementOrderDTO>;
export type InvoicePage = MpPage<InvoiceDTO>;
export type ReconciliationRecordPage = MpPage<ReconciliationRecordDTO>;

export interface ReconciliationQuery {
  page: number;
  size: number;
  keyword?: string;
  reconciliationPeriod?: string;
  status?: number;
  hasDiff?: boolean;
}

/** `com.scmcloud.finance.domain.entity.ReconciliationRecord`. */
export interface ReconciliationRecordDTO {
  id: string;
  tenantId?: string | null;
  reconciliationNo?: string | null;
  partyType?: string | null;
  partyId?: string | null;
  partyName?: string | null;
  reconciliationPeriod?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  ourTotalAmount?: number | null;
  ourPaidAmount?: number | null;
  ourUnpaidAmount?: number | null;
  theirTotalAmount?: number | null;
  theirPaidAmount?: number | null;
  theirUnpaidAmount?: number | null;
  diffAmount?: number | null;
  hasDiff?: boolean | null;
  diffReason?: string | null;
  status?: number | null;
  reconcilerId?: string | null;
  reconcilerName?: string | null;
  reconciledAt?: string | null;
  confirmerId?: string | null;
  confirmerName?: string | null;
  confirmedAt?: string | null;
  attachments?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  deleted?: boolean | null;
  remark?: string | null;
}
