"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
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
import type { DocumentFileDTO } from "@/lib/api/contracts/document";
import { documentApiService } from "@/lib/api/services/document";

import { useDeleteDocumentFile, useDocumentFilePage } from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const SOURCE_TYPES: Record<string, string> = {
  TEMPLATE_SOURCE: "模板源文件",
  RENDERED_OUTPUT: "渲染产物",
};

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

export function DocumentFileList() {
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState({ keyword: "", sourceType: ALL });
  const [applied, setApplied] = useState(draft);

  const query = useDocumentFilePage({
    page,
    size: PAGE_SIZE,
    keyword: applied.keyword || undefined,
    sourceType: applied.sourceType === ALL ? undefined : applied.sourceType,
  });
  const rows = query.data?.records ?? [];
  const total = query.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const remove = useDeleteDocumentFile();
  const [deleting, setDeleting] = useState<DocumentFileDTO | null>(null);
  const [urlDialog, setUrlDialog] = useState<{
    name: string;
    url: string;
  } | null>(null);

  const apply = () => {
    setApplied(draft);
    setPage(1);
  };

  const reset = () => {
    const empty = { keyword: "", sourceType: ALL };
    setDraft(empty);
    setApplied(empty);
    setPage(1);
  };

  const openUrl = async (row: DocumentFileDTO) => {
    try {
      const result = await documentApiService.getDocumentFileUrl(row.id);
      setUrlDialog({
        name: row.originalName ?? row.id,
        url: result?.url ?? "",
      });
    } catch (error) {
      setUrlDialog({ name: row.originalName ?? row.id, url: "" });
    }
  };

  return (
    <div className="space-y-4">
      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-52 text-xs"
            placeholder="文件名称"
            value={draft.keyword}
            onChange={(event) => setDraft({ ...draft, keyword: event.target.value })}
            onKeyDown={(event) => event.key === "Enter" && apply()}
          />
          <Select
            value={draft.sourceType}
            onValueChange={(value) => setDraft({ ...draft, sourceType: value })}
          >
            <SelectTrigger size="sm" className="w-36 text-xs">
              <SelectValue placeholder="来源" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部来源</SelectItem>
              {Object.entries(SOURCE_TYPES).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
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
            <span className="text-muted-foreground text-[11px]">共 {total} 个文件</span>
          </TableToolbarEnd>
        </TableToolbar>

        {query.isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无文档文件</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">文件名</TableHead>
                <TableHead>来源</TableHead>
                <TableHead>关联业务</TableHead>
                <TableHead>文件引用</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <div className="text-[12px] font-medium">
                      {row.originalName ?? "-"}
                    </div>
                    <div className="text-muted-foreground font-mono text-[10px]">
                      {row.id}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary">
                      {SOURCE_TYPES[row.sourceType ?? ""] ?? row.sourceType ?? "-"}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-[10px]">
                    {row.refId ?? "-"}
                  </TableCell>
                  <TableCell className="font-mono text-[10px]">
                    {row.fileRef ?? "-"}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {formatDateTime(row.createTime)}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openUrl(row)}
                    >
                      访问地址
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-[var(--color-error-text)]"
                      onClick={() => setDeleting(row)}
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
          onPageChange={setPage}
        />
      </TablePanel>

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
              未获取到访问地址，可能文件尚未同步到 scm-file。
            </div>
          )}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(deleting)}
        title="删除文档文件"
        description={
          deleting ? `确定删除「${deleting.originalName ?? deleting.id}」吗？` : undefined
        }
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onOpenChange={(open) => !open && setDeleting(null)}
        onConfirm={async () => {
          if (!deleting) return;
          await remove.mutateAsync(deleting.id);
          setDeleting(null);
        }}
      />
    </div>
  );
}
