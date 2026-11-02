"use client";

import { useState } from "react";
import {
  ArrowRight,
  Ban,
  Check,
  Eye,
  MoreHorizontal,
  Pencil,
  Plus,
  Search,
  Send,
  Trash2,
  X,
} from "lucide-react";

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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
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
import { Textarea } from "@/components/ui/textarea";
import type { PurRequestDTO } from "@/lib/api/contracts/purchase";

import { useRequestAction, useRequestPage } from "../hooks";
import {
  PRIORITIES,
  REQUEST_STATUS,
  REQUEST_TYPES,
  formatDateTime,
  formatMoney,
  requestStatusMeta,
} from "../lib/request-meta";
import { PurchaseRequestConvertDialog } from "./purchase-request-convert-dialog";
import { PurchaseRequestDetailDialog } from "./purchase-request-detail-dialog";
import { PurchaseRequestFormDialog } from "./purchase-request-form-dialog";

const PAGE_SIZE = 20;
const ALL = "__all__";

const ACTION_COPY: Record<
  "submit" | "approve" | "close" | "delete",
  { title: string; description: string; confirmText: string; destructive?: boolean }
> = {
  submit: {
    title: "提交采购申请",
    description: "提交后申请进入待审批状态，审批前不能修改明细。",
    confirmText: "提交审批",
  },
  approve: {
    title: "审批通过",
    description: "通过后即可将申请转为采购单。",
    confirmText: "审批通过",
  },
  close: {
    title: "关闭采购申请",
    description: "关闭后该申请不能再提交或转采购单。",
    confirmText: "关闭申请",
  },
  delete: {
    title: "删除采购申请",
    description: "申请单与全部明细将被删除，且不可恢复。",
    confirmText: "删除",
    destructive: true,
  },
};

export function PurchaseRequestList() {
  const [page, setPage] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState(ALL);
  const [requestType, setRequestType] = useState(ALL);

  const { data, isLoading, isError, refetch } = useRequestPage({
    page,
    size: PAGE_SIZE,
    keyword: keyword || undefined,
    status: status === ALL ? undefined : Number(status),
    requestType: requestType === ALL ? undefined : Number(requestType),
  });

  const action = useRequestAction();

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [detailId, setDetailId] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<PurRequestDTO | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [confirming, setConfirming] = useState<{
    action: "submit" | "approve" | "close" | "delete";
    request: PurRequestDTO;
  } | null>(null);

  const [convertingId, setConvertingId] = useState<string | null>(null);

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openCreate = () => {
    setEditingId(null);
    setFormOpen(true);
  };

  const openEdit = (request: PurRequestDTO) => {
    setEditingId(request.id);
    setFormOpen(true);
  };

  const openConvert = (request: PurRequestDTO) => {
    setConvertingId(request.id);
  };

  const runConfirmedAction = async () => {
    if (!confirming) return;
    try {
      await action.mutateAsync({
        id: confirming.request.id,
        action: confirming.action,
      });
      setConfirming(null);
    } catch {
      // Mutation hook already surfaces the API error.
    }
  };

  const submitReject = async () => {
    if (!rejecting) return;
    if (!rejectReason.trim()) return;
    try {
      await action.mutateAsync({
        id: rejecting.id,
        action: "reject",
        reason: rejectReason.trim(),
      });
      setRejecting(null);
      setRejectReason("");
    } catch {
      // Mutation hook already surfaces the API error.
    }
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="采购协同"
        title="采购申请"
        description="从需求申请、审批到生成采购单的完整闭环"
        actions={
          <Button onClick={openCreate}>
            <Plus />
            新建申请
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setKeyword(keywordDraft.trim());
              setPage(1);
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
              placeholder="搜索申请单号 / 申请人 / 用途"
              className="h-8 w-64 text-xs"
            />
            <Button
              type="submit"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label="搜索"
            >
              <Search />
            </Button>
          </form>

          <TableToolbarEnd>
            <Select
              value={requestType}
              onValueChange={(value) => {
                setRequestType(value);
                setPage(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">类型</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部类型</SelectItem>
                {Object.entries(REQUEST_TYPES).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={status}
              onValueChange={(value) => {
                setStatus(value);
                setPage(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                {Object.entries(REQUEST_STATUS).map(([value, meta]) => (
                  <SelectItem key={value} value={value}>
                    {meta.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 张申请单
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : isError ? (
          <div className="py-16 text-center">
            <p className="text-muted-foreground text-[13px]">
              采购申请加载失败，请检查后端连接后重试
            </p>
            <Button
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={() => refetch()}
            >
              重试
            </Button>
          </div>
        ) : rows.length === 0 ? (
          <TableEmpty>暂无采购申请</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">申请单</TableHead>
                <TableHead>申请人 / 部门</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>优先级</TableHead>
                <TableHead numeric>预算</TableHead>
                <TableHead>期望到货</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((request) => {
                const requestStatus = request.status ?? 0;
                const meta = requestStatusMeta(requestStatus);
                const editable = requestStatus === 0 || requestStatus === 3;
                return (
                  <TableRow key={request.id}>
                    <TableCell>
                      <button
                        type="button"
                        className="text-foreground font-mono text-[12px] font-semibold hover:text-[var(--color-primary-text)] hover:underline"
                        onClick={() => setDetailId(request.id)}
                      >
                        {request.requestNo ?? "-"}
                      </button>
                      <div className="text-muted-foreground mt-0.5 max-w-[260px] truncate text-[10px]">
                        {request.purpose || "未填写用途"}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-[13px]">
                        {request.requesterName || "-"}
                      </div>
                      <div className="text-muted-foreground text-[11px]">
                        {request.deptName || "-"}
                      </div>
                    </TableCell>
                    <TableCell>
                      {REQUEST_TYPES[request.requestType ?? 1]}
                    </TableCell>
                    <TableCell>
                      {PRIORITIES[request.priority ?? 2]}
                    </TableCell>
                    <TableCell numeric>
                      {formatMoney(request.budgetAmount)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDateTime(request.expectedDelivery)}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={meta.tone}>{meta.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDateTime(request.createTime)}
                    </TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="size-8"
                            aria-label="申请操作"
                          >
                            <MoreHorizontal />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => setDetailId(request.id)}>
                            <Eye />
                            查看详情
                          </DropdownMenuItem>
                          {editable && (
                            <DropdownMenuItem onClick={() => openEdit(request)}>
                              <Pencil />
                              编辑
                            </DropdownMenuItem>
                          )}
                          {editable && (
                            <DropdownMenuItem
                              onClick={() =>
                                setConfirming({ action: "submit", request })
                              }
                            >
                              <Send />
                              提交审批
                            </DropdownMenuItem>
                          )}
                          {requestStatus === 1 && (
                            <DropdownMenuItem
                              onClick={() =>
                                setConfirming({ action: "approve", request })
                              }
                            >
                              <Check />
                              审批通过
                            </DropdownMenuItem>
                          )}
                          {requestStatus === 1 && (
                            <DropdownMenuItem
                              onClick={() => {
                                setRejecting(request);
                                setRejectReason("");
                              }}
                            >
                              <X />
                              驳回
                            </DropdownMenuItem>
                          )}
                          {requestStatus === 2 && (
                            <DropdownMenuItem onClick={() => openConvert(request)}>
                              <ArrowRight />
                              转采购单
                            </DropdownMenuItem>
                          )}
                          {[0, 1, 2, 3].includes(requestStatus) && (
                            <DropdownMenuItem
                              onClick={() =>
                                setConfirming({ action: "close", request })
                              }
                            >
                              <Ban />
                              关闭
                            </DropdownMenuItem>
                          )}
                          {editable && (
                            <>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                variant="destructive"
                                onClick={() =>
                                  setConfirming({ action: "delete", request })
                                }
                              >
                                <Trash2 />
                                删除
                              </DropdownMenuItem>
                            </>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
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

      <PurchaseRequestFormDialog
        open={formOpen}
        requestId={editingId}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) setEditingId(null);
        }}
      />

      <PurchaseRequestDetailDialog
        open={!!detailId}
        requestId={detailId}
        onOpenChange={(open) => {
          if (!open) setDetailId(null);
        }}
      />

      <PurchaseRequestConvertDialog
        open={!!convertingId}
        requestId={convertingId}
        onOpenChange={(open) => {
          if (!open) setConvertingId(null);
        }}
      />

      <Dialog
        open={!!rejecting}
        onOpenChange={(open) => {
          if (!open) setRejecting(null);
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>驳回采购申请</DialogTitle>
            <DialogDescription>
              {rejecting?.requestNo} · 驳回原因将展示给申请人。
            </DialogDescription>
          </DialogHeader>
          <Textarea
            rows={3}
            autoFocus
            placeholder="请填写驳回原因"
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejecting(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={!rejectReason.trim() || action.isPending}
              onClick={submitReject}
            >
              {action.isPending ? "提交中..." : "确认驳回"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!confirming}
        title={confirming ? ACTION_COPY[confirming.action].title : ""}
        description={
          confirming ? ACTION_COPY[confirming.action].description : undefined
        }
        confirmText={
          confirming ? ACTION_COPY[confirming.action].confirmText : undefined
        }
        destructive={
          confirming ? ACTION_COPY[confirming.action].destructive : false
        }
        pending={action.isPending}
        onConfirm={runConfirmedAction}
        onOpenChange={(open) => {
          if (!open) setConfirming(null);
        }}
      />
    </div>
  );
}
