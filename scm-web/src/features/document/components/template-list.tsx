"use client";

import { useState } from "react";

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
import {
  TableEmpty,
  TableLoading,
  TablePagination,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import {
  DOC_TEMPLATE_STATUS,
  type DocTemplateDTO,
} from "@/lib/api/contracts/document";

import { useCreateTemplate, useTemplatePage } from "../hooks";
import { TemplateManageDialog } from "./template-manage-dialog";
import { TemplateRenderDialog } from "./template-render-dialog";

const PAGE_SIZE = 20;
const ALL = "__all__";

const TEMPLATE_TYPES = ["CONTRACT", "REPORT", "NOTICE", "OTHER"];
const FILE_FORMATS = ["DOCX", "XLSX", "PDF", "HTML"];

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

export function TemplateList() {
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState({
    keyword: "",
    templateType: ALL,
    status: ALL,
  });
  const [applied, setApplied] = useState(draft);

  const query = useTemplatePage({
    page,
    size: PAGE_SIZE,
    keyword: applied.keyword || undefined,
    templateType: applied.templateType === ALL ? undefined : applied.templateType,
    status: applied.status === ALL ? undefined : applied.status,
  });
  const rows = query.data?.records ?? [];
  const total = query.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const create = useCreateTemplate();

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    templateCode: "",
    templateName: "",
    templateType: "CONTRACT",
    category: "",
    fileFormat: "DOCX",
    description: "",
  });

  const [managing, setManaging] = useState<DocTemplateDTO | null>(null);
  const [rendering, setRendering] = useState<DocTemplateDTO | null>(null);

  const apply = () => {
    setApplied(draft);
    setPage(1);
  };

  const reset = () => {
    const empty = { keyword: "", templateType: ALL, status: ALL };
    setDraft(empty);
    setApplied(empty);
    setPage(1);
  };

  const submitCreate = async () => {
    try {
      await create.mutateAsync({
        templateCode: createForm.templateCode.trim(),
        templateName: createForm.templateName.trim(),
        templateType: createForm.templateType,
        category: createForm.category.trim() || null,
        fileFormat: createForm.fileFormat,
        description: createForm.description.trim() || null,
        status: "DRAFT",
        currentVersion: 0,
      });
      setCreateOpen(false);
      setCreateForm({
        templateCode: "",
        templateName: "",
        templateType: "CONTRACT",
        category: "",
        fileFormat: "DOCX",
        description: "",
      });
    } catch {
      // hook 已提示错误
    }
  };

  return (
    <div className="space-y-4">
      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-52 text-xs"
            placeholder="模板名称 / 编码"
            value={draft.keyword}
            onChange={(event) =>
              setDraft({ ...draft, keyword: event.target.value })
            }
            onKeyDown={(event) => event.key === "Enter" && apply()}
          />
          <Select
            value={draft.templateType}
            onValueChange={(value) =>
              setDraft({ ...draft, templateType: value })
            }
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="模板类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部类型</SelectItem>
              {TEMPLATE_TYPES.map((type) => (
                <SelectItem key={type} value={type}>
                  {type}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={draft.status}
            onValueChange={(value) => setDraft({ ...draft, status: value })}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              {Object.entries(DOC_TEMPLATE_STATUS).map(([value, meta]) => (
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
            <span className="text-muted-foreground text-[11px]">共 {total} 个模板</span>
            <Button size="sm" onClick={() => setCreateOpen(true)}>
              新建模板
            </Button>
          </TableToolbarEnd>
        </TableToolbar>

        {query.isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无文档模板</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">模板</TableHead>
                <TableHead>类型 / 分类</TableHead>
                <TableHead>格式</TableHead>
                <TableHead className="text-center">版本</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => {
                const status =
                  DOC_TEMPLATE_STATUS[row.status ?? ""] ?? DOC_TEMPLATE_STATUS.DRAFT;
                return (
                  <TableRow key={row.id}>
                    <TableCell>
                      <div className="text-[12px] font-medium">
                        {row.templateName ?? "-"}
                      </div>
                      <div className="text-muted-foreground font-mono text-[10px]">
                        {row.templateCode ?? "-"}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div>{row.templateType ?? "-"}</div>
                      <div className="text-muted-foreground text-[10px]">
                        {row.category ?? "未分类"}
                      </div>
                    </TableCell>
                    <TableCell>{row.fileFormat ?? "-"}</TableCell>
                    <TableCell className="text-center tabular-nums">
                      v{row.currentVersion ?? 0}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDateTime(row.createTime)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setRendering(row)}
                      >
                        渲染
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setManaging(row)}
                      >
                        管理
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
            <DialogTitle>新建文档模板</DialogTitle>
            <DialogDescription>
              模板创建后为草稿状态，可在渲染前补充变量 Schema 与版本。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">模板编码</Label>
              <Input
                value={createForm.templateCode}
                placeholder="例如 TPL-CONTRACT-001"
                onChange={(event) =>
                  setCreateForm({ ...createForm, templateCode: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">模板名称</Label>
              <Input
                value={createForm.templateName}
                onChange={(event) =>
                  setCreateForm({ ...createForm, templateName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">模板类型</Label>
              <Select
                value={createForm.templateType}
                onValueChange={(value) =>
                  setCreateForm({ ...createForm, templateType: value })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {TEMPLATE_TYPES.map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">文件格式</Label>
              <Select
                value={createForm.fileFormat}
                onValueChange={(value) =>
                  setCreateForm({ ...createForm, fileFormat: value })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {FILE_FORMATS.map((format) => (
                    <SelectItem key={format} value={format}>
                      {format}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">分类</Label>
              <Input
                value={createForm.category}
                placeholder="例如 采购合同"
                onChange={(event) =>
                  setCreateForm({ ...createForm, category: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">描述</Label>
              <Textarea
                rows={2}
                value={createForm.description}
                onChange={(event) =>
                  setCreateForm({ ...createForm, description: event.target.value })
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
                create.isPending ||
                !createForm.templateCode.trim() ||
                !createForm.templateName.trim()
              }
              onClick={submitCreate}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {managing && (
        <TemplateManageDialog
          template={managing}
          onOpenChange={(open) => !open && setManaging(null)}
        />
      )}

      {rendering && (
        <TemplateRenderDialog
          template={rendering}
          onOpenChange={(open) => !open && setRendering(null)}
        />
      )}
    </div>
  );
}
