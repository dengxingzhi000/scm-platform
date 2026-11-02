import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  DocDocumentAuditDTO,
  DocDocumentDTO,
  DocDocumentPage,
  DocDocumentQuery,
  DocDocumentVersionDTO,
  DocumentFileDTO,
  DocumentFilePage,
  DocumentFileQuery,
  DocumentStatusChangeRequest,
  DocTemplateDTO,
  DocTemplatePage,
  DocTemplateSchemaDTO,
  DocTemplateVersionDTO,
  RenderRequest,
  RenderResult,
  TemplateQuery,
} from "../contracts/document";

const DOCUMENT_BASE = "/scm-document";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.post(url, body));

const put = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.put(url, body));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const documentApiService = {
  // ---- 模板 ----
  pageTemplates: (query: TemplateQuery) =>
    get<DocTemplatePage>(`${DOCUMENT_BASE}/api/v1/templates/page`, query),
  getTemplate: (id: string) =>
    get<DocTemplateDTO>(`${DOCUMENT_BASE}/api/v1/templates/${id}`),
  listTemplateVersions: (id: string) =>
    get<DocTemplateVersionDTO[]>(`${DOCUMENT_BASE}/api/v1/templates/${id}/versions`),
  createTemplate: (template: Partial<DocTemplateDTO>) =>
    post<DocTemplateDTO>(`${DOCUMENT_BASE}/api/v1/templates`, template),
  updateTemplate: (id: string, template: Partial<DocTemplateDTO>) =>
    put<DocTemplateDTO>(`${DOCUMENT_BASE}/api/v1/templates/${id}`, template),
  createTemplateVersion: (
    templateId: string,
    version: Partial<DocTemplateVersionDTO>,
  ) =>
    post<DocTemplateVersionDTO>(
      `${DOCUMENT_BASE}/api/v1/templates/${templateId}/versions`,
      version,
    ),
  publishTemplateVersion: (templateId: string, versionId: string) =>
    post<DocTemplateVersionDTO>(
      `${DOCUMENT_BASE}/api/v1/templates/${templateId}/versions/${versionId}/publish`,
    ),
  listTemplateSchema: (templateId: string) =>
    get<DocTemplateSchemaDTO[]>(
      `${DOCUMENT_BASE}/api/v1/templates/${templateId}/schema`,
    ),
  saveTemplateSchema: (
    templateId: string,
    schema: Partial<DocTemplateSchemaDTO>,
  ) =>
    post<DocTemplateSchemaDTO>(
      `${DOCUMENT_BASE}/api/v1/templates/${templateId}/schema`,
      schema,
    ),
  render: (request: RenderRequest) =>
    post<RenderResult>(`${DOCUMENT_BASE}/api/v1/templates/render`, request),

  // ---- 文档 ----
  pageDocuments: (query: DocDocumentQuery) =>
    get<DocDocumentPage>(`${DOCUMENT_BASE}/api/v1/documents/page`, query),
  getDocument: (id: string) =>
    get<DocDocumentDTO>(`${DOCUMENT_BASE}/api/v1/documents/${id}`),
  createDocument: (document: Partial<DocDocumentDTO>) =>
    post<DocDocumentDTO>(`${DOCUMENT_BASE}/api/v1/documents`, document),
  listDocumentVersions: (id: string) =>
    get<DocDocumentVersionDTO[]>(`${DOCUMENT_BASE}/api/v1/documents/${id}/versions`),
  listDocumentAudits: (id: string) =>
    get<DocDocumentAuditDTO[]>(`${DOCUMENT_BASE}/api/v1/documents/${id}/audits`),
  changeDocumentStatus: (id: string, request: DocumentStatusChangeRequest) =>
    post<DocDocumentDTO>(`${DOCUMENT_BASE}/api/v1/documents/${id}/status`, request),

  // ---- 文档文件 ----
  pageDocumentFiles: (query: DocumentFileQuery) =>
    get<DocumentFilePage>(`${DOCUMENT_BASE}/api/v1/document-files/page`, query),
  getDocumentFile: (id: string) =>
    get<DocumentFileDTO>(`${DOCUMENT_BASE}/api/v1/document-files/${id}`),
  getDocumentFileUrl: (id: string) =>
    get<Record<string, string>>(
      `${DOCUMENT_BASE}/api/v1/document-files/${id}/url`,
    ),
  createDocumentFile: (file: Partial<DocumentFileDTO>) =>
    post<DocumentFileDTO>(`${DOCUMENT_BASE}/api/v1/document-files`, file),
  deleteDocumentFile: (id: string) =>
    del<void>(`${DOCUMENT_BASE}/api/v1/document-files/${id}`),
};
