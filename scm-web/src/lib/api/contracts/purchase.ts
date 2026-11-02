import type { MpPage } from "./page";

/** `com.scmcloud.purchase.domain.entity.PurOrder`. */
export interface PurOrderDTO {
  id: string;
  orderNo?: string | null;
  supplierId?: string | null;
  supplierName?: string | null;
  supplierContact?: string | null;
  orderType?: number | null;
  totalAmount?: number | null;
  taxAmount?: number | null;
  freightAmount?: number | null;
  discountAmount?: number | null;
  payableAmount?: number | null;
  status?: number | null;
  warehouseId?: string | null;
  warehouseName?: string | null;
  warehouseAddress?: string | null;
  orderDate?: string | null;
  expectedDelivery?: string | null;
  actualDelivery?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  buyerId?: string | null;
  buyerName?: string | null;
  approvedByName?: string | null;
  approvedAt?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.purchase.domain.entity.PurOrderItem`. */
export interface PurOrderItemDTO {
  id: string;
  orderId?: string | null;
  orderNo?: string | null;
  materialCode?: string | null;
  materialName?: string | null;
  materialSpec?: string | null;
  materialCategory?: string | null;
  skuId?: string | null;
  skuCode?: string | null;
  quantity?: number | null;
  unit?: string | null;
  receivedQuantity?: number | null;
  qualifiedQuantity?: number | null;
  unitPrice?: number | null;
  taxRate?: number | null;
  discountRate?: number | null;
  subtotal?: number | null;
  deliveryDate?: string | null;
  qualityRequirement?: string | null;
  itemRemark?: string | null;
  createTime?: string | null;
}

export type PurOrderPage = MpPage<PurOrderDTO>;

export interface PurOrderQuery {
  page: number;
  size: number;
  keyword?: string;
  status?: number;
  supplierId?: string;
}

/** `com.scmcloud.purchase.domain.entity.PurRfq`. */
export interface PurRfqDTO {
  id: string;
  tenantId?: string | null;
  rfqNo?: string | null;
  rfqTitle?: string | null;
  requestId?: string | null;
  requestNo?: string | null;
  rfqType?: number | null;
  inquiryMethod?: number | null;
  deadline?: string | null;
  quotationRequirement?: string | null;
  paymentTerms?: string | null;
  deliveryTerms?: string | null;
  status?: number | null;
  initiatorId?: string | null;
  initiatorName?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  deleted?: boolean | null;
  remark?: string | null;
}

/** `com.scmcloud.purchase.domain.entity.PurRfqItem`. */
export interface PurRfqItemDTO {
  id: string;
  tenantId?: string | null;
  rfqId?: string | null;
  rfqNo?: string | null;
  materialCode?: string | null;
  materialName?: string | null;
  materialSpec?: string | null;
  quantity?: number | null;
  unit?: string | null;
  technicalRequirement?: string | null;
  qualityStandard?: string | null;
  deliveryRequirement?: string | null;
  createTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.purchase.domain.entity.PurQuotation`. */
export interface PurQuotationDTO {
  id: string;
  tenantId?: string | null;
  quotationNo?: string | null;
  rfqId?: string | null;
  rfqNo?: string | null;
  supplierId?: string | null;
  supplierName?: string | null;
  supplierContact?: string | null;
  supplierPhone?: string | null;
  supplierEmail?: string | null;
  totalAmount?: number | null;
  taxRate?: number | null;
  paymentTerms?: string | null;
  paymentMethod?: string | null;
  deliveryTerms?: string | null;
  deliveryPeriod?: string | null;
  warrantyPeriod?: string | null;
  warrantyTerms?: string | null;
  validFrom?: string | null;
  validUntil?: string | null;
  status?: number | null;
  isSelected?: boolean | null;
  selectedBy?: string | null;
  selectedByName?: string | null;
  selectedAt?: string | null;
  attachments?: string | null;
  createTime?: string | null;
  createBy?: string | null;
  updateTime?: string | null;
  updateBy?: string | null;
  deleted?: boolean | null;
  remark?: string | null;
}

/** `com.scmcloud.purchase.domain.entity.PurQuotationItem`. */
export interface PurQuotationItemDTO {
  id: string;
  tenantId?: string | null;
  quotationId?: string | null;
  quotationNo?: string | null;
  rfqItemId?: string | null;
  materialCode?: string | null;
  materialName?: string | null;
  materialSpec?: string | null;
  quantity?: number | null;
  unit?: string | null;
  unitPrice?: number | null;
  taxRate?: number | null;
  subtotal?: number | null;
  deliveryDays?: number | null;
  brand?: string | null;
  origin?: string | null;
  createTime?: string | null;
  remark?: string | null;
}

export type PurRfqPage = MpPage<PurRfqDTO>;
export type PurQuotationPage = MpPage<PurQuotationDTO>;

export interface PurRfqQuery {
  page: number;
  size: number;
  status?: number;
  rfqType?: number;
  keyword?: string;
}

export interface PurQuotationQuery {
  page: number;
  size: number;
  status?: number;
  supplierId?: string;
  rfqId?: string;
}

/** `com.scmcloud.purchase.domain.entity.PurRequest`. */
export interface PurRequestDTO {
  id: string;
  requestNo?: string | null;
  requestType?: number | null;
  priority?: number | null;
  deptId?: string | null;
  deptName?: string | null;
  requesterId?: string | null;
  requesterName?: string | null;
  requesterPhone?: string | null;
  expectedDelivery?: string | null;
  purpose?: string | null;
  budgetAmount?: number | null;
  status?: number | null;
  approvalFlowId?: string | null;
  currentApproverId?: string | null;
  currentApproverName?: string | null;
  submittedAt?: string | null;
  approvedAt?: string | null;
  rejectedAt?: string | null;
  rejectReason?: string | null;
  converted?: boolean | null;
  purchaseOrderId?: string | null;
  purchaseOrderNo?: string | null;
  attachments?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  remark?: string | null;
}

/** `com.scmcloud.purchase.domain.entity.PurRequestItem`. */
export interface PurRequestItemDTO {
  id: string;
  requestId?: string | null;
  requestNo?: string | null;
  materialCode?: string | null;
  materialName?: string | null;
  materialSpec?: string | null;
  materialCategory?: string | null;
  quantity?: number | null;
  unit?: string | null;
  budgetPrice?: number | null;
  requirementDesc?: string | null;
  qualityStandard?: string | null;
  suggestedSupplierId?: string | null;
  suggestedSupplierName?: string | null;
  createTime?: string | null;
  remark?: string | null;
}

export interface PurRequestDetailDTO {
  request: PurRequestDTO;
  items: PurRequestItemDTO[];
}

export type PurRequestPage = MpPage<PurRequestDTO>;

export interface PurRequestQuery {
  page: number;
  size: number;
  status?: number;
  requestType?: number;
  keyword?: string;
}

export interface PurRequestUpsertPayload {
  request: Partial<PurRequestDTO>;
  items: Partial<PurRequestItemDTO>[];
}

export interface PurRequestConvertPayload {
  supplierId: string;
  supplierName?: string;
  supplierContact?: string;
  warehouseId: string;
  warehouseName?: string;
  warehouseAddress?: string;
  orderType?: number;
  expectedDelivery: string;
  freightAmount?: number;
  discountAmount?: number;
  taxRate?: number;
  remark?: string;
  itemPrices?: { requestItemId: string; unitPrice: number }[];
}
