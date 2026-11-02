"use client";

import { useState } from "react";
import { message } from "antd";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
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
import {
  TableEmpty,
  TableLoading,
  TablePagination,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import {
  DOC_DOCUMENT_OPERATION,
  DOC_DOCUMENT_STATUS,
  type DocDocumentDTO,
} from "@/lib/api/contracts/document";
import { toApiError } from "@/lib/api/envelope";
import { documentApiService } from "@/lib/api/services/document";

import {
  useChangeDocumentStatus,
  useCreateDocument,
  useDocumentAudits,
  useDocumentPage,
  useDocumentVersions,
} from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

type StatusAction = "SIGNED" | "ARCHIVED";

const STATUS_ACTION_LABEL: Record<StatusAction, string> = {
  SIGNED: "签署",
  ARCHIVED: "归档",
};

export function DocumentList() {
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState({ keyword: "", businessType: "", status: ALL });
  const [applied, setApplied] = useState(draft);
  const [selected, setSelected] = useState<DocDocumentDTO | null>(null);

  const query = useDocumentPage({
    page,
    size: PAGE_SIZE,
    keyword: applied.keyword || undefined,
    businessType: applied.businessType || undefined,
    status: applied.status === ALL ? undefined : applied.status,
  });
  const rows = query.data?.records ?? [];
  const total = query.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const versionsQuery = useDocumentVersions(selected?.id ?? "", Boolean(selected));
  const auditsQuery = useDocumentAudits(selected?.id ?? "", Boolean(selected));
  const changeStatus = useChangeDocumentStatus();
  const [statusAction, setStatusAction] = useState<StatusAction | null>(null);
  const [downloading, setDownloading] = useState<string | null>(null);

  const create = useCreateDocument();
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({
    docCode: "",
    docName: "",
    templateId: "",
    businessType: "",
    businessId: "",
  });

  const apply = () => {
    setApplied(draft);
    setPage(1);
  };

  const reset = () => {
    const empty = { keyword: "", businessType: "", status: ALL };
    setDraft(empty);
    setApplied(empty);
    setPage(1);
  };

  const submitCreate = async () => {
    try {
      await create.mutateAsync({
        docCode: form.docCode.trim(),
        docName: form.docName.trim(),
        templateId: form.templateId.trim() || null,
        businessType: form.businessType.trim() || null,
        businessId: form.businessId.trim() || null,
        status: "GENERATED",
        currentVersion: 1,
      });
      setCreateOpen(false);
      setForm({
        docCode: "",
        docName: "",
        templateId: "",
        businessType: "",
        businessId: "",
      });
    } catch {
      // hook 已提示错误
    }
  };

  const downloadVersion = async (fileId?: string | null, versionId?: string) => {
    if (!fileId) {
      message.warning("该版本尚未关联产物文件");
      return;
    }
    setDownloading(versionId ?? fileId);
    try {
      const result = await documentApiService.getDocumentFileUrl(fileId);
      if (!result?.url) {
        message.warning("未获取到文件访问地址");
        return;
      }
      window.open(result.url, "_blank", "noopener,noreferrer");
    } catch (error) {
      message.error(toApiError(error).message);
    } finally {
      setDownloading(null);
    }
  };

  const submitStatusChange = async () => {
    if (!selected || !statusAction) return;
    try {
      const updated = await changeStatus.mutateAsync({
        documentId: selected.id,
        request: { status: statusAction },
      });
      setSelected(updated ?? selected);
      setStatusAction(null);
    } catch {
      // hook 已提示错误
    }
  };

  const status = selected
    ? DOC_DOCUMENT_STATUS[selected.status ?? ""] ?? DOC_DOCUMENT_STATUS.GENERATED
    : null;
  const versions = versionsQuery.data ?? [];
  const audits = auditsQuery.data ?? [];

  return (
    <div className="space-y-4">
      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-52 text-xs"
            placeholder="文档名称 / 编码"
            value={draft.keyword}
            onChange={(event) => setDraft({ ...draft, keyword: event.target.value })}
            onKeyDown={(event) => event.key === "Enter" && apply()}
          />
          <Input
            className="h-7 w-36 text-xs"
            placeholder="业务类型"
            value={draft.businessType}
            onChange={(event) =>
              setDraft({ ...draft, businessType: event.target.value })
            }
          />
          <Select
            value={draft.status}
            onValueChange={(value) => setDraft({ ...draft, status: value })}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              {Object.entries(DOC_DOCUMENT_STATUS).map(([value, meta]) => (
                <SelectItem key={value} value={value}>
                  {meta.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button size="sm" variant="outline" onClick={apply}>
            查询
          </Button>
          <Button size="sm" variant="ghost" onClick={reset}>
            重置
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">共 {total} 份文档</span>
            <Button size="sm" onClick={() => setCreateOpen(true)}>
              新建文档
            </Button>
          </TableToolbarEnd>
        </TableToolbar>

        {query.isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无文档</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">文档</TableHead>
                <TableHead>业务类型</TableHead>
                <TableHead className="text-center">版本</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => {
                const meta =
                  DOC_DOCUMENT_STATUS[row.status ?? ""] ??
                  DOC_DOCUMENT_STATUS.GENERATED;
                return (
                  <TableRow key={row.id}>
                    <TableCell>
                      <div className="text-[12px] font-medium">{row.docName ?? "-"}</div>
                      <div className="text-muted-foreground font-mono text-[10px]">
                        {row.docCode ?? "-"}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div>{row.businessType ?? "-"}</div>
                      <div className="text-muted-foreground text-[10px]">
                        {row.businessId ?? ""}
                      </div>
                    </TableCell>
                    <TableCell className="text-center tabular-nums">
                      v{row.currentVersion ?? 0}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={meta.variant}>{meta.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDateTime(row.createTime)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setSelected(row)}>
                        详情
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>新建文档</DialogTitle>
            <DialogDescription>
              手工登记文档元数据；由模板渲染生成的文档会自动出现在此处。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">文档编码</Label>
              <Input
                value={form.docCode}
                placeholder="例如 DOC-2026-001"
                onChange={(event) => setForm({ ...form, docCode: event.target.value })}
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">文档名称</Label>
              <Input
                value={form.docName}
                onChange={(event) => setForm({ ...form, docName: event.target.value })}
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">来源模板 ID</Label>
              <Input
                value={form.templateId}
                onChange={(event) =>
                  setForm({ ...form, templateId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">业务类型</Label>
              <Input
                value={form.businessType}
                placeholder="例如 PURCHASE_CONTRACT"
                onChange={(event) =>
                  setForm({ ...form, businessType: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">业务 ID</Label>
              <Input
                value={form.businessId}
                onChange={(event) =>
                  setForm({ ...form, businessId: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                create.isPending || !form.docCode.trim() || !form.docName.trim()
              }
              onClick={submitCreate}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(selected)}
        onOpenChange={(open) => !open && setSelected(null)}
      >
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{selected?.docName ?? "文档详情"}</DialogTitle>
            <DialogDescription>
              {selected?.docCode ?? "-"}
              {status && ` · ${status.label}`}
            </DialogDescription>
          </DialogHeader>

          {selected && (
            <div className="space-y-4">
              <div className="grid gap-2 text-[12px] sm:grid-cols-3">
                <InfoItem label="业务类型" value={selected.businessType} />
                <InfoItem label="业务 ID" value={selected.businessId} />
                <InfoItem label="创建人" value={selected.createdBy} />
                <InfoItem label="来源模板" value={selected.templateId} mono />
                <InfoItem
                  label="模板版本"
                  value={
                    selected.templateVersion != null
                      ? `v${selected.templateVersion}`
                      : null
                  }
                />
                <InfoItem
                  label="当前版本"
                  value={`v${selected.currentVersion ?? 0}`}
                />
                <InfoItem label="创建时间" value={formatDateTime(selected.createTime)} />
              </div>

              <section className="space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-[12px] font-medium">版本历史</h4>
                  <span className="text-muted-foreground text-[11px]">
                    共 {versions.length} 个版本
                  </span>
                </div>
                {versionsQuery.isLoading ? (
                  <div className="text-muted-foreground text-[12px]">版本加载中…</div>
                ) : versions.length === 0 ? (
                  <div className="text-muted-foreground text-[12px]">
                    暂无版本记录（手工登记的文档没有渲染产物）
                  </div>
                ) : (
                  <div className="divide-y rounded-md border border-[var(--color-border-secondary)]">
                    {versions.map((version) => (
                      <div
                        key={version.id}
                        className="flex flex-wrap items-center justify-between gap-2 px-3 py-2 text-[12px]"
                      >
                        <div>
                          <span className="font-medium tabular-nums">
                            v{version.version ?? "-"}
                          </span>
                          <span className="text-muted-foreground ml-2 font-mono text-[10px]">
                            {version.renderParamsChecksum
                              ? `checksum ${version.renderParamsChecksum}`
                              : "-"}
                          </span>
                        </div>
                        <div className="text-muted-foreground text-[11px]">
                          {formatDateTime(version.createTime)}
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={!version.fileId || downloading === version.id}
                          onClick={() =>
                            downloadVersion(version.fileId, version.id)
                          }
                        >
                          {downloading === version.id ? "获取中…" : "下载"}
                        </Button>
                      </div>
                    ))}
                  </div>
                )}
              </section>

              <section className="space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-[12px] font-medium">审计轨迹</h4>
                  <span className="text-muted-foreground text-[11px]">
                    共 {audits.length} 条记录
                  </span>
                </div>
                {auditsQuery.isLoading ? (
                  <div className="text-muted-foreground text-[12px]">轨迹加载中…</div>
                ) : audits.length === 0 ? (
                  <div className="text-muted-foreground text-[12px]">暂无审计记录</div>
                ) : (
                  <ol className="space-y-2">
                    {audits.map((audit) => (
                      <li
                        key={audit.id}
                        className="flex flex-wrap items-center gap-2 rounded-md bg-sunken px-3 py-2 text-[12px]"
                      >
                        <Badge variant="secondary">
                          {DOC_DOCUMENT_OPERATION[audit.operation ?? ""] ??
                            audit.operation ??
                            "-"}
                        </Badge>
                        <span>{audit.operatorName || audit.operatorId || "系统"}</span>
                        <span className="text-muted-foreground text-[11px]">
                          v{audit.beforeVersion ?? 0} → v{audit.afterVersion ?? 0}
                        </span>
                        <span className="text-muted-foreground ml-auto text-[11px]">
                          {formatDateTime(audit.createTime)}
                        </span>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setSelected(null)}>
              关闭
            </Button>
            {selected?.status === "GENERATED" && (
              <Button
                variant="outline"
                onClick={() => setStatusAction("SIGNED")}
              >
                签署
              </Button>
            )}
            {(selected?.status === "GENERATED" ||
              selected?.status === "SIGNED") && (
              <Button onClick={() => setStatusAction("ARCHIVED")}>归档</Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(statusAction)}
        title={statusAction ? `${STATUS_ACTION_LABEL[statusAction]}文档` : "变更文档状态"}
        description={
          statusAction
            ? `确定将「${selected?.docName ?? selected?.id ?? ""}」标记为${
                DOC_DOCUMENT_STATUS[statusAction]?.label ?? statusAction
              }吗？`
            : undefined
        }
        confirmText={statusAction ? STATUS_ACTION_LABEL[statusAction] : "确认"}
        pending={changeStatus.isPending}
        onOpenChange={(open) => !open && setStatusAction(null)}
        onConfirm={submitStatusChange}
      />
    </div>
  );
}

function InfoItem({
  label,
  value,
  mono = false,
}: {
  label: string;
  value?: string | null;
  mono?: boolean;
}) {
  return (
    <div>
      <div className="text-muted-foreground text-[10px] uppercase tracking-wide">
        {label}
      </div>
      <div className={mono ? "font-mono text-[11px] break-all" : "break-all"}>
        {value || "-"}
      </div>
    </div>
  );
}
