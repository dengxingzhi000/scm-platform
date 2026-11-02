"use client";

import { useRef, useState } from "react";
import { message } from "antd";
import { useQueryClient } from "@tanstack/react-query";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import type {
  DocTemplateDTO,
  DocTemplateSchemaDTO,
  DocTemplateVersionDTO,
} from "@/lib/api/contracts/document";
import { DOC_TEMPLATE_VERSION_STATUS } from "@/lib/api/contracts/document";
import { toApiError } from "@/lib/api/envelope";
import { documentApiService } from "@/lib/api/services/document";
import { fileApiService } from "@/lib/api/services/file";
import { cn } from "@/lib/utils";

import {
  useCreateTemplateVersion,
  useDocumentFilePage,
  usePublishTemplateVersion,
  useSaveTemplateSchema,
  useTemplateSchemas,
  useTemplateVersions,
} from "../hooks";

type TabKey = "versions" | "schemas";

const TABS: Array<{ key: TabKey; label: string }> = [
  { key: "versions", label: "版本管理" },
  { key: "schemas", label: "变量 Schema" },
];

const SAMPLE_VARIABLES = `[
  {"name": "contract.no", "type": "String", "required": true, "description": "合同编号", "example": "HT-2026-001"},
  {"name": "party.name", "type": "String", "required": true, "description": "对方名称", "example": "某某供应商"},
  {"name": "contract.amount", "type": "Decimal", "required": true, "description": "合同金额", "example": "100000.00"},
  {"name": "contract.signDate", "type": "Date", "required": false, "description": "签署日期", "example": "2026-10-30"}
]`;

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

interface TemplateManageDialogProps {
  template: DocTemplateDTO | null;
  onOpenChange: (open: boolean) => void;
}

export function TemplateManageDialog({
  template,
  onOpenChange,
}: TemplateManageDialogProps) {
  const templateId = template?.id ?? "";
  const open = Boolean(template);
  const qc = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [tab, setTab] = useState<TabKey>("versions");

  const versionsQuery = useTemplateVersions(templateId, open);
  const schemasQuery = useTemplateSchemas(templateId, open);
  const filesQuery = useDocumentFilePage(
    { page: 1, size: 100, sourceType: "TEMPLATE_SOURCE" },
    open,
  );
  const versions = versionsQuery.data ?? [];
  const schemas = schemasQuery.data ?? [];
  const files = filesQuery.data?.records ?? [];

  const createVersion = useCreateTemplateVersion();
  const publishVersion = usePublishTemplateVersion();
  const saveSchema = useSaveTemplateSchema();

  const [versionForm, setVersionForm] = useState({ fileId: "", schemaId: "" });
  const [uploading, setUploading] = useState(false);

  const [editingSchema, setEditingSchema] =
    useState<Partial<DocTemplateSchemaDTO> | null>(null);
  const [variablesError, setVariablesError] = useState<string | null>(null);

  const fileNameOf = (fileId?: string | null) => {
    if (!fileId) return "未关联源文件";
    const file = files.find((item) => item.id === fileId);
    return file?.originalName ?? fileId;
  };

  const schemaNameOf = (schemaId?: string | null) => {
    if (!schemaId) return "未关联";
    const schema = schemas.find((item) => item.id === schemaId);
    return schema?.name || schema?.schemaCode || schemaId;
  };

  const uploadSource = async (file: File) => {
    setUploading(true);
    try {
      const meta = await fileApiService.upload(file, "TEMPLATE_SOURCE");
      const catalog = await documentApiService.createDocumentFile({
        originalName: file.name,
        sourceType: "TEMPLATE_SOURCE",
        fileRef: meta.id,
      });
      setVersionForm((prev) => ({ ...prev, fileId: catalog.id }));
      qc.invalidateQueries({ queryKey: ["document", "files"] });
      message.success(`已上传模板源文件 ${file.name}`);
    } catch (error) {
      message.error(toApiError(error).message);
    } finally {
      setUploading(false);
    }
  };

  const submitVersion = async () => {
    if (!template) return;
    if (!versionForm.fileId) {
      message.warning("请先选择或上传模板源文件");
      return;
    }
    try {
      await createVersion.mutateAsync({
        templateId: template.id,
        version: {
          fileId: versionForm.fileId,
          schemaId: versionForm.schemaId || null,
        },
      });
      setVersionForm({ fileId: "", schemaId: "" });
    } catch {
      // hook 已提示错误
    }
  };

  const submitPublish = async (version: DocTemplateVersionDTO) => {
    if (!template) return;
    try {
      await publishVersion.mutateAsync({
        templateId: template.id,
        versionId: version.id,
      });
    } catch {
      // hook 已提示错误
    }
  };

  const openSchemaEditor = (schema?: DocTemplateSchemaDTO) => {
    setVariablesError(null);
    setEditingSchema(
      schema
        ? { ...schema, variables: schema.variables ?? "[]" }
        : {
            schemaCode: "",
            name: "",
            description: "",
            variables: SAMPLE_VARIABLES,
            status: "DRAFT",
          },
    );
  };

  const submitSchema = async () => {
    if (!template || !editingSchema) return;
    try {
      const parsed = JSON.parse(editingSchema.variables || "[]");
      if (!Array.isArray(parsed)) throw new Error("not an array");
    } catch {
      setVariablesError("变量定义必须是合法的 JSON 数组");
      return;
    }
    setVariablesError(null);
    try {
      await saveSchema.mutateAsync({
        templateId: template.id,
        schema: editingSchema,
      });
      setEditingSchema(null);
    } catch {
      // hook 已提示错误
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            模板配置 · {template?.templateName ?? "-"}
          </DialogTitle>
          <DialogDescription>
            当前生效版本 v{template?.currentVersion ?? 0}
            ，可在此维护模板版本与变量 Schema。
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-wrap items-center gap-1">
          {TABS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setTab(item.key)}
              className={cn(
                "rounded-md px-3 py-1.5 text-[12px] transition-colors",
                tab === item.key
                  ? "bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]"
                  : "text-muted-foreground hover:bg-sunken",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>

        {tab === "versions" && (
          <div className="space-y-3">
            <div className="space-y-2 rounded-md border border-[var(--color-border-secondary)] p-3">
              <div className="text-[12px] font-medium">新建版本</div>
              <div className="grid gap-2 sm:grid-cols-2">
                <div>
                  <Label className="mb-1 text-[11px]">模板源文件</Label>
                  <Select
                    value={versionForm.fileId}
                    onValueChange={(value) =>
                      setVersionForm({ ...versionForm, fileId: value })
                    }
                  >
                    <SelectTrigger size="sm" className="w-full text-xs">
                      <SelectValue placeholder="选择模板源文件" />
                    </SelectTrigger>
                    <SelectContent>
                      {files.length === 0 ? (
                        <SelectItem value="__empty__" disabled>
                          暂无模板源文件
                        </SelectItem>
                      ) : (
                        files.map((file) => (
                          <SelectItem key={file.id} value={file.id}>
                            {file.originalName ?? file.id}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label className="mb-1 text-[11px]">变量 Schema</Label>
                  <Select
                    value={versionForm.schemaId || "__none__"}
                    onValueChange={(value) =>
                      setVersionForm({
                        ...versionForm,
                        schemaId: value === "__none__" ? "" : value,
                      })
                    }
                  >
                    <SelectTrigger size="sm" className="w-full text-xs">
                      <SelectValue placeholder="选择变量 Schema" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="__none__">不关联</SelectItem>
                      {schemas
                        .filter((schema) => Boolean(schema.id))
                        .map((schema) => (
                          <SelectItem key={schema.id} value={schema.id as string}>
                            {schema.name || schema.schemaCode || schema.id}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".docx,.doc,.xlsx,.pdf"
                  className="hidden"
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    if (file) void uploadSource(file);
                    event.target.value = "";
                  }}
                />
                <Button
                  size="sm"
                  variant="outline"
                  disabled={uploading}
                  onClick={() => fileInputRef.current?.click()}
                >
                  {uploading ? "上传中…" : "上传源文件"}
                </Button>
                <Button
                  size="sm"
                  disabled={createVersion.isPending || !versionForm.fileId}
                  onClick={submitVersion}
                >
                  创建版本
                </Button>
                <span className="text-muted-foreground text-[11px]">
                  新版本自动递增版本号，状态为草稿。
                </span>
              </div>
            </div>

            {versionsQuery.isLoading ? (
              <div className="text-muted-foreground py-6 text-center text-[12px]">
                版本加载中…
              </div>
            ) : versions.length === 0 ? (
              <div className="text-muted-foreground py-6 text-center text-[12px]">
                暂无版本，请先上传模板源文件并创建版本。
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-16">版本</TableHead>
                    <TableHead>源文件</TableHead>
                    <TableHead>变量 Schema</TableHead>
                    <TableHead className="text-center">状态</TableHead>
                    <TableHead>发布时间</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {versions.map((version) => {
                    const status =
                      DOC_TEMPLATE_VERSION_STATUS[version.status ?? ""] ??
                      DOC_TEMPLATE_VERSION_STATUS.DRAFT;
                    return (
                      <TableRow key={version.id}>
                        <TableCell className="tabular-nums">
                          v{version.version ?? "-"}
                        </TableCell>
                        <TableCell className="max-w-[220px] truncate">
                          {fileNameOf(version.fileId)}
                        </TableCell>
                        <TableCell className="max-w-[160px] truncate">
                          {schemaNameOf(version.schemaId)}
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge variant={status.variant}>{status.label}</Badge>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {formatDateTime(version.publishedAt)}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={
                              publishVersion.isPending ||
                              version.status === "PUBLISHED"
                            }
                            onClick={() => submitPublish(version)}
                          >
                            {version.status === "PUBLISHED" ? "已发布" : "发布"}
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            )}
          </div>
        )}

        {tab === "schemas" && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground text-[11px]">
                变量 Schema 驱动渲染时的字段校验与表单生成。
              </span>
              <Button size="sm" onClick={() => openSchemaEditor()}>
                新建 Schema
              </Button>
            </div>
            {schemasQuery.isLoading ? (
              <div className="text-muted-foreground py-6 text-center text-[12px]">
                Schema 加载中…
              </div>
            ) : schemas.length === 0 ? (
              <div className="text-muted-foreground py-6 text-center text-[12px]">
                暂无变量 Schema
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>编码 / 名称</TableHead>
                    <TableHead>说明</TableHead>
                    <TableHead className="text-center">变量数</TableHead>
                    <TableHead className="text-center">状态</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {schemas.map((schema) => (
                    <TableRow key={schema.id ?? schema.schemaCode ?? ""}>
                      <TableCell>
                        <div className="text-[12px] font-medium">
                          {schema.name || "-"}
                        </div>
                        <div className="text-muted-foreground font-mono text-[10px]">
                          {schema.schemaCode ?? "-"}
                        </div>
                      </TableCell>
                      <TableCell className="max-w-[220px] truncate text-[11px]">
                        {schema.description ?? "-"}
                      </TableCell>
                      <TableCell className="text-center tabular-nums">
                        {variableCount(schema.variables)}
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge variant="secondary">
                          {schema.status ?? "DRAFT"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openSchemaEditor(schema)}
                        >
                          编辑
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>

        <Dialog
          open={Boolean(editingSchema)}
          onOpenChange={(next) => !next && setEditingSchema(null)}
        >
          <DialogContent className="sm:max-w-xl">
            <DialogHeader>
              <DialogTitle>
                {editingSchema?.id ? "编辑变量 Schema" : "新建变量 Schema"}
              </DialogTitle>
              <DialogDescription>
                变量定义使用 JSON 数组，name 支持点路径（如 party.name）。
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-3">
              <div className="grid gap-2 sm:grid-cols-2">
                <div>
                  <Label className="mb-1 text-[11px]">Schema 编码</Label>
                  <Input
                    value={editingSchema?.schemaCode ?? ""}
                    placeholder="例如 SCHEMA-CONTRACT"
                    onChange={(event) =>
                      setEditingSchema({
                        ...editingSchema,
                        schemaCode: event.target.value,
                      })
                    }
                  />
                </div>
                <div>
                  <Label className="mb-1 text-[11px]">名称</Label>
                  <Input
                    value={editingSchema?.name ?? ""}
                    onChange={(event) =>
                      setEditingSchema({
                        ...editingSchema,
                        name: event.target.value,
                      })
                    }
                  />
                </div>
              </div>
              <div>
                <Label className="mb-1 text-[11px]">说明</Label>
                <Input
                  value={editingSchema?.description ?? ""}
                  onChange={(event) =>
                    setEditingSchema({
                      ...editingSchema,
                      description: event.target.value,
                    })
                  }
                />
              </div>
              <div>
                <div className="mb-1 flex items-center justify-between">
                  <Label className="text-[11px]">变量定义（JSON）</Label>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-6 px-2 text-[11px]"
                    onClick={() =>
                      setEditingSchema({
                        ...editingSchema,
                        variables: SAMPLE_VARIABLES,
                      })
                    }
                  >
                    插入示例
                  </Button>
                </div>
                <Textarea
                  rows={9}
                  className="font-mono text-[11px]"
                  value={editingSchema?.variables ?? ""}
                  onChange={(event) => {
                    setVariablesError(null);
                    setEditingSchema({
                      ...editingSchema,
                      variables: event.target.value,
                    });
                  }}
                />
                {variablesError && (
                  <div className="mt-1 text-[11px] text-[var(--color-error-text)]">
                    {variablesError}
                  </div>
                )}
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setEditingSchema(null)}>
                取消
              </Button>
              <Button
                disabled={saveSchema.isPending || !editingSchema?.name}
                onClick={submitSchema}
              >
                保存
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </DialogContent>
    </Dialog>
  );
}

function variableCount(variables?: string | null) {
  if (!variables) return 0;
  try {
    const parsed = JSON.parse(variables);
    return Array.isArray(parsed) ? parsed.length : 0;
  } catch {
    return 0;
  }
}
