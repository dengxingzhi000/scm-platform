"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Eye } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { PageHeader } from "@/components/ui/page-header";
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
  TablePanel,
  TablePagination,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import {
  useAuditSupplier,
  useExpiringCertificates,
  useSupplierList,
  useSupplierStats,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import {
  CERTIFICATE_TYPES,
  SUPPLIER_TYPES,
  labelOf,
} from "../lib/constants";
import { expiryDaysClass } from "../lib/expiry";
import { formatDate } from "../lib/mappers";
import type { Supplier, SupplierCertificate } from "../types";

/** 队列与到期预警共用前端的每页条数 —— 后端没有单独的页大小约定。 */
const PAGE_SIZE = 20;

/** 到期预警的天数窗口。 */
const EXPIRY_WINDOWS = [30, 60, 90] as const;

/** 准入状态：1-待审核，3-已驳回。 */
type QueueMode = 1 | 3;

/** 缺失值的统一占位 —— 与其他分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

/** 列表查询失败时的占位：原因 + 重试。 */
function PanelError({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="flex flex-col items-center gap-3 py-12 text-center">
      <p className="text-destructive text-sm">{message}</p>
      <Button variant="outline" size="sm" onClick={onRetry}>
        重试
      </Button>
    </div>
  );
}

export default function SupplierAuditWorkbench() {
  const router = useRouter();

  const [mode, setMode] = useState<QueueMode>(1);
  const [queuePage, setQueuePage] = useState(1);
  const [queueError, setQueueError] = useState<string | null>(null);

  const [days, setDays] = useState<number>(EXPIRY_WINDOWS[0]);
  const [expiryPage, setExpiryPage] = useState(1);

  const [rejectTarget, setRejectTarget] = useState<Supplier | null>(null);
  const [rejectRemark, setRejectRemark] = useState("");
  const [rejectError, setRejectError] = useState<string | null>(null);

  const queue = useSupplierList({
    auditStatus: mode,
    current: queuePage,
    pageSize: PAGE_SIZE,
  });
  const { data: stats } = useSupplierStats();
  const expiry = useExpiringCertificates(days, expiryPage);
  const audit = useAuditSupplier();

  const suppliers = queue.data?.records ?? [];
  const queueTotal = queue.data?.total ?? 0;
  const queuePageCount = Math.max(1, Math.ceil(queueTotal / PAGE_SIZE));

  const certificates = expiry.data?.records ?? [];
  const expiryTotal = expiry.data?.total ?? 0;
  const expiryPageSize = expiry.data?.pageSize || PAGE_SIZE;
  const expiryPageCount = Math.max(
    1,
    Math.ceil(expiryTotal / expiryPageSize),
  );

  const pendingCount = stats?.byAuditStatus?.[1];
  const rejectedCount = stats?.byAuditStatus?.[3];

  const changeMode = (next: QueueMode) => {
    setMode(next);
    setQueuePage(1);
    setQueueError(null);
  };

  const changeExpiryDays = (next: number) => {
    setDays(next);
    setExpiryPage(1);
  };

  const approve = async (supplier: Supplier) => {
    setQueueError(null);
    try {
      await audit.mutateAsync({
        id: supplier.id,
        action: "APPROVE",
        remark: undefined,
      });
    } catch (error) {
      setQueueError(errorMessage(error));
    }
  };

  const openReject = (supplier: Supplier) => {
    setRejectTarget(supplier);
    setRejectRemark("");
    setRejectError(null);
  };

  const closeReject = () => {
    setRejectTarget(null);
    setRejectRemark("");
    setRejectError(null);
  };

  const submitReject = async () => {
    if (!rejectTarget || rejectRemark.trim().length === 0) return;
    setRejectError(null);
    try {
      await audit.mutateAsync({
        id: rejectTarget.id,
        action: "REJECT",
        remark: rejectRemark.trim(),
      });
      closeReject();
    } catch (error) {
      setRejectError(errorMessage(error));
    }
  };

  const canSubmitReject =
    rejectRemark.trim().length > 0 && !audit.isPending;

  /** 「通过」在税号缺失时置灰，并用 tooltip 说明原因。 */
  const renderApprove = (supplier: Supplier) => {
    const missingTaxNo = !supplier.taxNo?.trim();
    const button = (
      <Button
        size="sm"
        className="bg-[var(--color-success)] text-white hover:bg-[var(--color-success-hover)]"
        disabled={missingTaxNo}
        onClick={() => void approve(supplier)}
      >
        通过
      </Button>
    );
    if (!missingTaxNo) return button;
    return (
      <Tooltip>
        <TooltipTrigger asChild>
          <span className="inline-flex">{button}</span>
        </TooltipTrigger>
        <TooltipContent>税号未补全，无法提交审核</TooltipContent>
      </Tooltip>
    );
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="供应商管理"
        title="准入审核工作台"
        description="供应商准入审核队列与资质证照到期预警"
        actions={
          <div
            role="group"
            aria-label="准入队列"
            className="flex items-center gap-0.5 rounded-lg border border-[var(--color-border-secondary)] p-0.5"
          >
            <Button
              size="sm"
              variant={mode === 1 ? "secondary" : "ghost"}
              className="h-7 px-2 text-[11px]"
              onClick={() => changeMode(1)}
            >
              待审核
              {pendingCount !== undefined ? ` ${pendingCount}` : ""}
            </Button>
            <Button
              size="sm"
              variant={mode === 3 ? "secondary" : "ghost"}
              className="h-7 px-2 text-[11px]"
              onClick={() => changeMode(3)}
            >
              已驳回
              {rejectedCount !== undefined ? ` ${rejectedCount}` : ""}
            </Button>
          </div>
        }
      />

      <TablePanel>
        <TableToolbar>
          <div>
            <div className="text-[13px] font-semibold">
              {mode === 1 ? "待审核供应商" : "已驳回供应商"}
            </div>
            <div className="text-muted-foreground text-[11px]">
              {mode === 1
                ? "税号补全后才能通过准入审核，驳回必须填写审核意见。"
                : "审核接口只接受待审核状态，已驳回记录仅供查看。"}
            </div>
          </div>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {queueTotal} 家供应商
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {queueError && (
          <div className={cn(ERROR_STRIP, "mx-3 mt-3")}>{queueError}</div>
        )}

        {queue.isLoading ? (
          <TableLoading />
        ) : queue.isError ? (
          <PanelError
            message={errorMessage(queue.error)}
            onRetry={() => void queue.refetch()}
          />
        ) : suppliers.length === 0 ? (
          <TableEmpty>
            {mode === 1 ? "暂无待审核供应商。" : "暂无已驳回供应商。"}
          </TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>供应商</TableHead>
                <TableHead>供应商类型</TableHead>
                <TableHead>信用等级</TableHead>
                <TableHead>税号</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {suppliers.map((supplier) => (
                <TableRow key={supplier.id}>
                  <TableCell>
                    <div className="max-w-[260px]">
                      <div className="truncate text-[12px] font-semibold">
                        {supplier.name}
                      </div>
                      <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                        {supplier.code}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    {labelOf(SUPPLIER_TYPES, supplier.supplierType)}
                  </TableCell>
                  <TableCell>{supplier.creditRating ?? MISSING}</TableCell>
                  <TableCell className="font-mono">
                    {supplier.taxNo?.trim() ? supplier.taxNo : MISSING}
                  </TableCell>
                  <TableCell className="font-mono">
                    {supplier.createdAt ? supplier.createdAt : MISSING}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-1">
                      {mode === 1 ? (
                        <>
                          {renderApprove(supplier)}
                          <Button
                            variant="outline"
                            size="sm"
                            className="border-destructive/40 text-[var(--color-error-text)] hover:bg-destructive/5"
                            onClick={() => openReject(supplier)}
                          >
                            驳回
                          </Button>
                        </>
                      ) : (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() =>
                            router.push(`/supplier/${supplier.id}`)
                          }
                        >
                          <Eye />
                          查看供应商
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={queuePage}
          pageSize={PAGE_SIZE}
          total={queueTotal}
          pageCount={queuePageCount}
          onPageChange={setQueuePage}
        />
      </TablePanel>

      <TablePanel>
        <TableToolbar>
          <div>
            <div className="text-[13px] font-semibold">
              资质证照到期预警 · 未来 {days} 天内到期
            </div>
            <div className="text-muted-foreground text-[11px]">
              只列出尚未过期的证照，剩余天数越近越醒目。
            </div>
          </div>
          <TableToolbarEnd>
            <div
              role="group"
              aria-label="预警天数"
              className="flex items-center gap-0.5 rounded-lg border border-[var(--color-border-secondary)] p-0.5"
            >
              {EXPIRY_WINDOWS.map((window) => (
                <Button
                  key={window}
                  size="sm"
                  variant={window === days ? "secondary" : "ghost"}
                  className="h-7 px-2 text-[11px] tabular-nums"
                  aria-pressed={window === days}
                  onClick={() => changeExpiryDays(window)}
                >
                  {window} 天
                </Button>
              ))}
            </div>
          </TableToolbarEnd>
        </TableToolbar>

        {expiry.isLoading ? (
          <TableLoading />
        ) : expiry.isError ? (
          <PanelError
            message={errorMessage(expiry.error)}
            onRetry={() => void expiry.refetch()}
          />
        ) : certificates.length === 0 ? (
          <TableEmpty>未来 {days} 天内没有到期证照。</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>供应商</TableHead>
                <TableHead>证照名称</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>到期日</TableHead>
                <TableHead numeric>剩余天数</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {certificates.map((certificate: SupplierCertificate) => {
                const daysLeft = certificate.daysToExpiry;
                return (
                  <TableRow key={certificate.id}>
                    <TableCell>{certificate.supplierName ?? MISSING}</TableCell>
                    <TableCell>
                      {certificate.certificateName ?? MISSING}
                    </TableCell>
                    <TableCell>
                      {labelOf(
                        CERTIFICATE_TYPES,
                        certificate.certificateType,
                      )}
                    </TableCell>
                    <TableCell className="font-mono">
                      {formatDate(certificate.validTo)}
                    </TableCell>
                    <TableCell
                      numeric
                      className={cn(
                        "font-mono tabular-nums",
                        expiryDaysClass(daysLeft),
                      )}
                    >
                      {daysLeft === null ? MISSING : daysLeft}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center justify-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={!certificate.supplierId}
                          onClick={() =>
                            router.push(`/supplier/${certificate.supplierId}`)
                          }
                        >
                          <Eye />
                          查看供应商
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={expiryPage}
          pageSize={expiryPageSize}
          total={expiryTotal}
          pageCount={expiryPageCount}
          onPageChange={setExpiryPage}
        />
      </TablePanel>

      <Dialog
        open={rejectTarget !== null}
        onOpenChange={(open) => {
          if (!open) closeReject();
        }}
      >
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>驳回供应商准入</DialogTitle>
            <DialogDescription>
              驳回「{rejectTarget?.name ?? MISSING}」的准入申请，审核意见必填。
            </DialogDescription>
          </DialogHeader>

          <div>
            <div className="mb-1.5 flex items-center gap-1">
              <label
                className="text-[12px] font-medium"
                htmlFor="audit-reject-remark"
              >
                审核意见
              </label>
              <span className="text-destructive text-[12px]">*</span>
            </div>
            <Textarea
              id="audit-reject-remark"
              rows={3}
              value={rejectRemark}
              onChange={(event) => setRejectRemark(event.target.value)}
              placeholder="说明驳回原因，供应商会看到这条意见"
            />
          </div>

          {rejectError && <div className={ERROR_STRIP}>{rejectError}</div>}

          <DialogFooter>
            <Button variant="outline" onClick={closeReject}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={!canSubmitReject}
              onClick={() => void submitReject()}
            >
              {audit.isPending ? "提交中…" : "确认驳回"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
