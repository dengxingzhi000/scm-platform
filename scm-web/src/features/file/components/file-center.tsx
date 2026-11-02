"use client";

import { useRef, useState } from "react";
import { message } from "antd";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { PageHeader } from "@/components/ui/page-header";
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
import type {
  ContractInfo,
  FileMetadataDTO,
  InvoiceInfo,
} from "@/lib/api/contracts/file";
import { toApiError } from "@/lib/api/envelope";
import { fileApiService } from "@/lib/api/services/file";
import { cn } from "@/lib/utils";

import {
  useDeleteFile,
  useDeleteFiles,
  useFilePage,
  useFileStats,
  useOcr,
  useUpdateFile,
  useUploadFile,
  type OcrMode,
} from "../hooks";
import { FilePreviewDialog } from "./file-preview-dialog";

const PAGE_SIZE = 20;

const OCR_MODES: Record<OcrMode, string> = {
  recognize: "通用文字",
  invoice: "发票信息",
  contract: "合同信息",
};

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

const formatBytes = (value?: number | null) => {
  if (!value || value <= 0) return "-";
  const units = ["B", "KB", "MB", "GB"];
  let size = value;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit += 1;
  }
  return `${size.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
};

function OcrResultPanel({
  mode,
  text,
  invoice,
  contract,
  pending,
}: {
  mode: OcrMode;
  text: string | null;
  invoice: InvoiceInfo | null;
  contract: ContractInfo | null;
  pending: boolean;
}) {
  if (pending) {
    return (
      <TablePanel>
        <TableLoading />
      </TablePanel>
    );
  }
  if (mode === "recognize") {
    return (
      <div className="rounded-lg border border-[var(--color-border-secondary)] bg-card p-4">
        <div className="text-muted-foreground mb-2 text-[10px] uppercase tracking-wide">
          识别文本
        </div>
        <pre className="bg-sunken max-h-[420px] overflow-auto rounded-md p-3 text-[12px] whitespace-pre-wrap break-all">
          {text ?? "暂无识别结果"}
        </pre>
      </div>
    );
  }

  const data = (mode === "invoice" ? invoice : contract) as Record<
    string,
    unknown
  > | null;
  const entries = data
    ? Object.entries(data).filter(
        ([key, value]) =>
          key !== "rawText" && value !== null && value !== undefined && value !== "",
      )
    : [];
  const rawText = data?.rawText ? String(data.rawText) : "";

  return (
    <div className="rounded-lg border border-[var(--color-border-secondary)] bg-card p-4">
      <div className="text-muted-foreground mb-2 text-[10px] uppercase tracking-wide">
        {mode === "invoice" ? "发票信息" : "合同信息"}
      </div>
      {entries.length === 0 ? (
        <div className="text-muted-foreground text-[12px]">
          未提取到结构化字段
        </div>
      ) : (
        <div className="grid gap-2 text-[12px] sm:grid-cols-2">
          {entries.map(([key, value]) => (
            <div key={key}>
              <div className="text-muted-foreground text-[10px] uppercase tracking-wide">
                {key}
              </div>
              <div>{String(value)}</div>
            </div>
          ))}
        </div>
      )}
      {rawText && (
        <details className="mt-3">
          <summary className="text-muted-foreground cursor-pointer text-[11px]">
            原始识别文本
          </summary>
          <pre className="bg-sunken mt-1 max-h-64 overflow-auto rounded-md p-2 text-[11px] whitespace-pre-wrap break-all">
            {rawText}
          </pre>
        </details>
      )}
    </div>
  );
}

export function FileCenter() {
  const [tab, setTab] = useState<"files" | "ocr">("files");
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState({ keyword: "", bizType: "", bizId: "" });
  const [applied, setApplied] = useState(draft);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const query = useFilePage({
    page,
    size: PAGE_SIZE,
    keyword: applied.keyword || undefined,
    bizType: applied.bizType || undefined,
    bizId: applied.bizId || undefined,
  });
  const stats = useFileStats();
  const statsData = stats.data;
  const rows = query.data?.records ?? [];
  const total = query.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const pageIds = rows.map((row) => row.id);
  const selectedOnPage = pageIds.filter((id) => selectedIds.includes(id)).length;
  const allSelected = pageIds.length > 0 && selectedOnPage === pageIds.length;
  const someSelected = selectedOnPage > 0 && !allSelected;

  const statItems = [
    {
      label: "文件总数",
      value: statsData ? `${statsData.totalFiles}` : "-",
      hint: statsData
        ? `图片 ${statsData.imageFiles} · 文档 ${statsData.documentFiles} · 其他 ${statsData.otherFiles}`
        : "统计加载中",
    },
    {
      label: "存储总量",
      value: statsData ? formatBytes(statsData.totalSize) : "-",
      hint: "当前租户占用空间",
    },
    {
      label: "最近上传",
      value: formatDateTime(statsData?.lastUploadTime),
      hint: statsData?.lastUploadTime ? "按上传时间倒序" : "暂无上传记录",
    },
  ];

  const upload = useUploadFile();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadBizType, setUploadBizType] = useState("");
  const [uploadBizId, setUploadBizId] = useState("");
  const uploadInputRef = useRef<HTMLInputElement>(null);

  const updateFile = useUpdateFile();
  const [editingFile, setEditingFile] = useState<FileMetadataDTO | null>(null);
  const [editDraft, setEditDraft] = useState({
    originalName: "",
    bizType: "",
    bizId: "",
  });

  const remove = useDeleteFile();
  const [deletingFile, setDeletingFile] = useState<FileMetadataDTO | null>(null);
  const removeBatch = useDeleteFiles();
  const [batchOpen, setBatchOpen] = useState(false);

  const [previewFile, setPreviewFile] = useState<FileMetadataDTO | null>(null);

  const [urlDialog, setUrlDialog] = useState<{
    name: string;
    url: string;
  } | null>(null);

  const ocr = useOcr();
  const [ocrMode, setOcrMode] = useState<OcrMode>("recognize");
  const [ocrFile, setOcrFile] = useState<File | null>(null);
  const ocrInputRef = useRef<HTMLInputElement>(null);
  const [ocrText, setOcrText] = useState<string | null>(null);
  const [ocrInvoice, setOcrInvoice] = useState<InvoiceInfo | null>(null);
  const [ocrContract, setOcrContract] = useState<ContractInfo | null>(null);

  const apply = () => {
    setApplied(draft);
    setSelectedIds([]);
    setPage(1);
  };

  const reset = () => {
    const empty = { keyword: "", bizType: "", bizId: "" };
    setDraft(empty);
    setApplied(empty);
    setSelectedIds([]);
    setPage(1);
  };

  const changePage = (next: number) => {
    setSelectedIds([]);
    setPage(next);
  };

  const toggleRow = (id: string) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  const toggleAll = () => {
    setSelectedIds((prev) => {
      if (allSelected) {
        return prev.filter((id) => !pageIds.includes(id));
      }
      return [...new Set([...prev, ...pageIds])];
    });
  };

  const openEdit = (row: FileMetadataDTO) => {
    setEditingFile(row);
    setEditDraft({
      originalName: row.originalName ?? "",
      bizType: row.bizType ?? "",
      bizId: row.bizId ?? "",
    });
  };

  const submitEdit = async () => {
    if (!editingFile) return;
    try {
      await updateFile.mutateAsync({
        id: editingFile.id,
        payload: {
          originalName: editDraft.originalName.trim(),
          bizType: editDraft.bizType.trim(),
          bizId: editDraft.bizId.trim(),
        },
      });
      setEditingFile(null);
    } catch {
      // useUpdateFile already surfaces the API error message.
    }
  };

  const confirmDelete = async () => {
    if (!deletingFile) return;
    try {
      await remove.mutateAsync(deletingFile.id);
      setSelectedIds((prev) => prev.filter((id) => id !== deletingFile.id));
      setDeletingFile(null);
    } catch {
      // useDeleteFile already surfaces the API error message.
    }
  };

  const confirmBatchDelete = async () => {
    if (selectedIds.length === 0) return;
    try {
      await removeBatch.mutateAsync(selectedIds);
      setSelectedIds([]);
      setBatchOpen(false);
    } catch {
      // useDeleteFiles already surfaces the API error message.
    }
  };

  const submitUpload = async () => {
    if (!uploadFile) return;
    await upload.mutateAsync({
      file: uploadFile,
      bizType: uploadBizType.trim() || undefined,
      bizId: uploadBizId.trim() || undefined,
    });
    setUploadOpen(false);
    setUploadFile(null);
    setUploadBizType("");
    setUploadBizId("");
  };

  const download = async (row: FileMetadataDTO) => {
    try {
      const blob = await fileApiService.download(row.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = row.originalName ?? row.id;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      message.error(toApiError(error).message);
    }
  };

  const openUrl = async (row: FileMetadataDTO) => {
    try {
      const result = await fileApiService.getUrl(row.id);
      setUrlDialog({ name: row.originalName ?? row.id, url: result?.url ?? "" });
    } catch {
      setUrlDialog({ name: row.originalName ?? row.id, url: "" });
    }
  };

  const submitOcr = async () => {
    if (!ocrFile) return;
    setOcrText(null);
    setOcrInvoice(null);
    setOcrContract(null);
    const result = await ocr.mutateAsync({ file: ocrFile, mode: ocrMode });
    if (result.mode === "invoice") {
      setOcrInvoice(result.invoice ?? null);
    } else if (result.mode === "contract") {
      setOcrContract(result.contract ?? null);
    } else {
      setOcrText(result.text ?? null);
    }
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="文件中心"
        title="文件与 OCR"
        description="查看文件用量、检索与预览文件，或对图片执行文字、发票与合同识别"
        actions={<Button onClick={() => setUploadOpen(true)}>上传文件</Button>}
      />

      <div className="grid gap-3 sm:grid-cols-3">
        {statItems.map((item) => (
          <div
            key={item.label}
            className="rounded-lg border border-[var(--color-border-secondary)] bg-card px-4 py-3"
          >
            <div className="text-muted-foreground text-[10px] uppercase tracking-wide">
              {item.label}
            </div>
            <div className="mt-1 text-[18px] font-semibold tabular-nums">
              {item.value}
            </div>
            <div className="text-muted-foreground mt-0.5 text-[11px]">
              {item.hint}
            </div>
          </div>
        ))}
      </div>

      <div className="flex items-center gap-1">
        {[
          { key: "files" as const, label: "文件列表" },
          { key: "ocr" as const, label: "OCR 识别" },
        ].map((item) => (
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

      {tab === "files" ? (
        <TablePanel>
          <TableToolbar>
            <Input
              className="h-7 w-44 text-xs"
              placeholder="文件名"
              value={draft.keyword}
              onChange={(event) =>
                setDraft({ ...draft, keyword: event.target.value })
              }
              onKeyDown={(event) => event.key === "Enter" && apply()}
            />
            <Input
              className="h-7 w-32 text-xs"
              placeholder="业务类型"
              value={draft.bizType}
              onChange={(event) =>
                setDraft({ ...draft, bizType: event.target.value })
              }
              onKeyDown={(event) => event.key === "Enter" && apply()}
            />
            <Input
              className="h-7 w-36 text-xs"
              placeholder="业务 ID"
              value={draft.bizId}
              onChange={(event) =>
                setDraft({ ...draft, bizId: event.target.value })
              }
              onKeyDown={(event) => event.key === "Enter" && apply()}
            />
            <Button size="sm" variant="outline" onClick={apply}>
              查询
            </Button>
            <Button size="sm" variant="ghost" onClick={reset}>
              重置
            </Button>
            {selectedIds.length > 0 && (
              <>
                <span className="text-muted-foreground text-[11px]">
                  已选 {selectedIds.length} 项
                </span>
                <Button
                  size="sm"
                  variant="ghost"
                  className="text-[var(--color-error-text)]"
                  onClick={() => setBatchOpen(true)}
                >
                  批量删除
                </Button>
              </>
            )}
            <TableToolbarEnd>
              <span className="text-muted-foreground text-[11px]">
                共 {total} 个文件
              </span>
            </TableToolbarEnd>
          </TableToolbar>

          {query.isLoading ? (
            <TableLoading />
          ) : rows.length === 0 ? (
            <TableEmpty>暂无文件</TableEmpty>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-8">
                    <Checkbox
                      checked={
                        allSelected ? true : someSelected ? "indeterminate" : false
                      }
                      onCheckedChange={toggleAll}
                      aria-label="选择本页全部文件"
                    />
                  </TableHead>
                  <TableHead className="min-w-[220px]">文件</TableHead>
                  <TableHead>类型</TableHead>
                  <TableHead numeric>大小</TableHead>
                  <TableHead>业务</TableHead>
                  <TableHead>存储引擎</TableHead>
                  <TableHead>上传时间</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>
                      <Checkbox
                        checked={selectedIds.includes(row.id)}
                        onCheckedChange={() => toggleRow(row.id)}
                        aria-label={`选择 ${row.originalName ?? row.id}`}
                      />
                    </TableCell>
                    <TableCell>
                      <div className="text-[12px] font-medium">
                        {row.originalName ?? "-"}
                      </div>
                      <div className="text-muted-foreground font-mono text-[10px]">
                        {row.id}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{row.contentType ?? "-"}</Badge>
                    </TableCell>
                    <TableCell numeric>{formatBytes(row.fileSize)}</TableCell>
                    <TableCell>
                      <div>{row.bizType ?? "-"}</div>
                      <div className="text-muted-foreground font-mono text-[10px]">
                        {row.bizId ?? ""}
                      </div>
                    </TableCell>
                    <TableCell>{row.storageEngine ?? "-"}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDateTime(row.createTime)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setPreviewFile(row)}
                      >
                        预览
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => download(row)}
                      >
                        下载
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => openUrl(row)}>
                        地址
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => openEdit(row)}>
                        编辑
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-[var(--color-error-text)]"
                        onClick={() => setDeletingFile(row)}
                      >
                        删除
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          <TablePagination
            page={page}
            pageSize={PAGE_SIZE}
            total={total}
            pageCount={pageCount}
            onPageChange={changePage}
          />
        </TablePanel>
      ) : (
        <div className="space-y-4">
          <div className="rounded-lg border border-[var(--color-border-secondary)] bg-card p-4">
            <div className="flex flex-wrap items-center gap-2">
              <Select
                value={ocrMode}
                onValueChange={(value) => setOcrMode(value as OcrMode)}
              >
                <SelectTrigger size="sm" className="w-32 text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(OCR_MODES).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <input
                ref={ocrInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) =>
                  setOcrFile(event.target.files?.[0] ?? null)
                }
              />
              <Button
                variant="outline"
                size="sm"
                onClick={() => ocrInputRef.current?.click()}
              >
                选择图片
              </Button>
              <span className="text-muted-foreground text-[11px]">
                {ocrFile?.name ?? "未选择图片"}
              </span>
              <Button
                size="sm"
                disabled={!ocrFile || ocr.isPending}
                onClick={submitOcr}
              >
                开始识别
              </Button>
            </div>
            <p className="text-muted-foreground mt-2 text-[11px]">
              识别依赖服务端 Tesseract 语言包，发票/合同提取为启发式规则，结果仅供参考。
            </p>
          </div>

          <OcrResultPanel
            mode={ocrMode}
            text={ocrText}
            invoice={ocrInvoice}
            contract={ocrContract}
            pending={ocr.isPending}
          />
        </div>
      )}

      <Dialog open={uploadOpen} onOpenChange={setUploadOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>上传文件</DialogTitle>
            <DialogDescription>
              文件存入 scm-file，可关联业务类型与业务 ID 便于检索。
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <input
              ref={uploadInputRef}
              type="file"
              className="hidden"
              onChange={(event) =>
                setUploadFile(event.target.files?.[0] ?? null)
              }
            />
            <Button
              variant="outline"
              className="w-full"
              onClick={() => uploadInputRef.current?.click()}
            >
              {uploadFile ? uploadFile.name : "选择文件"}
            </Button>
            <div>
              <Label className="mb-1.5 text-[12px]">业务类型</Label>
              <Input
                value={uploadBizType}
                placeholder="例如 CONTRACT / INVOICE"
                onChange={(event) => setUploadBizType(event.target.value)}
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">业务 ID</Label>
              <Input
                value={uploadBizId}
                onChange={(event) => setUploadBizId(event.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setUploadOpen(false)}>
              取消
            </Button>
            <Button
              disabled={upload.isPending || !uploadFile}
              onClick={submitUpload}
            >
              上传
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(editingFile)}
        onOpenChange={(open) => !open && setEditingFile(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>编辑文件信息</DialogTitle>
            <DialogDescription>
              重命名文件，或调整它关联的业务类型与业务 ID。
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <Label className="mb-1.5 text-[12px]">文件名</Label>
              <Input
                value={editDraft.originalName}
                onChange={(event) =>
                  setEditDraft({ ...editDraft, originalName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">业务类型</Label>
              <Input
                value={editDraft.bizType}
                placeholder="留空表示解除关联"
                onChange={(event) =>
                  setEditDraft({ ...editDraft, bizType: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">业务 ID</Label>
              <Input
                value={editDraft.bizId}
                placeholder="留空表示解除关联"
                onChange={(event) =>
                  setEditDraft({ ...editDraft, bizId: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditingFile(null)}>
              取消
            </Button>
            <Button
              disabled={updateFile.isPending || !editDraft.originalName.trim()}
              onClick={submitEdit}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <FilePreviewDialog
        file={previewFile}
        onOpenChange={(open) => !open && setPreviewFile(null)}
      />

      <Dialog
        open={Boolean(urlDialog)}
        onOpenChange={(open) => !open && setUrlDialog(null)}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>文件访问地址</DialogTitle>
            <DialogDescription>{urlDialog?.name}</DialogDescription>
          </DialogHeader>
          {urlDialog?.url ? (
            <a
              href={urlDialog.url}
              target="_blank"
              rel="noreferrer"
              className="break-all text-[12px] text-[var(--color-primary-text)] underline"
            >
              {urlDialog.url}
            </a>
          ) : (
            <div className="text-muted-foreground text-[12px]">
              未获取到预签名地址。
            </div>
          )}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(deletingFile)}
        title="删除文件"
        description={
          deletingFile
            ? `确定删除「${deletingFile.originalName ?? deletingFile.id}」吗？文件内容与元数据都会被移除。`
            : undefined
        }
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onOpenChange={(open) => !open && setDeletingFile(null)}
        onConfirm={confirmDelete}
      />

      <ConfirmDialog
        open={batchOpen}
        title="批量删除文件"
        description={`确定删除选中的 ${selectedIds.length} 个文件吗？文件内容与元数据都会被移除。`}
        confirmText="删除"
        destructive
        pending={removeBatch.isPending}
        onOpenChange={setBatchOpen}
        onConfirm={confirmBatchDelete}
      />
    </div>
  );
}
