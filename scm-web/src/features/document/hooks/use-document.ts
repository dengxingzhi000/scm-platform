"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";

import type {
  DocDocumentDTO,
  DocDocumentQuery,
  DocumentFileDTO,
  DocumentFileQuery,
  DocumentStatusChangeRequest,
  DocTemplateDTO,
  DocTemplateSchemaDTO,
  DocTemplateVersionDTO,
  RenderRequest,
  TemplateQuery,
} from "@/lib/api/contracts/document";
import { toApiError } from "@/lib/api/envelope";
import { documentApiService } from "@/lib/api/services/document";

const fail = (error: unknown) => message.error(toApiError(error).message);

export function useTemplatePage(query: TemplateQuery) {
  return useQuery({
    queryKey: ["document", "templates", query],
    queryFn: () => documentApiService.pageTemplates(query),
  });
}

export function useTemplateVersions(templateId: string, enabled = true) {
  return useQuery({
    queryKey: ["document", "template-versions", templateId],
    queryFn: () => documentApiService.listTemplateVersions(templateId),
    enabled: enabled && Boolean(templateId),
  });
}

export function useTemplateSchemas(templateId: string, enabled = true) {
  return useQuery({
    queryKey: ["document", "template-schemas", templateId],
    queryFn: () => documentApiService.listTemplateSchema(templateId),
    enabled: enabled && Boolean(templateId),
  });
}

export function useDocumentPage(query: DocDocumentQuery) {
  return useQuery({
    queryKey: ["document", "documents", query],
    queryFn: () => documentApiService.pageDocuments(query),
  });
}

export function useDocumentVersions(documentId: string, enabled = true) {
  return useQuery({
    queryKey: ["document", "document-versions", documentId],
    queryFn: () => documentApiService.listDocumentVersions(documentId),
    enabled: enabled && Boolean(documentId),
  });
}

export function useDocumentAudits(documentId: string, enabled = true) {
  return useQuery({
    queryKey: ["document", "document-audits", documentId],
    queryFn: () => documentApiService.listDocumentAudits(documentId),
    enabled: enabled && Boolean(documentId),
  });
}

export function useDocumentFilePage(query: DocumentFileQuery, enabled = true) {
  return useQuery({
    queryKey: ["document", "files", query],
    queryFn: () => documentApiService.pageDocumentFiles(query),
    enabled,
  });
}

export function useCreateTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (template: Partial<DocTemplateDTO>) =>
      documentApiService.createTemplate(template),
    onSuccess: () => {
      message.success("模板已创建");
      qc.invalidateQueries({ queryKey: ["document", "templates"] });
    },
    onError: fail,
  });
}

export function useCreateTemplateVersion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      templateId,
      version,
    }: {
      templateId: string;
      version: Partial<DocTemplateVersionDTO>;
    }) => documentApiService.createTemplateVersion(templateId, version),
    onSuccess: (_, { templateId }) => {
      message.success("模板版本已创建");
      qc.invalidateQueries({
        queryKey: ["document", "template-versions", templateId],
      });
    },
    onError: fail,
  });
}

export function usePublishTemplateVersion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      templateId,
      versionId,
    }: {
      templateId: string;
      versionId: string;
    }) => documentApiService.publishTemplateVersion(templateId, versionId),
    onSuccess: (_, { templateId }) => {
      message.success("模板版本已发布");
      qc.invalidateQueries({
        queryKey: ["document", "template-versions", templateId],
      });
      qc.invalidateQueries({ queryKey: ["document", "templates"] });
    },
    onError: fail,
  });
}

export function useSaveTemplateSchema() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      templateId,
      schema,
    }: {
      templateId: string;
      schema: Partial<DocTemplateSchemaDTO>;
    }) => documentApiService.saveTemplateSchema(templateId, schema),
    onSuccess: (_, { templateId }) => {
      message.success("变量 Schema 已保存");
      qc.invalidateQueries({
        queryKey: ["document", "template-schemas", templateId],
      });
    },
    onError: fail,
  });
}

export function useRenderTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: RenderRequest) => documentApiService.render(request),
    onSuccess: () => {
      message.success("模板渲染成功");
      qc.invalidateQueries({ queryKey: ["document", "documents"] });
    },
    onError: fail,
  });
}

export function useCreateDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (document: Partial<DocDocumentDTO>) =>
      documentApiService.createDocument(document),
    onSuccess: () => {
      message.success("文档已创建");
      qc.invalidateQueries({ queryKey: ["document", "documents"] });
    },
    onError: fail,
  });
}

export function useChangeDocumentStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      documentId,
      request,
    }: {
      documentId: string;
      request: DocumentStatusChangeRequest;
    }) => documentApiService.changeDocumentStatus(documentId, request),
    onSuccess: (_, { documentId }) => {
      message.success("文档状态已更新");
      qc.invalidateQueries({ queryKey: ["document", "documents"] });
      qc.invalidateQueries({
        queryKey: ["document", "document-audits", documentId],
      });
    },
    onError: fail,
  });
}

export function useCreateDocumentFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: Partial<DocumentFileDTO>) =>
      documentApiService.createDocumentFile(file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["document", "files"] });
    },
    onError: fail,
  });
}

export function useDeleteDocumentFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => documentApiService.deleteDocumentFile(id),
    onSuccess: () => {
      message.success("文档文件已删除");
      qc.invalidateQueries({ queryKey: ["document", "files"] });
    },
    onError: fail,
  });
}
