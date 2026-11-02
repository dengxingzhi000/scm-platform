import type { MpPage } from "./page";

/** `com.scmcloud.file.entity.FileMetadata`. */
export interface FileMetadataDTO {
  id: string;
  originalName?: string | null;
  storageKey?: string | null;
  contentType?: string | null;
  fileSize?: number | null;
  storageEngine?: string | null;
  md5?: string | null;
  version?: number | null;
  bizType?: string | null;
  bizId?: string | null;
  status?: string | null;
  refCount?: number | null;
  tenantId?: string | null;
  createBy?: string | null;
  updateBy?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** `com.scmcloud.file.service.ocr.OcrService.InvoiceInfo`. */
export interface InvoiceInfo {
  invoiceNo?: string | null;
  invoiceDate?: string | null;
  amount?: string | null;
  buyerName?: string | null;
  sellerName?: string | null;
  rawText?: string | null;
}

/** `com.scmcloud.file.service.ocr.OcrService.ContractInfo`. */
export interface ContractInfo {
  contractNo?: string | null;
  partyA?: string | null;
  partyB?: string | null;
  amount?: string | null;
  signDate?: string | null;
  rawText?: string | null;
}

export type FileMetadataPage = MpPage<FileMetadataDTO>;

export interface FileQuery {
  page: number;
  size: number;
  keyword?: string;
  bizType?: string;
  bizId?: string;
}

/** `com.scmcloud.file.api.dto.FileStatsDTO`. */
export interface FileStats {
  totalFiles: number;
  totalSize: number;
  imageFiles: number;
  documentFiles: number;
  otherFiles: number;
  lastUploadTime?: string | null;
}

/** `com.scmcloud.file.api.dto.FileUpdateRequest`. Empty strings clear the business link. */
export interface FileUpdatePayload {
  originalName?: string;
  bizType?: string;
  bizId?: string;
}
