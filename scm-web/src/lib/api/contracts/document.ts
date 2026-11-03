import type { MpPage } from "./page";

/** `com.cognora.platform.document.template.domain.entity.DocTemplate`. */
export interface DocTemplateDTO {
  id: string;
  templateCode?: string | null;
  templateName?: string | null;
  templateType?: string | null;
  category?: string | null;
  fileFormat?: string | null;
  status?: string | null;
  currentVersion?: number | null;
  description?: string | null;
  tenantId?: string | null;
  createdBy?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** `com.cognora.platform.document.template.domain.entity.DocTemplateSchema`. */
export interface DocTemplateSchemaDTO {
  id?: string | null;
  templateId?: string | null;
  schemaCode?: string | null;
  name?: string | null;
  description?: string | null;
  variables?: string | null;
  status?: string | null;
  tenantId?: string | null;
  createdBy?: string | null;
  createTime?: string | null;
}

/** `com.cognora.platform.document.document.domain.entity.DocDocument`. */
export interface DocDocumentDTO {
  id: string;
  docCode?: string | null;
  docName?: string | null;
  templateId?: string | null;
  templateVersion?: number | null;
  currentVersion?: number | null;
  status?: string | null;
  businessType?: string | null;
  businessId?: string | null;
  tenantId?: string | null;
  createdBy?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** `com.cognora.platform.document.file.domain.entity.DocumentFile`. */
export interface DocumentFileDTO {
  id: string;
  originalName?: string | null;
  sourceType?: string | null;
  refId?: string | null;
  fileRef?: string | null;
  tenantId?: string | null;
  createBy?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** `com.cognora.platform.document.template.domain.entity.DocTemplateVersion`. */
export interface DocTemplateVersionDTO {
  id: string;
  templateId?: string | null;
  version?: number | null;
  fileId?: string | null;
  schemaId?: string | null;
  status?: string | null;
  checksum?: string | null;
  createdBy?: string | null;
  createTime?: string | null;
  publishedAt?: string | null;
}

/** `com.cognora.platform.document.document.domain.entity.DocDocumentVersion`. */
export interface DocDocumentVersionDTO {
  id: string;
  documentId?: string | null;
  version?: number | null;
  fileId?: string | null;
  renderParamsChecksum?: string | null;
  createTime?: string | null;
}

/** `com.cognora.platform.document.document.domain.entity.DocDocumentAudit`. */
export interface DocDocumentAuditDTO {
  id: string;
  documentId?: string | null;
  operation?: string | null;
  operatorId?: string | null;
  operatorName?: string | null;
  ip?: string | null;
  userAgent?: string | null;
  beforeVersion?: number | null;
  afterVersion?: number | null;
  traceId?: string | null;
  createTime?: string | null;
}

/** `com.cognora.platform.document.template.domain.dto.TemplateVariable`. */
export interface TemplateVariableDTO {
  name: string;
  type?: string | null;
  required?: boolean | null;
  defaultValue?: string | null;
  description?: string | null;
  example?: string | null;
}

export interface DocumentStatusChangeRequest {
  status: "SIGNED" | "ARCHIVED";
  operatorId?: string;
  operatorName?: string;
}

export interface RenderRequest {
  /** 模板版本 ID（不是模板 ID）。 */
  templateVersionId: string;
  data: Record<string, unknown>;
  businessType?: string;
  businessId?: string;
  operatorId?: string;
  operatorName?: string;
}

export interface RenderResult {
  documentId?: string | null;
  documentVersion?: number | null;
  docCode?: string | null;
  status?: string | null;
}

export type DocTemplatePage = MpPage<DocTemplateDTO>;
export type DocDocumentPage = MpPage<DocDocumentDTO>;
export type DocumentFilePage = MpPage<DocumentFileDTO>;

export interface TemplateQuery {
  page: number;
  size: number;
  keyword?: string;
  templateType?: string;
  category?: string;
  status?: string;
}

export interface DocDocumentQuery {
  page: number;
  size: number;
  keyword?: string;
  businessType?: string;
  status?: string;
}

export interface DocumentFileQuery {
  page: number;
  size: number;
  keyword?: string;
  sourceType?: string;
  refId?: string;
}

export const DOC_TEMPLATE_STATUS: Record<
  string,
  { label: string; variant: "secondary" | "warning" | "success" | "info" }
> = {
  DRAFT: { label: "草稿", variant: "secondary" },
  REVIEWING: { label: "审核中", variant: "warning" },
  PUBLISHED: { label: "已发布", variant: "success" },
  ARCHIVED: { label: "已归档", variant: "info" },
};

export const DOC_TEMPLATE_VERSION_STATUS: Record<
  string,
  { label: string; variant: "secondary" | "warning" | "success" | "info" }
> = {
  DRAFT: { label: "草稿", variant: "secondary" },
  PUBLISHED: { label: "已发布", variant: "success" },
  ARCHIVED: { label: "已归档", variant: "info" },
};

export const DOC_DOCUMENT_OPERATION: Record<string, string> = {
  GENERATE: "生成",
  RENDER: "渲染",
  PREVIEW: "预览",
  SIGN: "签署",
  ARCHIVE: "归档",
};

export const DOC_DOCUMENT_STATUS: Record<
  string,
  { label: string; variant: "secondary" | "warning" | "success" | "info" }
> = {
  GENERATED: { label: "已生成", variant: "success" },
  SIGNED: { label: "已签署", variant: "info" },
  ARCHIVED: { label: "已归档", variant: "secondary" },
};
