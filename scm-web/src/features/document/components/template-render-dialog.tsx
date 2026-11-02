"use client";

import { useState } from "react";
import { message } from "antd";

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
import { Textarea } from "@/components/ui/textarea";
import {
  DOC_TEMPLATE_VERSION_STATUS,
  type DocTemplateDTO,
  type RenderResult,
  type TemplateVariableDTO,
} from "@/lib/api/contracts/document";

import {
  useRenderTemplate,
  useTemplateSchemas,
  useTemplateVersions,
} from "../hooks";

interface TemplateRenderDialogProps {
  template: DocTemplateDTO;
  onOpenChange: (open: boolean) => void;
}

function parseVariables(variables?: string | null): TemplateVariableDTO[] {
  if (!variables) return [];
  try {
    const parsed: unknown = JSON.parse(variables);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (item): item is TemplateVariableDTO =>
        Boolean(item) &&
        typeof item === "object" &&
        typeof (item as TemplateVariableDTO).name === "string",
    );
  } catch {
    return [];
  }
}

function setPath(target: Record<string, unknown>, path: string, value: unknown) {
  const segments = path.split(".").filter(Boolean);
  if (segments.length === 0) return;
  let current = target;
  for (const segment of segments.slice(0, -1)) {
    const next = current[segment];
    if (!next || typeof next !== "object") {
      current[segment] = {};
    }
    current = current[segment] as Record<string, unknown>;
  }
  current[segments[segments.length - 1]] = value;
}

function castValue(type: string | null | undefined, raw: string): unknown {
  switch (type) {
    case "Integer": {
      const value = Number.parseInt(raw, 10);
      return Number.isNaN(value) ? raw : value;
    }
    case "Decimal": {
      const value = Number(raw);
      return Number.isNaN(value) ? raw : value;
    }
    case "Boolean":
      return raw === "true";
    default:
      return raw;
  }
}

export function TemplateRenderDialog({
  template,
  onOpenChange,
}: TemplateRenderDialogProps) {
  const versionsQuery = useTemplateVersions(template.id);
  const schemasQuery = useTemplateSchemas(template.id);
  const render = useRenderTemplate();

  const versions = versionsQuery.data ?? [];
  const schemas = schemasQuery.data ?? [];

  const [versionId, setVersionId] = useState("");
  const [values, setValues] = useState<Record<string, string>>({});
  const [jsonMode, setJsonMode] = useState(false);
  const [jsonText, setJsonText] = useState("{}");
  const [businessType, setBusinessType] = useState(template.templateType ?? "");
  const [businessId, setBusinessId] = useState("");
  const [result, setResult] = useState<RenderResult | null>(null);

  // 默认使用已发布的最新版本，避免用户手填版本 ID。
  const defaultVersion = versions.find((item) => item.status === "PUBLISHED") ?? versions[0];
  const activeVersionId = versionId || defaultVersion?.id || "";
  const selectedVersion = versions.find((item) => item.id === activeVersionId);
  const schema = schemas.find((item) => item.id === selectedVersion?.schemaId);
  const variables = parseVariables(schema?.variables);

  const selectVersion = (value: string) => {
    setVersionId(value);
    setValues({});
    setJsonText("{}");
    setJsonMode(false);
    setResult(null);
  };

  const buildData = () => {
    const data: Record<string, unknown> = {};
    for (const variable of variables) {
      const raw =
        values[variable.name] ?? variable.defaultValue ?? "";
      if (raw === "") continue;
      setPath(data, variable.name, castValue(variable.type, raw));
    }
    return data;
  };

  const toggleJsonMode = () => {
    if (!jsonMode) {
      setJsonText(JSON.stringify(buildData(), null, 2));
    }
    setJsonMode(!jsonMode);
  };

  const submit = async () => {
    if (!activeVersionId) return;
    let data: Record<string, unknown>;
    if (jsonMode) {
      try {
        const parsed: unknown = JSON.parse(jsonText || "{}");
        if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
          message.error("变量数据必须是 JSON 对象");
          return;
        }
        data = parsed as Record<string, unknown>;
      } catch {
        message.error("变量数据不是合法的 JSON");
        return;
      }
    } else {
      const missing = variables.find(
        (variable) =>
          variable.required &&
          !(values[variable.name] ?? variable.defaultValue),
      );
      if (missing) {
        message.warning(`请填写必填变量：${missing.description || missing.name}`);
        return;
      }
      data = buildData();
    }
    try {
      const rendered = await render.mutateAsync({
        templateVersionId: activeVersionId,
        data,
        businessType: businessType || undefined,
        businessId: businessId || undefined,
      });
      setResult(rendered);
    } catch {
      // hook 已提示错误
    }
  };

  const statusMeta = selectedVersion
    ? DOC_TEMPLATE_VERSION_STATUS[selectedVersion.status ?? ""] ??
      DOC_TEMPLATE_VERSION_STATUS.DRAFT
    : null;

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>渲染模板 · {template.templateName ?? "-"}</DialogTitle>
          <DialogDescription>
            选择模板版本并按变量 Schema 填写数据，生成文档后可在文档管理中查看。
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div>
            <Label className="mb-1.5 text-[12px]">模板版本</Label>
            {versionsQuery.isLoading ? (
              <div className="text-muted-foreground text-[12px]">版本加载中…</div>
            ) : versions.length > 0 ? (
              <div className="flex items-center gap-2">
                <Select value={activeVersionId} onValueChange={selectVersion}>
                  <SelectTrigger className="flex-1">
                    <SelectValue placeholder="选择版本" />
                  </SelectTrigger>
                  <SelectContent>
                    {versions.map((version) => (
                      <SelectItem key={version.id} value={version.id}>
                        v{version.version ?? "-"} ·{" "}
                        {DOC_TEMPLATE_VERSION_STATUS[version.status ?? ""]?.label ??
                          version.status ??
                          "-"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {statusMeta && (
                  <Badge variant={statusMeta.variant}>{statusMeta.label}</Badge>
                )}
              </div>
            ) : (
              <div className="text-muted-foreground text-[12px]">
                该模板暂无版本，请先在“管理”中上传模板源文件并发布版本。
              </div>
            )}
            {selectedVersion && !selectedVersion.fileId && (
              <div className="mt-1 text-[11px] text-[var(--color-warning-text)]">
                所选版本未关联模板源文件，无法渲染。
              </div>
            )}
          </div>

          <div className="grid gap-2 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">业务类型</Label>
              <Input
                value={businessType}
                placeholder="例如 PURCHASE_CONTRACT"
                onChange={(event) => setBusinessType(event.target.value)}
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">业务 ID</Label>
              <Input
                value={businessId}
                placeholder="关联单据 ID（可选）"
                onChange={(event) => setBusinessId(event.target.value)}
              />
            </div>
          </div>

          <div>
            <div className="mb-1.5 flex items-center justify-between">
              <Label className="text-[12px]">
                变量数据
                {variables.length > 0 && !jsonMode && (
                  <span className="text-muted-foreground ml-1 text-[10px]">
                    （按 Schema 生成 {variables.length} 个字段）
                  </span>
                )}
              </Label>
              <Button
                variant="ghost"
                size="sm"
                className="h-6 px-2 text-[11px]"
                onClick={toggleJsonMode}
              >
                {jsonMode ? "切换表单模式" : "切换 JSON 模式"}
              </Button>
            </div>

            {jsonMode ? (
              <Textarea
                rows={8}
                className="font-mono text-[11px]"
                value={jsonText}
                onChange={(event) => setJsonText(event.target.value)}
              />
            ) : variables.length === 0 ? (
              <Textarea
                rows={6}
                className="font-mono text-[11px]"
                value={jsonText}
                placeholder='该版本未关联变量 Schema，可直接填写 JSON，如 {"name": "张三"}'
                onChange={(event) => {
                  setJsonText(event.target.value);
                  setJsonMode(true);
                }}
              />
            ) : (
              <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
                {variables.map((variable) => (
                  <div key={variable.name}>
                    <div className="mb-1 flex items-center gap-1.5">
                      <span className="font-mono text-[11px]">{variable.name}</span>
                      {variable.required && (
                        <span className="text-[10px] text-[var(--color-error-text)]">
                          必填
                        </span>
                      )}
                      {variable.type && (
                        <span className="text-muted-foreground text-[10px]">
                          {variable.type}
                        </span>
                      )}
                    </div>
                    {variable.description && (
                      <div className="text-muted-foreground mb-1 text-[10px]">
                        {variable.description}
                      </div>
                    )}
                    {variable.type === "Boolean" ? (
                      <Select
                        value={values[variable.name] ?? variable.defaultValue ?? "false"}
                        onValueChange={(value) =>
                          setValues({ ...values, [variable.name]: value })
                        }
                      >
                        <SelectTrigger size="sm" className="w-full text-xs">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="true">是</SelectItem>
                          <SelectItem value="false">否</SelectItem>
                        </SelectContent>
                      </Select>
                    ) : (
                      <Input
                        className="h-7 text-xs"
                        type={
                          variable.type === "Date"
                            ? "date"
                            : variable.type === "Integer" ||
                              variable.type === "Decimal"
                            ? "number"
                            : "text"
                        }
                        placeholder={variable.example ?? ""}
                        value={
                          values[variable.name] ?? variable.defaultValue ?? ""
                        }
                        onChange={(event) =>
                          setValues({
                            ...values,
                            [variable.name]: event.target.value,
                          })
                        }
                      />
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {result && (
            <div className="rounded-md border border-[var(--color-border-secondary)] bg-sunken p-3 text-[12px]">
              已生成文档 {result.docCode ?? result.documentId ?? "-"}，版本 v
              {result.documentVersion ?? 1}，状态 {result.status ?? "-"}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
          <Button
            disabled={
              render.isPending ||
              !activeVersionId ||
              !selectedVersion?.fileId ||
              versions.length === 0
            }
            onClick={submit}
          >
            {render.isPending ? "渲染中…" : "渲染"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
